package de.julianweinelt.caesar.exceptions;

public class DiscordNotActiveException extends RuntimeException {
    public DiscordNotActiveException() {
        super("This action has been prevented as the Discord service is not running.");
    }
}
