package com.lawground.global.validation;

public final class TextPolicy {
    private TextPolicy() {}

    private static boolean whitespace(int codePoint) {
        return Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint);
    }

    public static String normalize(String value) {
        if (value == null) return null;
        String normalized = value.replace("\r\n", "\n").replace('\r', '\n');
        int start = 0, end = normalized.length();
        while (start < end && whitespace(normalized.codePointAt(start)))
            start += Character.charCount(normalized.codePointAt(start));
        while (end > start && whitespace(normalized.codePointBefore(end)))
            end -= Character.charCount(normalized.codePointBefore(end));
        return normalized.substring(start, end);
    }

    public static boolean valid(String value, int max) {
        return value != null
                && !value.isEmpty()
                && value.codePointCount(0, value.length()) <= max
                && value.codePoints().anyMatch(point -> !whitespace(point))
                && value.codePoints()
                        .noneMatch(point -> point == 0 || point >= 0xD800 && point <= 0xDFFF);
    }
}
