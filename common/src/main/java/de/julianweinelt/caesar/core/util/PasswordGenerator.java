package de.julianweinelt.caesar.core.util;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PasswordGenerator {

    private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String SPECIAL_CHARACTERS = "!@#$%^&*()-_=+<>?";

    private static final String ALL_CHARACTERS = UPPERCASE + LOWERCASE + DIGITS + SPECIAL_CHARACTERS;
    private static final SecureRandom RANDOM = new SecureRandom();

    public static String generatePassword(int length) {
        if (length < 8) {
            throw new IllegalArgumentException("Password must contain at least 8 characters!");
        }

        List<Character> passwordChars = new ArrayList<>();

        // Garantiert, dass jede Zeichenklasse mindestens einmal vorkommt
        passwordChars.add(getRandomChar(UPPERCASE));
        passwordChars.add(getRandomChar(LOWERCASE));
        passwordChars.add(getRandomChar(DIGITS));
        passwordChars.add(getRandomChar(SPECIAL_CHARACTERS));

        // Füllt den Rest des Passworts mit zufälligen Zeichen
        for (int i = 4; i < length; i++) {
            passwordChars.add(getRandomChar(ALL_CHARACTERS));
        }

        // Durchmischen für bessere Zufälligkeit
        Collections.shuffle(passwordChars);

        // Umwandeln in einen String
        StringBuilder password = new StringBuilder();
        for (char c : passwordChars) {
            password.append(c);
        }

        return password.toString();
    }

    private static char getRandomChar(String charSet) {
        return charSet.charAt(RANDOM.nextInt(charSet.length()));
    }
}
