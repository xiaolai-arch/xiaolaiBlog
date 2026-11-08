package com.xiaolai.blog.common.validation;

import com.xiaolai.blog.common.exception.BlogException;

import java.util.Locale;

public final class SlugUtils {
    private static final String VALID_SLUG = "^[\\p{L}\\p{N}]+(?:-[\\p{L}\\p{N}]+)*$";
    public static final String PATTERN = "^$|" + VALID_SLUG;

    private SlugUtils() {
    }

    public static String from(String value) {
        if (value == null) return "";
        return value.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]+", "-")
                .replaceAll("^-+|-+$", "");
    }

    public static String requireValid(String value, String field) {
        String slug = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (slug.isEmpty() || !slug.matches(VALID_SLUG)) {
            throw new BlogException(422, field + " 只能包含文字、数字和单个连字符");
        }
        return slug;
    }
}
