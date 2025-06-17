package de.julianweinelt.caesar.exceptions;

public class TicketSystemNotUsedException extends RuntimeException {
    public TicketSystemNotUsedException() {
        super("The ticket system has not been activated by the user.");
    }
}
