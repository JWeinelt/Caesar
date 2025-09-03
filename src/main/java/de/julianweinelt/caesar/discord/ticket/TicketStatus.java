package de.julianweinelt.caesar.discord.ticket;

import java.awt.*;
import java.util.UUID;

public record TicketStatus(UUID uniqueID, String statusName, String statusDescription, Color statusColor) {
    public static TicketStatus of(String name) {
        return TicketManager.getInstance().getTicketStatus(name);
    }
}