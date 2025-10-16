package de.julianweinelt.caesar.discord.ticket;

import java.awt.*;
import java.util.UUID;

public record TicketStatus(UUID uniqueID, String statusName, String statusDescription, Color statusColor) {
    public static TicketStatus valueOf(String name) {
        for (TicketStatus t : TicketManager.getInstance().getStatuses()) {
            if (t.statusName().equalsIgnoreCase(name)) return t;
        }
        return null;
    }
    public TicketStatus[] values() {
        return TicketManager.getInstance().getStatuses().toArray(new TicketStatus[TicketManager.getInstance().getStatuses().size()]);
    }
}