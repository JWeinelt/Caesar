package de.julianweinelt.caesar.discord.ticket;

import java.util.UUID;

public record TicketType(UUID uniqueID, String name, String prefix, boolean showInSel, String selText, String selEmoji) {
    /**
     * Get {@link TicketType} by name
     * @param name Name of the TicketType
     * @return {@link TicketType} or {@code null} if not found
     */
    public static TicketType valueOf(String name) { //TODO: Improve with Optional
        for (TicketType t : TicketManager.getInstance().getTicketTypes()) {
            if (t.name().equalsIgnoreCase(name)) return t;
        }
        return null;
    }
}