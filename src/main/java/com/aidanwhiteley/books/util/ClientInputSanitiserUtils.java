package com.aidanwhiteley.books.util;

public class ClientInputSanitiserUtils {

    private ClientInputSanitiserUtils() {
        throw new UnsupportedOperationException("Should only be called through static methods!");
    }

    public static String sanitiseGoogleBookId(String id) {
        if (id == null) {
            return "";
        }
        // Only allow characters 0-9, a-z, A-Z (i.e. alphanumeric characters)
        return id.replaceAll("[^0-9a-zA-Z]", "");
    }

    public static boolean isValidTitleOrAuthor(String input) {
        if (input == null) {
            return false;
        }
        if (input.isEmpty() || input.length() > 150) {
            return false;
        }
        if (input.contains("\n") || input.contains("\r")) {
            return false;
        }
        String lower = input.toLowerCase();
        return !lower.contains("http://") && !lower.contains("https://");
    }

}
