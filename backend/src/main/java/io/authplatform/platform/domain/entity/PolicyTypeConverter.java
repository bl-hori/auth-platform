package io.authplatform.platform.domain.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA AttributeConverter for {@link Policy.PolicyType}.
 *
 * <p>Converts between Java enum values (uppercase) and database string values (lowercase).
 * This ensures compatibility with the database CHECK constraint that expects lowercase values
 * ('rego', 'cedar').
 *
 * <p>Example:
 * <pre>{@code
 * // Java enum -> Database
 * REGO -> "rego"
 * CEDAR -> "cedar"
 * }</pre>
 *
 * @see Policy.PolicyType
 */
@Converter(autoApply = false)
public class PolicyTypeConverter implements AttributeConverter<Policy.PolicyType, String> {

    /**
     * Converts the Java enum to the database column value.
     *
     * @param attribute the enum value to convert
     * @return the lowercase string representation for the database
     */
    @Override
    public String convertToDatabaseColumn(Policy.PolicyType attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getValue();
    }

    /**
     * Converts the database column value to the Java enum.
     *
     * @param dbData the database string value
     * @return the corresponding PolicyType enum value
     * @throws IllegalArgumentException if the database value doesn't match any enum value
     */
    @Override
    public Policy.PolicyType convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }

        for (Policy.PolicyType type : Policy.PolicyType.values()) {
            if (type.getValue().equals(dbData)) {
                return type;
            }
        }

        throw new IllegalArgumentException("Unknown database value for PolicyType: " + dbData);
    }
}
