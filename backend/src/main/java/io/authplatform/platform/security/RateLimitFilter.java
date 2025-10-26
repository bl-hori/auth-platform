package io.authplatform.platform.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.authplatform.platform.config.RateLimitProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Servlet filter for API rate limiting using Bucket4j.
 *
 * <p>This filter implements the Token Bucket algorithm to limit the rate of requests
 * per API key. Each API key gets its own bucket with configurable capacity and refill rate.
 *
 * <p><strong>How it works:</strong>
 * <ol>
 *   <li>Extract API key from request header</li>
 *   <li>Get or create bucket for this API key</li>
 *   <li>Try to consume 1 token from bucket</li>
 *   <li>If successful, allow request to proceed</li>
 *   <li>If failed (no tokens), reject with 429 Too Many Requests</li>
 * </ol>
 *
 * <p><strong>Response Headers:</strong>
 * When rate limit is exceeded, the response includes:
 * <ul>
 *   <li>X-Rate-Limit-Retry-After-Seconds: Time until tokens refill</li>
 *   <li>Retry-After: Same value for HTTP standard compliance</li>
 * </ul>
 *
 * <p><strong>Filter Order:</strong>
 * This filter runs after authentication but before authorization checks.
 * Unauthenticated requests are not rate limited (they fail auth first).
 *
 * @see RateLimitProperties
 * @since 0.1.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitProperties rateLimitProperties;
    private final ObjectMapper objectMapper;

    /**
     * In-memory storage of buckets per API key.
     *
     * <p>In production, this could be moved to Redis for distributed rate limiting
     * across multiple application instances. For MVP, in-memory is sufficient.
     *
     * <p>Key: API key string
     * <p>Value: Bucket4j bucket instance
     */
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * Header name for API key.
     */
    private static final String API_KEY_HEADER = "X-API-Key";

    /**
     * Header name for retry-after information.
     */
    private static final String RETRY_AFTER_HEADER = "Retry-After";

    /**
     * Custom header for rate limit retry information.
     */
    private static final String RATE_LIMIT_RETRY_AFTER_HEADER = "X-Rate-Limit-Retry-After-Seconds";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        // Skip rate limiting if disabled
        if (!rateLimitProperties.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        // Skip rate limiting for actuator and swagger endpoints
        String requestURI = request.getRequestURI();
        if (requestURI.startsWith("/actuator/") ||
            requestURI.startsWith("/swagger-ui") ||
            requestURI.startsWith("/v3/api-docs")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extract API key
        String apiKey = request.getHeader(API_KEY_HEADER);
        if (apiKey == null || apiKey.isBlank()) {
            // No API key - let authentication filter handle it
            filterChain.doFilter(request, response);
            return;
        }

        // Get or create bucket for this API key
        Bucket bucket = resolveBucket(apiKey);

        // Try to consume 1 token
        if (bucket.tryConsume(1)) {
            // Token consumed successfully - allow request
            log.debug("Rate limit check passed for API key: {}", maskApiKey(apiKey));
            filterChain.doFilter(request, response);
        } else {
            // No tokens available - rate limit exceeded
            long waitForRefill = bucket.estimateAbilityToConsume(1).getNanosToWaitForRefill() / 1_000_000_000;

            log.warn("Rate limit exceeded for API key: {}, retry after {} seconds",
                    maskApiKey(apiKey), waitForRefill);

            // Set rate limit headers
            response.setHeader(RETRY_AFTER_HEADER, String.valueOf(waitForRefill));
            response.setHeader(RATE_LIMIT_RETRY_AFTER_HEADER, String.valueOf(waitForRefill));

            // Return 429 Too Many Requests with RFC 7807 Problem Details
            sendRateLimitExceededResponse(response, waitForRefill);
        }
    }

    /**
     * Get or create a rate limit bucket for the given API key.
     *
     * @param apiKey the API key
     * @return the bucket for this API key
     */
    private Bucket resolveBucket(String apiKey) {
        return buckets.computeIfAbsent(apiKey, key -> createNewBucket());
    }

    /**
     * Create a new rate limit bucket with configured limits.
     *
     * @return new bucket instance
     */
    private Bucket createNewBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(rateLimitProperties.getCapacity())
                .refillGreedy(
                        rateLimitProperties.getRefillTokens(),
                        rateLimitProperties.getRefillPeriod()
                )
                .build();

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Send a 429 Too Many Requests response with Problem Details.
     *
     * @param response HTTP response
     * @param retryAfterSeconds seconds until retry is allowed
     * @throws IOException if writing response fails
     */
    private void sendRateLimitExceededResponse(HttpServletResponse response, long retryAfterSeconds)
            throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.TOO_MANY_REQUESTS,
                "Rate limit exceeded. Too many requests from this API key."
        );
        problemDetail.setTitle("Too Many Requests");
        problemDetail.setType(URI.create("https://docs.authplatform.io/errors/rate-limit-exceeded"));
        problemDetail.setProperty("retryAfterSeconds", retryAfterSeconds);

        objectMapper.writeValue(response.getOutputStream(), problemDetail);
    }

    /**
     * Mask API key for logging (show only first 4 and last 4 characters).
     *
     * @param apiKey the API key to mask
     * @return masked API key
     */
    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() < 8) {
            return "***";
        }
        return apiKey.substring(0, 4) + "..." + apiKey.substring(apiKey.length() - 4);
    }
}
