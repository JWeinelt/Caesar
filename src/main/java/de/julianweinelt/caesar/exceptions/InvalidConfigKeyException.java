package de.julianweinelt.caesar.exceptions;

public class InvalidConfigKeyException extends RuntimeException {
    public InvalidConfigKeyException(String key, boolean readonly) {
        super("The key " + key + " of the configuration can't be changed. " + ((readonly) ? "It is read-only."
                : "It was not found in configuration system."));
    }
}
