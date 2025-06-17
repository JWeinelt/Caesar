package de.julianweinelt.caesar.discord.ticket;

import de.julianweinelt.caesar.storage.StorageFactory;
import lombok.Getter;

import java.util.UUID;

@Getter
public class Ticket {
    private final UUID uniqueID;
    private final String creator;
    private String handler;
    private final String channelID;
    private TicketStatus status;
    private final TicketType ticketType;

    public Ticket(UUID uniqueID, String creator, String handler, String channelID, TicketStatus status, TicketType ticketType) {
        this.uniqueID = uniqueID;
        this.creator = creator;
        this.handler = handler;
        this.channelID = channelID;
        this.status = status;
        this.ticketType = ticketType;
    }

    public Ticket(String creator, String handler, String channelID, TicketStatus status, TicketType ticketType) {
        this.uniqueID = UUID.randomUUID();
        this.creator = creator;
        this.handler = handler;
        this.channelID = channelID;
        this.status = status;
        this.ticketType = ticketType;
    }

    public void updateStatus(TicketStatus ticketStatus) {
        this.status = ticketStatus;
        StorageFactory.getInstance().getUsedStorage().updateTicketStatus(this, ticketStatus);
    }

    public void updateHandlingUser(String newHandler) {
        this.handler = newHandler;
        StorageFactory.getInstance().getUsedStorage().handleTicket(this, newHandler);
    }
}
