package io.authplatform.platform.opa.client;

import io.authplatform.platform.config.OpaProperties;
import io.authplatform.platform.opa.dto.OpaRequest;
import io.authplatform.platform.opa.dto.OpaResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

/**
 * Client for communicating with OPA (Open Policy Agent) server.
 *
 * <p>This client handles HTTP communication with OPA's REST API for policy evaluation.
 * It uses Spring WebClient for non-blocking I/O and includes retry logic for resilience.
 *
 * <p><strong>Features:</strong>
 * <ul>
 *   <li>Non-blocking HTTP communication with OPA</li>
 *   <li>Automatic retry on transient failures</li>
 *   <li>Configurable timeouts</li>
 *   <li>Comprehensive error handling</li>
 *   <li>Request/response logging</li>
 * </ul>
 *
 * <p><strong>Example Usage:</strong>
 * <pre>{@code
 * @Autowired
 * private OpaClient opaClient;
 *
 * OpaRequest request = OpaRequest.builder()
 *     .input(OpaRequest.OpaInput.builder()
 *         .principal(Map.of("id", "user-123"))
 *         .action("read")
 *         .resource(Map.of("type", "document", "id", "doc-456"))
 *         .build())
 *     .build();
 *
 * OpaResponse response = opaClient.evaluatePolicy(request);
 * if (response.getResult().isAllow()) {
 *     // Grant access
 * }
 * }</pre>
 *
 * @see OpaRequest
 * @see OpaResponse
 * @see OpaProperties
 */
@Component
@ConditionalOnProperty(prefix = "opa", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class OpaClient {

    private final WebClient.Builder webClientBuilder;
    private final OpaProperties opaProperties;

    /**
     * Evaluate a policy decision with OPA.
     *
     * <p>This method sends a policy evaluation request to OPA and returns the decision.
     * It includes retry logic for transient failures and comprehensive error handling.
     *
     * @param request the OPA policy evaluation request
     * @return the OPA policy evaluation response
     * @throws OpaClientException if communication with OPA fails after retries
     */
    public OpaResponse evaluatePolicy(OpaRequest request) {
        log.debug("Evaluating policy with OPA: action={}, resource={}",
                request.getInput().getAction(),
                request.getInput().getResource().get("type"));

        try {
            WebClient webClient = webClientBuilder
                    .baseUrl(opaProperties.getBaseUrl())
                    .build();

            OpaResponse response = webClient.post()
                    .uri(opaProperties.getPolicyPath())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(OpaResponse.class)
                    .timeout(Duration.ofMillis(opaProperties.getTimeoutMs()))
                    .retryWhen(Retry.fixedDelay(opaProperties.getRetryAttempts(), Duration.ofMillis(500))
                            .filter(this::isRetryableError)
                            .doBeforeRetry(retrySignal ->
                                    log.warn("Retrying OPA request, attempt: {}", retrySignal.totalRetries() + 1)))
                    .block();

            if (response == null || response.getResult() == null) {
                throw new OpaClientException("OPA returned null or invalid response");
            }

            log.debug("OPA policy evaluation complete: allow={}, reasons={}",
                    response.getResult().isAllow(),
                    response.getResult().getReasons());

            return response;

        } catch (WebClientResponseException e) {
            log.error("OPA HTTP error: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new OpaClientException("OPA request failed with HTTP " + e.getStatusCode(), e);
        } catch (Exception e) {
            log.error("OPA communication error", e);
            throw new OpaClientException("Failed to communicate with OPA: " + e.getMessage(), e);
        }
    }

    /**
     * Check if an error is retryable.
     *
     * @param throwable the error to check
     * @return true if the error is retryable (5xx server errors, timeouts)
     */
    private boolean isRetryableError(Throwable throwable) {
        if (throwable instanceof WebClientResponseException responseException) {
            HttpStatus status = (HttpStatus) responseException.getStatusCode();
            // Retry on 5xx server errors, but not on 4xx client errors
            return status.is5xxServerError();
        }
        // Retry on timeout and connection errors
        return throwable instanceof java.util.concurrent.TimeoutException
                || throwable instanceof java.net.ConnectException;
    }

    /**
     * Exception thrown when OPA communication fails.
     */
    public static class OpaClientException extends RuntimeException {
        public OpaClientException(String message) {
            super(message);
        }

        public OpaClientException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
