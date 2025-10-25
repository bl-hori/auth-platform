package io.authplatform.platform.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * PolicyVersion entity representing a versioned snapshot of a policy.
 *
 * <p>This entity maintains the complete version history of policies,
 * enabling audit trails, rollbacks, and A/B testing.
 *
 * <p>Key features:
 * <ul>
 *   <li>Version tracking: Each policy has multiple versions</li>
 *   <li>Content integrity: SHA-256 checksum for content verification</li>
 *   <li>Validation status: Tracks syntax/semantic validation results</li>
 *   <li>Publication tracking: Records when/who published each version</li>
 *   <li>Validation errors: JSONB field for detailed error information</li>
 *   <li>Immutability: Once created, versions should not be modified</li>
 * </ul>
 *
 * <p>Validation statuses:
 * <ul>
 *   <li>PENDING: Validation not yet performed</li>
 *   <li>VALID: Policy content passed validation</li>
 *   <li>INVALID: Policy content failed validation</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * // Create a new policy version
 * String policyContent = "package authz\nallow { ... }";
 * PolicyVersion version = PolicyVersion.builder()
 *     .policy(policy)
 *     .version(2)
 *     .content(policyContent)
 *     .createdBy(user)
 *     .build();
 * version.calculateChecksum(); // Automatically calculates SHA-256
 * policyVersionRepository.save(version);
 *
 * // Publish the version
 * version.publish(user);
 * policyVersionRepository.save(version);
 * }</pre>
 *
 * @see Policy
 * @see io.authplatform.platform.domain.repository.PolicyVersionRepository
 */
