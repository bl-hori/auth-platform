package io.authplatform.platform.domain.entity;

import io.authplatform.platform.domain.listener.UserCacheInvalidationListener;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * User entity representing a user account within an organization.
 *
 * <p>Users belong to a specific organization and have email-based identity.
 * Each user can have custom attributes for ABAC (Attribute-Based Access Control)
 * and external identity provider integration.
 *
 * <p>Key features:
 * <ul>
 *   <li>Multi-tenant: Each user belongs to one organization</li>
 *   <li>Soft delete support via {@code deletedAt} timestamp</li>
 *   <li>JSONB attributes field for flexible ABAC policies</li>
 *   <li>Status management (active, inactive, suspended, deleted)</li>
 *   <li>External IdP integration via {@code externalId}</li>
 *   <li>Automatic timestamp management for created/updated dates</li>
 * </ul>
 *
 * @see Organization
 * @see io.authplatform.platform.domain.repository.UserRepository
 */
@Entity
@Table(
    name = "users",
    uniqueConstraints = {
        @UniqueConstraint(name = "users_org_email_unique", columnNames = {"organization_id", "email"}),
        @UniqueConstraint(name = "users_org_username_unique", columnNames = {"organization_id", "username"})
    },
    indexes = {
        @Index(name = "idx_users_organization_id", columnList = "organization_id"),
        @Index(name = "idx_users_email", columnList = "email"),
        @Index(name = "idx_users_external_id", columnList = "external_id")
    }
)
@EntityListeners(UserCacheInvalidationListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    /**
     * Unique identifier for the user.
     * Generated using UUID v4.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Organization to which this user belongs.
     * Required for multi-tenancy isolation.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false, foreignKey = @ForeignKey(name = "users_organization_id_fkey"))
    private Organization organization;

    /**
     * User's email address.
     * Must be unique within the organization.
     *
     * <p>Example: "user@example.com"
     */
    @Column(name = "email", nullable = false, length = 255)
    private String email;

    /**
     * Optional username for the user.
     * If provided, must be unique within the organization.
     *
     * <p>Example: "john.doe", "jdoe"
     */
    @Column(name = "username", length = 255)
    private String username;

    /**
     * Human-readable display name for the user.
     *
     * <p>Example: "John Doe", "Jane Smith"
     */
    @Column(name = "display_name", length = 255)
    private String displayName;

    /**
     * External identity provider user ID for SCIM synchronization.
     *
     * <p>Used for integrating with external IdPs (Okta, Azure AD, etc.)
     * in Phase 2. This field maps the internal user to an external user identity.
     *
     * <p>Example: "okta:00u1234567890abcdef", "azuread:a1b2c3d4-e5f6-7890-abcd-ef1234567890"
     */
    @Column(name = "external_id", length = 255)
    private String externalId;

    /**
     * Current status of the user.
     *
     * <p>Valid values:
     * <ul>
     *   <li>active - User can authenticate and access resources</li>
     *   <li>inactive - User account exists but cannot authenticate</li>
     *   <li>suspended - User is temporarily disabled</li>
     *   <li>deleted - User is soft-deleted</li>
     * </ul>
     */
    @Column(name = "status", nullable = false, length = 50)
    @Convert(converter = UserStatusConverter.class)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    /**
     * Custom attributes for the user stored as JSONB.
     *
     * <p>This field enables Attribute-Based Access Control (ABAC) by storing
     * arbitrary user attributes that can be referenced in policy conditions.
     *
     * <p>Common use cases:
     * <ul>
     *   <li>User properties (department, title, location)</li>
     *   <li>Security attributes (clearance level, classification)</li>
     *   <li>Business attributes (cost center, region, business unit)</li>
     *   <li>Custom metadata for policy evaluation</li>
     * </ul>
     *
     * <p>Example:
     * <pre>{@code
     * {
     *   "department": "Engineering",
     *   "title": "Senior Developer",
     *   "location": "US-West",
     *   "clearanceLevel": "confidential",
     *   "costCenter": "CC-1234"
     * }
     * }</pre>
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attributes", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> attributes = Map.of();

    /**
     * Timestamp when the user was created.
     * Automatically set by the database on insert.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /**
     * Timestamp when the user was last updated.
     * Automatically updated by database trigger on each update.
     */
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /**
     * Timestamp when the user was soft-deleted.
     *
     * <p>When not null, the user is considered deleted and should be
     * excluded from normal queries. Use {@link #isDeleted()} to check deletion status.
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
     * Check if the user is deleted.
     *
     * @return true if deletedAt is not null, false otherwise
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * Check if the user is active.
     *
     * @return true if status is ACTIVE and not deleted, false otherwise
     */
    public boolean isActive() {
        return status == UserStatus.ACTIVE && !isDeleted();
    }

    /**
     * Soft delete the user by setting deletedAt timestamp and updating status.
     */
    public void softDelete() {
        this.deletedAt = OffsetDateTime.now();
        this.status = UserStatus.DELETED;
    }

    /**
     * Restore a soft-deleted user.
     */
    public void restore() {
        this.deletedAt = null;
        this.status = UserStatus.ACTIVE;
    }

    /**
     * User status enumeration.
     *
     * <p>Note: Values are stored in lowercase in the database.
     */
    public enum UserStatus {
        /**
         * User can authenticate and access resources.
         */
        @com.fasterxml.jackson.annotation.JsonProperty("active")
        ACTIVE("active"),

        /**
         * User account exists but cannot authenticate.
         */
        @com.fasterxml.jackson.annotation.JsonProperty("inactive")
        INACTIVE("inactive"),

        /**
         * User is temporarily disabled.
         */
        @com.fasterxml.jackson.annotation.JsonProperty("suspended")
        SUSPENDED("suspended"),

        /**
         * User is soft-deleted.
         */
        @com.fasterxml.jackson.annotation.JsonProperty("deleted")
        DELETED("deleted");

        private final String value;

        UserStatus(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
