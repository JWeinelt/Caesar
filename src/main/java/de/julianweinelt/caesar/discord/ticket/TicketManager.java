package de.julianweinelt.caesar.discord.ticket;

import de.julianweinelt.caesar.Caesar;
import de.julianweinelt.caesar.discord.DiscordBot;
import de.julianweinelt.caesar.exceptions.FeatureNotActiveException;
import de.julianweinelt.caesar.exceptions.TicketSystemNotUsedException;
import de.julianweinelt.caesar.storage.StorageFactory;
import lombok.Getter;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
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

    /**
     * Executes the given consumer if the TicketManager is available.
     * @param managerConsumer The consumer to execute with the TicketManager.
     */
    public static void execute(Consumer<TicketManager> managerConsumer) {
        try {
            getInstance();
        } catch (TicketSystemNotUsedException e) {
            log.debug(e.getMessage());
            return;
        }
        managerConsumer.accept(getInstance());
    }

    /**
     * Initializes the TicketManager with the given statuses and ticket types.
     * @param statuses A list of {@link TicketStatus}'s.
     * @param ticketTypes A list of {@link TicketType}s.
     */
    @ApiStatus.Internal
    public void startUp(List<TicketStatus> statuses, List<TicketType> ticketTypes) {
        this.statuses.clear();
        this.ticketTypes.clear();
        this.statuses.addAll(statuses);
        this.ticketTypes.addAll(ticketTypes);
        DiscordBot.getInstance().registerListener(new TicketListener());
    }

    /**
     * Sends a ticket creation message to the given channel.
     * @param channel The channel to send the message to.
     */
    public void sendTicketCreateMessage(TextChannel channel) {
        MessageCreateAction c = channel.sendMessage("""
                Use the selection below to create a ticket!
                """);
        StringSelectMenu.Builder menu = StringSelectMenu.create("create_ticket");
        for (TicketType t : ticketTypes) {
            if (t.showInSel()) menu.addOption(t.selText(), t.name(), Emoji.fromUnicode(t.selEmoji()));
        }
        c.setActionRow(menu.build());
        c.queue();
    }

    /**
     * Creates a ticket for the given creator and type.
     * @param creator The creator of the ticket as a Discord ID.
     * @param type The type of the ticket.
     * @return A {@link CompletableFuture<Ticket>} that completes with the created Ticket.
     */
    public CompletableFuture<Ticket> createTicket(String creator, TicketType type) {
        CompletableFuture<Ticket> future = new CompletableFuture<>();
        DiscordBot.getInstance().createTicketChannel(creator, type).thenAccept(channel -> {
            Ticket ticket = new Ticket(UUID.randomUUID(), creator, "", channel, getTicketStatus("OPEN"), type);
            tickets.add(ticket);
            StorageFactory.getInstance().getUsedStorage().createTicket(ticket);
            future.complete(ticket);
        }).exceptionally(ex -> {
            log.error("Could not create ticket channel!", ex);
            future.completeExceptionally(ex);
            return null;
        });
        return future;
    }

    /**
     * Gets a ticket status by its UUID.
     * @param uuid The UUID of the ticket status.
     * @return The {@link TicketStatus} or null if not found.
     */
    public TicketStatus getTicketStatus(UUID uuid) { //TODO: Improve with Optional
        for (TicketStatus ticketStatus : statuses) if (ticketStatus.uniqueID().equals(uuid)) return ticketStatus;
        return null;
    }

    /**
     * Gets a ticket status by its name.
     * @param name The name of the ticket status.
     * @return The {@link TicketStatus} or null if not found.
     */
    public TicketStatus getTicketStatus(String name) { //TODO: Improve with Optional
        for (TicketStatus ticketStatus : statuses) if (ticketStatus.statusName().equals(name)) return ticketStatus;
        return null;
    }

    /**
     * Gets a ticket type by its UUID.
     * @param uuid The UUID of the ticket type.
     * @return The {@link TicketType} or null if not found.
     */
    public TicketType getTicketType(UUID uuid) { //TODO: Improve with Optional
        for (TicketType ticketType : ticketTypes) if (ticketType.uniqueID().equals(uuid)) return ticketType;
        return null;
    }

    /**
     * Updates the status of the given ticket.
     * @param ticket The ticket to update.
     * @param status The new status.
     */
    public void updateTicketStatus(Ticket ticket, TicketStatus status) {
        ticket.updateStatus(status);
    }
}