@Entity
@Table(
    name = "policy_versions",
    uniqueConstraints = {
        @UniqueConstraint(name = "policy_versions_unique", columnNames = {"policy_id", "version"})
    },
    indexes = {
        @Index(name = "idx_policy_versions_policy_id", columnList = "policy_id"),
        @Index(name = "idx_policy_versions_validation_status", columnList = "validation_status"),
        @Index(name = "idx_policy_versions_published_at", columnList = "published_at")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PolicyVersion {

    /**
     * Unique identifier for the policy version.
     * Generated using UUID v4.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Policy to which this version belongs.
     * Cascade delete ensures versions are removed when policy is deleted.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_id", nullable = false, foreignKey = @ForeignKey(name = "policy_versions_policy_id_fkey"))
    private Policy policy;

    /**
     * Version number within the policy.
     * Starts at 1 and increments sequentially.
     * Must be unique per policy.
     */
    @Column(name = "version", nullable = false)
    private Integer version;

    /**
     * Policy content in Rego or Cedar language.
     * This is the actual policy code that will be evaluated.
     * Should be immutable once created.
     */
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * SHA-256 checksum of the content.
     * Used for integrity verification and deduplication.
     * Automatically calculated from content.
     *
     * <p>Format: 64-character hexadecimal string
     */
    @Column(name = "checksum", nullable = false, length = 64)
    private String checksum;

    /**
     * Validation status of the policy content.
     *
     * <p>Valid statuses:
     * <ul>
     *   <li>PENDING: Validation not yet performed</li>
     *   <li>VALID: Policy content passed syntax and semantic validation</li>
     *   <li>INVALID: Policy content failed validation</li>
     * </ul>
     */
    @Column(name = "validation_status", nullable = false, length = 50)
    @Convert(converter = ValidationStatusConverter.class)
    @Builder.Default
    private ValidationStatus validationStatus = ValidationStatus.PENDING;

    /**
     * Validation errors in JSONB format.
     * Contains detailed error messages from syntax/semantic validation.
     * Null if validation passed or not yet performed.
     *
     * <p>Example structure:
     * <pre>{@code
     * {
     *   "errors": [
     *     {
     *       "line": 5,
     *       "column": 10,
     *       "message": "Undefined variable 'user'",
     *       "severity": "error"
     *     }
     *   ]
     * }
     * }</pre>
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "validation_errors", columnDefinition = "jsonb")
    private Map<String, Object> validationErrors;

    /**
     * Timestamp when this version was published (made active).
     * Null if the version has not been published yet.
     */
    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    /**
     * User who published this version.
     * Null if not yet published or published by system.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "published_by", foreignKey = @ForeignKey(name = "policy_versions_published_by_fkey"))
    private User publishedBy;

    /**
     * User who created this version.
     * Can be null for system-created versions.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", foreignKey = @ForeignKey(name = "policy_versions_created_by_fkey"))
    private User createdBy;

    /**
     * Timestamp when this version was created.
     * Automatically set on insert.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /**
     * Lifecycle callback to set creation timestamp before persisting.
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
        if (this.checksum == null && this.content != null) {
            calculateChecksum();
        }
    }

    /**
     * Calculate and set the SHA-256 checksum of the content.
     * Should be called before saving if content is set.
     */
    public void calculateChecksum() {
        if (content == null) {
            throw new IllegalStateException("Cannot calculate checksum: content is null");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            this.checksum = bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Convert byte array to hexadecimal string.
     *
     * @param bytes the byte array
     * @return hexadecimal string representation
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    /**
     * Verify that the content matches the stored checksum.
     *
     * @return true if checksum matches, false otherwise
     */
    public boolean verifyChecksum() {
        if (content == null || checksum == null) {
            return false;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            String calculatedChecksum = bytesToHex(hash);
            return checksum.equals(calculatedChecksum);
        } catch (NoSuchAlgorithmException e) {
            return false;
        }
    }

    /**
     * Mark this version as published by a user.
     *
     * @param publisher the user who is publishing this version
     */
    public void publish(User publisher) {
        this.publishedAt = OffsetDateTime.now();
        this.publishedBy = publisher;
    }

    /**
     * Check if this version has been published.
     *
     * @return true if publishedAt is set, false otherwise
     */
    public boolean isPublished() {
        return publishedAt != null;
    }

    /**
     * Check if this version is validated as valid.
     *
     * @return true if validation status is VALID, false otherwise
     */
    public boolean isValid() {
        return validationStatus == ValidationStatus.VALID;
    }

    /**
     * Check if this version is validated as invalid.
     *
     * @return true if validation status is INVALID, false otherwise
     */
    public boolean isInvalid() {
        return validationStatus == ValidationStatus.INVALID;
    }

    /**
     * Check if validation is pending.
     *
     * @return true if validation status is PENDING, false otherwise
     */
    public boolean isPending() {
        return validationStatus == ValidationStatus.PENDING;
    }

    /**
     * Mark this version as valid (passed validation).
     */
    public void markAsValid() {
        this.validationStatus = ValidationStatus.VALID;
        this.validationErrors = null;
    }

    /**
     * Mark this version as invalid (failed validation).
     *
     * @param errors the validation errors
     */
    public void markAsInvalid(Map<String, Object> errors) {
        this.validationStatus = ValidationStatus.INVALID;
        this.validationErrors = errors;
    }

    /**
     * Check if this version has validation errors.
     *
     * @return true if validationErrors is not null and not empty, false otherwise
     */
    public boolean hasValidationErrors() {
        return validationErrors != null && !validationErrors.isEmpty();
    }

    /**
     * Get the size of the content in bytes.
     *
     * @return content size in bytes, or 0 if content is null
     */
    public int getContentSize() {
        return content != null ? content.getBytes(StandardCharsets.UTF_8).length : 0;
    }

    /**
     * Validation status enumeration.
     *
     * <p>Note: Values are stored in lowercase in the database.
     */
    public enum ValidationStatus {
        /**
         * Validation not yet performed.
         */
        @com.fasterxml.jackson.annotation.JsonProperty("pending")
        PENDING("pending"),

        /**
         * Policy content passed validation.
         */
        @com.fasterxml.jackson.annotation.JsonProperty("valid")
        VALID("valid"),

        /**
         * Policy content failed validation.
         */
        @com.fasterxml.jackson.annotation.JsonProperty("invalid")
        INVALID("invalid");

        private final String value;

        ValidationStatus(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
