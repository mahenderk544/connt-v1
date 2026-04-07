package com.connto.backend.util;

public final class PhoneNormalizer {

    private PhoneNormalizer() {}

    /**
     * Normalizes a mobile number for login: strips spaces/dashes/parens; keeps leading + when
     * present. Requires 10–15 digits (E.164-style local length).
     */
    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Mobile number is required");
        }
        String t = raw.trim();
        boolean hasPlus = t.startsWith("+");
        String digitsOnly = t.replaceAll("[^0-9]", "");
        if (digitsOnly.length() < 10 || digitsOnly.length() > 15) {
            throw new IllegalArgumentException(
                    "Enter a valid mobile number (10–15 digits, optional leading +)");
        }
        return hasPlus ? "+" + digitsOnly : digitsOnly;
    }
}
