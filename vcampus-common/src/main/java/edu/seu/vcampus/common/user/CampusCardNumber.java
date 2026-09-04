package edu.seu.vcampus.common.user;

import java.util.Objects;

/** Rules for the eight-digit campus-card number used as the login account. */
public final class CampusCardNumber {

    public static final int MIN_SEQUENCE = 1;
    public static final int MAX_SEQUENCE = 9_999;

    private CampusCardNumber() {
    }

    /** Normalizes and validates a value in {@code yyyyNNNN} form. */
    public static String normalize(String value) {
        Objects.requireNonNull(value, "campusCardNumber must not be null");
        String normalized = value.trim();
        if (!hasValidShapeAndSequence(normalized)) {
            throw new IllegalArgumentException(
                    "campusCardNumber must contain 8 digits in yyyyNNNN format");
        }
        return normalized;
    }

    /** Returns whether the value is a valid campus-card number. */
    public static boolean isValid(String value) {
        return value != null && hasValidShapeAndSequence(value.trim());
    }

    /** Builds one campus-card number from a year and its four-digit sequence. */
    public static String format(int year, int sequence) {
        if (year < 2000 || year > 2999) {
            throw new IllegalArgumentException("year must be between 2000 and 2999");
        }
        if (sequence < MIN_SEQUENCE || sequence > MAX_SEQUENCE) {
            throw new IllegalArgumentException("campus-card sequence is exhausted");
        }
        return "%04d%04d".formatted(year, sequence);
    }

    /** Returns the four-digit sequence portion of a valid number. */
    public static int sequence(String value) {
        String normalized = normalize(value);
        return Integer.parseInt(normalized.substring(4));
    }

    private static boolean hasValidShapeAndSequence(String value) {
        return value.matches("2\\d{7}") && !value.endsWith("0000");
    }
}
