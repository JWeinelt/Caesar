package de.julianweinelt.caesar.exceptions;

public class LanguageAlreadyRegisteredException extends RuntimeException {
    public LanguageAlreadyRegisteredException(String language) {
        super("The language '" + language + "' is already registered");
    }
}
