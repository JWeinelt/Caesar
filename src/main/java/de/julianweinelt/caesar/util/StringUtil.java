package de.julianweinelt.caesar.util;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.UUID;

public class StringUtil {
    /**
     * Copies all strings from the originals iterable that start with the given token (case-insensitive)
     * into the provided collection.
     * @param token the prefix to match
     * @param originals the iterable of original strings
     * @param collection the collection to copy matching strings into
     * @param <T> the type of the collection
     * @throws UnsupportedOperationException if the collection does not support the add operation
     * @throws IllegalArgumentException if any of the arguments are null
     */
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

    /**
     * Checks if a string starts with a given prefix, ignoring case considerations.
     * @param string The string to check.
     * @param prefix The prefix to look for.
     * @return {@code true} if the string starts with the prefix, ignoring case; {@code false} otherwise.
     */
    public static boolean startsWithIgnoreCase(@NotNull final String string, @NotNull final String prefix) {
        if (string.length() < prefix.length()) {
            return false;
        }
        return string.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    /**
     * Checks if the given string contains any of the characters in searchDigits.
     * @param string the string to search in
     * @param searchDigits the characters to search for
     * @return {@code true} if any character from searchDigits is found in string, {@code false} otherwise
     */
    public static boolean contains(String string, String searchDigits) {
        for (char d : searchDigits.toCharArray()) {
            if (string.contains(d + "")) return true;
        }
        return false;
    }

    /**
     * Checks if the given input string is a valid {@link UUID}.
     * @param input the string to check
     * @return {@code true} if the input is a valid UUID, {@code false} otherwise
     */
    public static boolean isUUID(String input) {
        try {
            UUID.fromString(input);
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }
}