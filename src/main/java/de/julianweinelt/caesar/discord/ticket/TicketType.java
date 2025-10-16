package de.julianweinelt.caesar.discord.ticket;

import java.util.UUID;

public record TicketType(UUID uniqueID, String name, String prefix, boolean showInSel, String selText, String selEmoji) {
    public static TicketType valueOf(String name) {
        for (TicketType t : TicketManager.getInstance().getTicketTypes()) {
            if (t.name().equalsIgnoreCase(name)) return t;
        }
        return null;
    }
}