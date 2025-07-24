package de.julianweinelt.caesar.storage;

import de.julianweinelt.caesar.discord.ticket.TicketStatus;

import java.awt.Color;
import java.util.List;
import java.util.UUID;

public class StorageHelperInitializer {
    public static final String[] PERMISSIONS = {
            "caesar.admin.user.create",
            "caesar.admin.user.delete",
            "caesar.admin.user.view",
            "caesar.admin.user.edit",
            "caesar.admin.user.list",
            "caesar.admin.user.permission.add",
            "caesar.admin.user.permission.remove",
            "caesar.admin.user.role.assign",
            "caesar.admin.role.create",
            "caesar.admin.role.delete",
            "caesar.admin.role.edit",
            "caesar.admin.role.view",
            "caesar.admin.role.list",
            "caesar.admin.discord.manage",
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
            "caesar.players.create",
            "caesar.players.edit",
            "caesar.players.delete",
            "caesar.players.view",
            "caesar.players.notes.view",
            "caesar.players.notes.create",
            "caesar.players.notes.delete",
            "caesar.process.create",
            "caesar.process.edit",
            "caesar.process.change-status",
            "caesar.process.assign-player",
            "caesar.process.remove",
            "caesar.process.archive",
            "caesar.process.type.create",
            "caesar.process.type.delete",
            "caesar.process.status.create",
            "caesar.process.status.delete",
            "caesar.tickets.edit",
            "caesar.tickets.remove",
            "caesar.tickets.archive",
            "caesar.connections.create",
            "caesar.connections.disable",
            "caesar.connections.enable",
            "caesar.connections.delete",
            "caesar.connections.update",
            "caesar.connections.view",
            "caesar.link.ban",
            "caesar.link.unban",
            "caesar.link.mute",
            "caesar.link.unmute",
            "caesar.link.kick",
            "caesar.link.warn",
            "caesar.link.punishment.view"
    };

    public static List<TicketStatus> getDefaultTicketStatusList() {
        return List.of(
                new TicketStatus(UUID.randomUUID(), "OPEN", "Ticket is open", Color.GREEN),
                new TicketStatus(UUID.randomUUID(), "CLOSED", "Ticket is closed", Color.RED),
                new TicketStatus(UUID.randomUUID(), "ARCHIVED", "Ticket has been archived", Color.GRAY)
        );
    }

}