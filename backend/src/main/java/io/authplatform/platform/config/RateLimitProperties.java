package io.authplatform.platform.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Configuration properties for API rate limiting.
 *
 * <p>This class holds the configuration for rate limiting applied to API endpoints.
 * Rate limits are enforced per API key to prevent abuse and ensure fair resource usage.
 *
 * <p><strong>Rate Limiting Strategy:</strong>
 * Uses the Token Bucket algorithm via Bucket4j:
 * <ul>
 *   <li>Each API key gets a bucket with a fixed capacity</li>
 *   <li>Tokens are consumed on each request</li>
 *   <li>Tokens refill at a constant rate</li>
 *   <li>Requests are rejected when bucket is empty</li>
 * </ul>
 *
 * <p>Example configuration in application.yml:
 * <pre>
 * auth-platform:
 *   rate-limit:
 *     enabled: true
 *     capacity: 100
 *     refill-tokens: 100
 *     refill-period: 1m
 * </pre>
 *
 * @since 0.1.0
 */
@Component
@ConfigurationProperties(prefix = "auth-platform.rate-limit")
@Data
public class RateLimitProperties {

    /**
     * Whether rate limiting is enabled.
     *
     * <p>When disabled, all requests are allowed through without rate checking.
     * Useful for development environments or specific deployment scenarios.
     *
     * <p>Default: true
     */
    private boolean enabled = true;

    /**
     * Maximum number of tokens (requests) allowed in the bucket.
     *
     * <p>This represents the burst capacity - how many requests can be made
     * in quick succession before rate limiting kicks in.
     *
     * <p><strong>Example:</strong>
     * With capacity=100, an API key can make 100 requests immediately,
     * then must wait for tokens to refill.
     *
     * <p>Default: 100 requests
     */
    private long capacity = 100;

    /**
     * Number of tokens to add to the bucket per refill period.
     *
     * <p>This controls the sustained request rate. Should typically match
     * the capacity to allow full bucket refill in one period.
     *
     * <p><strong>Example:</strong>
     * If refillTokens=100 and refillPeriod=1 minute, the sustained rate
     * is 100 requests per minute.
     *
     * <p>Default: 100 tokens
     */
    private long refillTokens = 100;

    /**
     * Duration of the refill period.
     *
     * <p>Tokens are added to the bucket at this interval.
     *
     * <p><strong>Common Values:</strong>
     * <ul>
     *   <li>1m (1 minute) - 100 requests per minute</li>
     *   <li>1s (1 second) - 100 requests per second</li>
     *   <li>1h (1 hour) - 100 requests per hour</li>
     * </ul>
     *
     * <p>Default: 1 minute (PT1M)
     */
    private Duration refillPeriod = Duration.ofMinutes(1);

    /**
     * Get the configured capacity.
     *
     * @return maximum bucket capacity
     */
    public long getCapacity() {
        return capacity;
    }

    /**
     * Get the number of tokens added per refill period.
     *
     * @return refill tokens count
     */
    public long getRefillTokens() {
        return refillTokens;
    }

    /**
     * Get the refill period duration.
     *
     * @return refill period
     */
    public Duration getRefillPeriod() {
        return refillPeriod;
    }

    /**
     * Check if rate limiting is enabled.
     *
     * @return true if enabled, false otherwise
     */
    public boolean isEnabled() {
        return enabled;
    }
}
