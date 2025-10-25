package io.authplatform.platform.domain.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA AttributeConverter for {@link User.UserStatus}.
 *
 * <p>Converts between Java enum values (uppercase) and database string values (lowercase).
 * This ensures compatibility with the database CHECK constraint that expects lowercase values
 * ('active', 'inactive', 'suspended', 'deleted').
 *
 * <p>Example:
 * <pre>{@code
 * // Java enum -> Database
 * ACTIVE -> "active"
 * INACTIVE -> "inactive"
 * SUSPENDED -> "suspended"
 * DELETED -> "deleted"
 * }</pre>
 *
 * @see User.UserStatus
 */
@Converter(autoApply = false)
public class UserStatusConverter implements AttributeConverter<User.UserStatus, String> {

    /**
     * Converts the Java enum to the database column value.
     *
     * @param attribute the enum value to convert
     * @return the lowercase string representation for the database
     */
    @Override
    public String convertToDatabaseColumn(User.UserStatus attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getValue();
    }

    /**
     * Converts the database column value to the Java enum.
     *
     * @param dbData the database string value
     * @return the corresponding UserStatus enum value
     * @throws IllegalArgumentException if the database value doesn't match any enum value
     */
    @Override
    public User.UserStatus convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }

        for (User.UserStatus status : User.UserStatus.values()) {
            if (status.getValue().equals(dbData)) {
                return status;
            }
        }

        throw new IllegalArgumentException("Unknown database value for UserStatus: " + dbData);
    }
}
