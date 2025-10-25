package io.authplatform.platform.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Policy entity representing a policy definition with versioning support.
 *
 * <p>Policies define authorization rules using either Rego (Open Policy Agent)
 * or Cedar (AWS Cedar) policy languages.
 *
 * <p>Key features:
 * <ul>
 *   <li>Multi-tenant: Each policy belongs to one organization</li>
 *   <li>Versioning: Tracks current version with full version history in PolicyVersion</li>
 *   <li>Soft delete support via deleted_at timestamp</li>
 *   <li>Lifecycle management: draft → active → archived</li>
 *   <li>Policy type support: Rego (OPA) or Cedar (AWS)</li>
 *   <li>Audit trail: created_by, created_at, updated_at</li>
 * </ul>
 *
 * <p>Policy lifecycle:
 * <ul>
 *   <li>DRAFT: Policy is being developed and tested</li>
 *   <li>ACTIVE: Policy is deployed and enforcing authorization decisions</li>
 *   <li>ARCHIVED: Policy is retired but kept for audit purposes</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * // Create a new Rego policy
 * Policy policy = Policy.builder()
 *     .organization(org)
 *     .name("document-access-policy")
 *     .displayName("Document Access Policy")
 *     .description("Controls access to documents based on user roles")
 *     .policyType(PolicyType.REGO)
 *     .status(PolicyStatus.DRAFT)
 *     .createdBy(user)
 *     .build();
 * policyRepository.save(policy);
 * }</pre>
 *
 * @see Organization
 * @see PolicyVersion
 * @see io.authplatform.platform.domain.repository.PolicyRepository
 */
@Entity
@Table(
    name = "policies",
    uniqueConstraints = {
        @UniqueConstraint(name = "policies_org_name_unique", columnNames = {"organization_id", "name"})
    },
    indexes = {
        @Index(name = "idx_policies_organization_id", columnList = "organization_id"),
        @Index(name = "idx_policies_status", columnList = "status"),
        @Index(name = "idx_policies_type", columnList = "policy_type")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Policy {

    /**
     * Unique identifier for the policy.
     * Generated using UUID v4.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Organization to which this policy belongs.
     * Required for multi-tenancy isolation.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false, foreignKey = @ForeignKey(name = "policies_organization_id_fkey"))
    private Organization organization;

    /**
     * Unique policy name within the organization.
     * Must be unique per organization.
     *
     * <p>Naming convention: lowercase-with-hyphens
     * <p>Examples: "document-access-policy", "admin-override-policy"
     */
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /**
     * Human-readable display name for the policy.
     *
     * <p>Example: "Document Access Policy", "Admin Override Policy"
     */
    @Column(name = "display_name", length = 255)
    private String displayName;

    /**
     * Optional description of the policy's purpose and scope.
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Policy language type.
     *
     * <p>Supported types:
     * <ul>
     *   <li>REGO: Open Policy Agent (OPA) Rego language</li>
     *   <li>CEDAR: AWS Cedar policy language</li>
     * </ul>
     */
    @Column(name = "policy_type", nullable = false, length = 50)
    @Convert(converter = PolicyTypeConverter.class)
    @Builder.Default
    private PolicyType policyType = PolicyType.REGO;

    /**
     * Current lifecycle status of the policy.
     *
     * <p>Valid statuses:
     * <ul>
     *   <li>DRAFT: Policy is being developed, not yet active</li>
     *   <li>ACTIVE: Policy is deployed and enforcing decisions</li>
     *   <li>ARCHIVED: Policy is retired but kept for audit</li>
     * </ul>
     */
    @Column(name = "status", nullable = false, length = 50)
    @Convert(converter = PolicyStatusConverter.class)
    @Builder.Default
    private PolicyStatus status = PolicyStatus.DRAFT;

    /**
     * Current version number of the policy.
     * Incremented each time a new version is created.
     * Default value is 1 for new policies.
     */
    @Column(name = "current_version", nullable = false)
    @Builder.Default
    private Integer currentVersion = 1;

    /**
     * User who created this policy.
     * Can be null for system-created policies.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", foreignKey = @ForeignKey(name = "policies_created_by_fkey"))
    private User createdBy;

    /**
     * Timestamp when the policy was created.
     * Automatically set by the database on insert.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /**
     * Timestamp when the policy was last updated.
     * Automatically updated by database trigger on each update.
     */
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /**
     * Timestamp when the policy was soft-deleted.
     * Null if the policy is not deleted.
     */
    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    /**
     * Lifecycle callback to set timestamps before persisting.
     */
    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * Lifecycle callback to update timestamp before updating.
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    /**
     * Soft delete the policy by setting deleted_at timestamp.
     */
    public void softDelete() {
        this.deletedAt = OffsetDateTime.now();
        this.status = PolicyStatus.ARCHIVED;
    }

    /**
     * Restore a soft-deleted policy.
     */
    public void restore() {
        this.deletedAt = null;
    }

    /**
     * Check if this policy is deleted.
     *
     * @return true if deleted_at is set, false otherwise
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * Check if this policy is in draft status.
     *
     * @return true if status is DRAFT, false otherwise
     */
    public boolean isDraft() {
        return status == PolicyStatus.DRAFT;
    }

    /**
     * Check if this policy is active.
     *
     * @return true if status is ACTIVE, false otherwise
     */
    public boolean isActive() {
        return status == PolicyStatus.ACTIVE;
    }

    /**
     * Check if this policy is archived.
     *
     * @return true if status is ARCHIVED, false otherwise
     */
    public boolean isArchived() {
        return status == PolicyStatus.ARCHIVED;
    }

    /**
     * Check if this policy uses Rego language.
     *
     * @return true if policy type is REGO, false otherwise
     */
    public boolean isRego() {
        return policyType == PolicyType.REGO;
    }

    /**
     * Check if this policy uses Cedar language.
     *
     * @return true if policy type is CEDAR, false otherwise
     */
    public boolean isCedar() {
        return policyType == PolicyType.CEDAR;
    }

    /**
     * Activate the policy (transition from DRAFT to ACTIVE).
     */
    public void activate() {
        if (isDraft()) {
            this.status = PolicyStatus.ACTIVE;
        }
    }

    /**
     * Archive the policy (transition to ARCHIVED status).
     */
    public void archive() {
        this.status = PolicyStatus.ARCHIVED;
    }

    /**
     * Increment the current version number.
     * Called when a new PolicyVersion is created.
     */
    public void incrementVersion() {
        this.currentVersion++;
    }

    /**
     * Policy type enumeration.
     *
     * <p>Note: Values are stored in lowercase in the database.
     */
    public enum PolicyType {
        /**
         * Open Policy Agent (OPA) Rego policy language.
         */
        @com.fasterxml.jackson.annotation.JsonProperty("rego")
        REGO("rego"),

        /**
         * AWS Cedar policy language.
         */
        @com.fasterxml.jackson.annotation.JsonProperty("cedar")
        CEDAR("cedar");

        private final String value;

        PolicyType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * Policy status enumeration.
     *
     * <p>Note: Values are stored in lowercase in the database.
     */
    public enum PolicyStatus {
        /**
         * Policy is being developed and tested.
         */
        @com.fasterxml.jackson.annotation.JsonProperty("draft")
        DRAFT("draft"),

        /**
         * Policy is deployed and enforcing authorization decisions.
         */
        @com.fasterxml.jackson.annotation.JsonProperty("active")
        ACTIVE("active"),

        /**
         * Policy is retired but kept for audit purposes.
         */
        @com.fasterxml.jackson.annotation.JsonProperty("archived")
        ARCHIVED("archived");

        private final String value;

        PolicyStatus(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
