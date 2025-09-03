package de.julianweinelt.caesar.discord.ticket;

import java.util.UUID;

public record TicketType(UUID uniqueID, String name, String prefix, boolean showInSel, String selText, String selEmoji) {
    public static TicketType ofID(String id) {
        String i = id.replace("type-", "");
        return TicketManager.getInstance().getTicketType(i);
    }
}