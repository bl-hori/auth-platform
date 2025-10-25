package io.authplatform.platform.domain.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA AttributeConverter for {@link Permission.PermissionEffect}.
 *
 * <p>Converts between Java enum values (uppercase) and database string values (lowercase).
 * This ensures compatibility with the database CHECK constraint that expects lowercase values
 * ('allow', 'deny').
 *
 * <p>Example:
 * <pre>{@code
 * // Java enum -> Database
 * ALLOW -> "allow"
 * DENY -> "deny"
 * }</pre>
 *
 * @see Permission.PermissionEffect
 */
@Converter(autoApply = false)
public class PermissionEffectConverter implements AttributeConverter<Permission.PermissionEffect, String> {

    /**
     * Converts the Java enum to the database column value.
     *
     * @param attribute the enum value to convert
     * @return the lowercase string representation for the database
     */
    @Override
    public String convertToDatabaseColumn(Permission.PermissionEffect attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getValue();
    }

    /**
     * Converts the database column value to the Java enum.
     *
     * @param dbData the database string value
     * @return the corresponding PermissionEffect enum value
     * @throws IllegalArgumentException if the database value doesn't match any enum value
     */
    @Override
    public Permission.PermissionEffect convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }

        for (Permission.PermissionEffect effect : Permission.PermissionEffect.values()) {
            if (effect.getValue().equals(dbData)) {
                return effect;
            }
        }

        throw new IllegalArgumentException("Unknown database value for PermissionEffect: " + dbData);
    }
}
