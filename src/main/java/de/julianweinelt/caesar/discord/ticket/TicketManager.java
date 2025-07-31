package de.julianweinelt.caesar.discord.ticket;

import de.julianweinelt.caesar.Caesar;
import de.julianweinelt.caesar.discord.DiscordBot;
import de.julianweinelt.caesar.exceptions.FeatureNotActiveException;
import de.julianweinelt.caesar.exceptions.TicketSystemNotUsedException;
import de.julianweinelt.caesar.storage.StorageFactory;
import lombok.Getter;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class TicketManager {
    private static final Logger log = LoggerFactory.getLogger(TicketManager.class);

    @Getter
    private final List<TicketType> ticketTypes = new ArrayList<>();
    @Getter
    private final List<TicketStatus> statuses = new ArrayList<>();

    private final List<Ticket> tickets = new ArrayList<>();

    public static TicketManager getInstance() throws TicketSystemNotUsedException {
        if (Caesar.getInstance().getTicketManager() == null) throw new FeatureNotActiveException("Discord Ticket Manager");
        else return Caesar.getInstance().getTicketManager();
    }

    public static void execute(Consumer<TicketManager> managerConsumer) {
        try {
            getInstance();
        } catch (TicketSystemNotUsedException e) {
            log.debug(e.getMessage());
            return;
        }
        managerConsumer.accept(getInstance());
    }

    public void startUp(List<TicketStatus> statuses, List<TicketType> ticketTypes) {
        this.statuses.clear();
        this.ticketTypes.clear();
        this.statuses.addAll(statuses);
        this.ticketTypes.addAll(ticketTypes);
    }

    public void sendTicketCreateMessage(TextChannel channel) {

    }

    public void createTicket(String creator, TicketType type) {
        DiscordBot.getInstance().createTicketChannel(creator, type).thenAccept(channel -> {
            Ticket ticket = new Ticket(UUID.randomUUID(), creator, "", channel, getTicketStatus("OPEN"), type);
            tickets.add(ticket);
            StorageFactory.getInstance().getUsedStorage().createTicket(ticket);
        });
    }

    public TicketStatus getTicketStatus(UUID uuid) {
        for (TicketStatus ticketStatus : statuses) if (ticketStatus.uniqueID().equals(uuid)) return ticketStatus;
        return null;
    }

    public TicketStatus getTicketStatus(String name) {
        for (TicketStatus ticketStatus : statuses) if (ticketStatus.statusName().equals(name)) return ticketStatus;
        return null;
    }
    public TicketType getTicketType(UUID uuid) {
        for (TicketType ticketType : ticketTypes) if (ticketType.uniqueID().equals(uuid)) return ticketType;
        return null;
    }
}