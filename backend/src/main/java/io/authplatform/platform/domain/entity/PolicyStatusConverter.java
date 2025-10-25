package io.authplatform.platform.domain.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA AttributeConverter for {@link Policy.PolicyStatus}.
 *
 * <p>Converts between Java enum values (uppercase) and database string values (lowercase).
 * This ensures compatibility with the database CHECK constraint that expects lowercase values
 * ('draft', 'active', 'archived').
 *
 * <p>Example:
 * <pre>{@code
 * // Java enum -> Database
 * DRAFT -> "draft"
 * ACTIVE -> "active"
 * ARCHIVED -> "archived"
 * }</pre>
 *
 * @see Policy.PolicyStatus
 */
@Converter(autoApply = false)
public class PolicyStatusConverter implements AttributeConverter<Policy.PolicyStatus, String> {

    /**
     * Converts the Java enum to the database column value.
     *
     * @param attribute the enum value to convert
     * @return the lowercase string representation for the database
     */
    @Override
    public String convertToDatabaseColumn(Policy.PolicyStatus attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getValue();
    }

    /**
     * Converts the database column value to the Java enum.
     *
     * @param dbData the database string value
     * @return the corresponding PolicyStatus enum value
     * @throws IllegalArgumentException if the database value doesn't match any enum value
     */
    @Override
    public Policy.PolicyStatus convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }

        for (Policy.PolicyStatus status : Policy.PolicyStatus.values()) {
            if (status.getValue().equals(dbData)) {
                return status;
            }
        }

        throw new IllegalArgumentException("Unknown database value for PolicyStatus: " + dbData);
    }
}
