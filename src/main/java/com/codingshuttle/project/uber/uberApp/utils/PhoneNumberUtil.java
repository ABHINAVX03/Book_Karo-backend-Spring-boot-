package com.codingshuttle.project.uber.uberApp.utils;

public final class PhoneNumberUtil {

    private PhoneNumberUtil() {
    }

    public static String toDialablePhoneNumber(String phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }

        String trimmed = phoneNumber.trim();
        if (trimmed.isEmpty()) {
            return "";
        }

        String digits = trimmed.replaceAll("\\D", "");
        if (digits.isEmpty()) {
            return "";
        }

        return trimmed.startsWith("+") ? "+" + digits : "+" + digits;
    }
}
