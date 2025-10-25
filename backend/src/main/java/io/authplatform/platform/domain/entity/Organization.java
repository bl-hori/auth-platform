package io.authplatform.platform.domain.entity;

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
 * Organization entity representing a multi-tenant organization.
 *
 * <p>Organizations provide isolation between different tenants in the authorization platform.
 * Each organization has its own set of users, roles, permissions, and policies.
 *
 * <p>Key features:
 * <ul>
 *   <li>Soft delete support via {@code deletedAt} timestamp</li>
 *   <li>JSONB settings field for flexible configuration</li>
 *   <li>Status management (active, suspended, deleted)</li>
 *   <li>Automatic timestamp management for created/updated dates</li>
 * </ul>
 *
 * @see io.authplatform.platform.domain.repository.OrganizationRepository
 */
@Entity
@Table(name = "organizations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Organization {

    /**
     * Unique identifier for the organization.
     * Generated using UUID v4.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Unique name identifier for the organization.
     * Used for URL-safe references and API calls.
     *
     * <p>Example: "acme-corp", "example-org"
     */
    @Column(name = "name", nullable = false, unique = true, length = 255)
    private String name;

    /**
     * Human-readable display name for the organization.
     *
     * <p>Example: "Acme Corporation", "Example Organization Inc."
     */
    @Column(name = "display_name", length = 255)
    private String displayName;

    /**
     * Optional description of the organization.
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Current status of the organization.
     *
     * <p>Valid values:
     * <ul>
     *   <li>active - Organization is fully operational</li>
     *   <li>suspended - Organization is temporarily disabled</li>
     *   <li>deleted - Organization is soft-deleted</li>
     * </ul>
     */
    @Column(name = "status", nullable = false, length = 50)
    @Convert(converter = OrganizationStatusConverter.class)
    @Builder.Default
    private OrganizationStatus status = OrganizationStatus.ACTIVE;

    /**
     * Custom settings for the organization stored as JSONB.
     *
     * <p>This field allows flexible configuration without schema changes.
     * Common use cases:
     * <ul>
     *   <li>Branding configuration (logo URL, colors)</li>
     *   <li>Feature flags</li>
     *   <li>Integration settings</li>
     *   <li>Custom business rules</li>
     * </ul>
     *
     * <p>Example:
     * <pre>{@code
     * {
     *   "branding": {
     *     "logoUrl": "https://example.com/logo.png",
     *     "primaryColor": "#3B82F6"
     *   },
     *   "features": {
     *     "mfaRequired": true,
     *     "sessionTimeout": 3600
     *   }
     * }
     * }</pre>
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "settings", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> settings = Map.of();

    /**
     * Timestamp when the organization was created.
     * Automatically set by the database on insert.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /**
     * Timestamp when the organization was last updated.
     * Automatically updated by database trigger on each update.
     */
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /**
     * Timestamp when the organization was soft-deleted.
     *
     * <p>When not null, the organization is considered deleted and should be
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
     * Check if the organization is deleted.
     *
     * @return true if deletedAt is not null, false otherwise
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * Check if the organization is active.
     *
     * @return true if status is ACTIVE and not deleted, false otherwise
     */
    public boolean isActive() {
        return status == OrganizationStatus.ACTIVE && !isDeleted();
    }

    /**
     * Soft delete the organization by setting deletedAt timestamp and updating status.
     */
    public void softDelete() {
        this.deletedAt = OffsetDateTime.now();
        this.status = OrganizationStatus.DELETED;
    }

    /**
     * Restore a soft-deleted organization.
     */
    public void restore() {
        this.deletedAt = null;
        this.status = OrganizationStatus.ACTIVE;
    }

    /**
     * Organization status enumeration.
     *
     * <p>Note: Values are stored in lowercase in the database.
     */
    public enum OrganizationStatus {
        /**
         * Organization is fully operational.
         */
        @com.fasterxml.jackson.annotation.JsonProperty("active")
        ACTIVE("active"),

        /**
         * Organization is temporarily disabled.
         * Users cannot access resources but data is preserved.
         */
        @com.fasterxml.jackson.annotation.JsonProperty("suspended")
        SUSPENDED("suspended"),

        /**
         * Organization is soft-deleted.
         * Can be restored if needed.
         */
        @com.fasterxml.jackson.annotation.JsonProperty("deleted")
        DELETED("deleted");

        private final String value;

        OrganizationStatus(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
