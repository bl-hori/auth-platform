package io.authplatform.platform.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Composite primary key for {@link AuditLog} entity.
 *
 * <p>Required for partitioned table support. The composite key consists of:
 * <ul>
 *   <li>id: UUID identifier</li>
 *   <li>timestamp: Event timestamp (partition key)</li>
 * </ul>
 *
 * <p>This class must be serializable and implement equals() and hashCode()
 * for JPA composite key requirements.
 *
 * @see AuditLog
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogId implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * UUID identifier.
     */
    private UUID id;

    /**
     * Event timestamp (partition key).
     */
    private OffsetDateTime timestamp;
}
