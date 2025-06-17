package de.julianweinelt.caesar.storage;

import de.julianweinelt.caesar.discord.ticket.TicketStatus;

import java.awt.*;
import java.util.List;
import java.util.UUID;

public class StorageHelperInitializer {
    public static final String[] PERMISSIONS = {
            "caesar.user.create",
            "caesar.user.delete",
            "caesar.user.edit",
            "caesar.user.list",
            "caesar.view.server",
            "caesar.view.server.services",
            "caesar.view.server.tasks",
            "caesar.view.server.tasks.manage",
            "caesar.view.server.groups",
            "caesar.view.server.groups.manage",
            "caesar.view.server.clusters",
            "caesar.view.server.clusters.manage",
            "caesar.view.files",
            "caesar.view.files.upload",
            "caesar.view.files.download",
            "caesar.view.consoles",
            "caesar.server.tasks.create",
            "caesar.server.tasks.edit",
            "caesar.server.tasks.delete",
            "caesar.server.servers.start",
            "caesar.server.servers.restart",
            "caesar.server.servers.stop",
            "caesar.server.servers.delete",
            "caesar.players.add",
            "caesar.players.edit",
            "caesar.players.remove",
            "caesar.process.create",
            "caesar.process.edit",
            "caesar.process.manage",
            "caesar.process.changestatus",
            "caesar.process.remove",
            "caesar.process.archive",
            "caesar.tickets.edit",
            "caesar.tickets.remove"
    };

    public static List<TicketStatus> getDefaultTicketStatusList() {
        return List.of(
                new TicketStatus(UUID.randomUUID(), "OPEN", "Ticket is open", java.awt.Color.GREEN),
                new TicketStatus(UUID.randomUUID(), "CLOSED", "Ticket is closed", java.awt.Color.RED),
                new TicketStatus(UUID.randomUUID(), "ARCHIVED", "Ticket has been archived", Color.GRAY)
        );
    }
}