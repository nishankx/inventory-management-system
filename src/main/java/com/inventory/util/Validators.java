package com.inventory.util;

import com.inventory.exception.ValidationException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Pattern;

/** Central input validation so every entry point enforces the same rules. */
public final class Validators {
    private static final Pattern SKU = Pattern.compile("^[A-Za-z0-9._-]{1,32}$");

    private Validators() { }

    public static String sku(String value) {
        if (value == null || !SKU.matcher(value).matches()) {
            throw new ValidationException("SKU must be 1-32 chars of letters, digits, '.', '_' or '-'");
        }
        return value.toUpperCase();
    }

    public static String text(String value, String field, int maxLen) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(field + " must not be blank");
        }
        String v = value.strip();
        if (v.length() > maxLen) {
            throw new ValidationException(field + " must be at most " + maxLen + " characters");
        }
        if (v.chars().anyMatch(Character::isISOControl)) {
            throw new ValidationException(field + " must not contain control characters");
        }
        return v;
    }

    public static <T> T notNull(T value, String field) {
        if (value == null) throw new ValidationException(field + " is required");
        return value;
    }

    public static BigDecimal money(BigDecimal value) {
        notNull(value, "price");
        if (value.signum() < 0) throw new ValidationException("price must not be negative");
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    public static int nonNegative(int value, String field) {
        if (value < 0) throw new ValidationException(field + " must not be negative");
        return value;
    }

    public static int positive(int value, String field) {
        if (value <= 0) throw new ValidationException(field + " must be greater than zero");
        return value;
    }
}
