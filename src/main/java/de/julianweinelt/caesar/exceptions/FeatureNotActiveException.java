package de.julianweinelt.caesar.exceptions;

public class FeatureNotActiveException extends RuntimeException {
    public FeatureNotActiveException(String feature) {
        super("The feature " + feature + " must be enabled in config before you can use it.");
    }
}
