package io.authplatform.platform.domain.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA AttributeConverter for {@link PolicyVersion.ValidationStatus}.
 *
 * <p>Converts between Java enum values (uppercase) and database string values (lowercase).
 * This ensures compatibility with the database CHECK constraint that expects lowercase values
 * ('pending', 'valid', 'invalid').
 *
 * <p>Example:
 * <pre>{@code
 * // Java enum -> Database
 * PENDING -> "pending"
 * VALID -> "valid"
 * INVALID -> "invalid"
 * }</pre>
 *
 * @see PolicyVersion.ValidationStatus
 */
@Converter(autoApply = false)
public class ValidationStatusConverter implements AttributeConverter<PolicyVersion.ValidationStatus, String> {

    /**
     * Converts the Java enum to the database column value.
     *
     * @param attribute the enum value to convert
     * @return the lowercase string representation for the database
     */
    @Override
    public String convertToDatabaseColumn(PolicyVersion.ValidationStatus attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getValue();
    }

    /**
     * Converts the database column value to the Java enum.
     *
     * @param dbData the database string value
     * @return the corresponding ValidationStatus enum value
     * @throws IllegalArgumentException if the database value doesn't match any enum value
     */
    @Override
    public PolicyVersion.ValidationStatus convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }

        for (PolicyVersion.ValidationStatus status : PolicyVersion.ValidationStatus.values()) {
            if (status.getValue().equals(dbData)) {
                return status;
            }
        }

        throw new IllegalArgumentException("Unknown database value for ValidationStatus: " + dbData);
    }
}
