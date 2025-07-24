package de.julianweinelt.caesar.util;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.UUID;

public class StringUtil {
    public static <T extends Collection<? super String>> void copyPartialMatches(@NotNull final String token,
                                                                                 @NotNull final Iterable<String> originals,
                                                                                 @NotNull final T collection)
            throws UnsupportedOperationException, IllegalArgumentException {

        for (String string : originals) {
            if (startsWithIgnoreCase(string, token)) {
                collection.add(string);
            }
        }

    }

    public static boolean startsWithIgnoreCase(@NotNull final String string, @NotNull final String prefix)
            throws IllegalArgumentException, NullPointerException {
        if (string.length() < prefix.length()) {
            return false;
        }
        return string.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    public static boolean contains(String string, String searchDigits) {
        for (char d : searchDigits.toCharArray()) {
            if (string.contains(d + "")) return true;
        }
        return false;
    }

    public static boolean isUUID(String input) {
        try {
            UUID.fromString(input);
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }
}