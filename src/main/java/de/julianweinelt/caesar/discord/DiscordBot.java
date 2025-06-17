package de.julianweinelt.caesar.discord;

import de.julianweinelt.caesar.Caesar;
import de.julianweinelt.caesar.discord.ticket.TicketManager;
import de.julianweinelt.caesar.discord.ticket.TicketType;
import de.julianweinelt.caesar.discord.wrapping.ChannelType;
import de.julianweinelt.caesar.discord.wrapping.ChannelWrapper;
import de.julianweinelt.caesar.plugin.event.Event;
import de.julianweinelt.caesar.plugin.event.Subscribe;
import de.julianweinelt.caesar.storage.LocalStorage;
import de.julianweinelt.caesar.util.wrapping.DiscordEmbedWrapper;
import lombok.Getter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.*;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class DiscordBot {
    private static final Logger log = LoggerFactory.getLogger(DiscordBot.class);
    private ScheduledExecutorService statusScheduler = Executors.newScheduledThreadPool(1);

    @Getter
    private DiscordConfiguration config = null;
    @Getter
    private BotListener defaultListener;

    private JDA jda;

    @Getter
    private Guild mainGuild;

    public static DiscordBot getInstance() {
        return Caesar.getInstance().getDiscordBot();
    }


    @Subscribe("StorageReadyEvent")
    public void onStorageReady(Event event) {
        config = LocalStorage.getInstance().load("discord.json", DiscordConfiguration.class);
        start();
    }

    public DiscordEmbedWrapper getEmbedWrapped(String name) {
        for (DiscordEmbedWrapper w : config.getEmbeds()) if (w.getEmbedID().equals(name)) return w;
        return null;
    }

    public void start() {
        log.info("Starting Discord bot instance...");
        if (config == null) {
            log.warn("Discord configuration not loaded yet.");
            return;
        }

        JDABuilder builder = JDABuilder.create(LocalStorage.getInstance().getData().getDiscordBotToken(),
                Arrays.asList(GatewayIntent.values()));
        jda = builder.build();
        try {
            jda.awaitReady();
        } catch (InterruptedException ignored) {
            log.error("Error while starting up Discord instance.");
            return;
        }

        mainGuild = jda.getGuildById(config.getGuild());
        defaultListener = new BotListener(config, this, jda);
        jda.addEventListener(defaultListener);

        startStatusScheduler();

        log.info("Discord bot instance started.");
    }

    public void stop() {
        try {
            if (!statusScheduler.isShutdown()) {
                if (statusScheduler.awaitTermination(10, TimeUnit.SECONDS)) log.info("Status scheduler has been shut down.");
                else {
                    log.error("Status scheduler failed to stop. Terminating...");
                    statusScheduler.shutdownNow();
                }
            }

            if (jda != null) {
                log.info("Shutting down JDA...");
                boolean success = jda.awaitShutdown(10, TimeUnit.SECONDS);
                if (success) {
                    log.info("JDA has been shut down.");
                } else {
                    log.error("JDA could not be shut down. Terminating...");
                    jda.shutdownNow();
                }
                defaultListener = null;
            }
        } catch (InterruptedException e) {
            log.error("A fatal error occurred while shutting down status message service.");
        }
    }

    public void restart() {
        log.info("Restarting Discord Bot...");
        stop();
        log.info("Done! Restarting scheduler...");
        statusScheduler = Executors.newScheduledThreadPool(1);
        start();
    }

    public void disableDefaultListener() {
        jda.removeEventListener(defaultListener);
    }

    public void enableDefaultListener() {
        jda.addEventListener(defaultListener);
    }

    public void registerListener(ListenerAdapter adapter) {
        log.info("Registering listener for discord bot instance...");
        log.info("Adapter: {}", adapter);
        jda.addEventListener(adapter);
    }

    public List<ChannelWrapper> getChannels() {
        List<ChannelWrapper> channels = new ArrayList<>();
        for (GuildChannel c : mainGuild.getChannels()) {
            if (c instanceof TextChannel) {
                channels.add(new ChannelWrapper(c.getId(), c.getName(), ChannelType.TEXT));
            }
            if (c instanceof NewsChannel) {
                channels.add(new ChannelWrapper(c.getId(), c.getName(), ChannelType.NEWS));
            }
            if (c instanceof VoiceChannel) {
                channels.add(new ChannelWrapper(c.getId(), c.getName(), ChannelType.VOICE));
            }
            if (c instanceof StageChannel) {
                channels.add(new ChannelWrapper(c.getId(), c.getName(), ChannelType.STAGE));
            }
            if (c instanceof ForumChannel) {
                channels.add(new ChannelWrapper(c.getId(), c.getName(), ChannelType.FORUM));
            }
            if (c instanceof Category) {
                channels.add(new ChannelWrapper(c.getId(), c.getName(), ChannelType.CATEGORY));
            }
        }
        return channels;
    }

    public CompletableFuture<String> createTicketChannel(String creator, TicketType type) {
        Member cMember = mainGuild.getMemberById(creator);
        if (cMember == null) return CompletableFuture.failedFuture(new IllegalStateException("Member not found."));
        CompletableFuture<String> channelID = new CompletableFuture<>();
        String channelName = type.prefix() + "-" + creator;
        mainGuild.getCategoryById(config.getTicketCategory()).createTextChannel(channelName)
                .addPermissionOverride(mainGuild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                .addPermissionOverride(cMember, EnumSet.of(Permission.VIEW_CHANNEL,
                        Permission.MESSAGE_SEND), null)
                .queue(ticketChannel -> {
            DiscordEmbedWrapper embed = getEmbedWrapped("ticket-system." + type.name().toLowerCase());
            ticketChannel.sendMessageEmbeds(embed.toEmbed().build())
                    .addActionRow(Button.primary("send-to-panel-ticket", "Send to panel")
                            .withEmoji(Emoji.fromUnicode("📶")))
                    .queue();
            channelID.complete(ticketChannel.getId());
        });
        return channelID;
    }

    public boolean sendTicketMenuMessage(String id) {
        TicketManager tM = TicketManager.getInstance();
        if (tM == null) return  false;
        TextChannel tc = mainGuild.getTextChannelById(id);
        if (tc == null) return false;
        MessageCreateAction action = tc.sendMessageEmbeds(getEmbedWrapped("ticket-creation").toEmbed().build());
        StringSelectMenu.Builder sm = StringSelectMenu.create("ticket-creation");
        for (TicketType t : tM.getTicketTypes()) {
            if (!t.showInSel()) continue;
            sm.addOption(t.selText(), "type-" + t.name().toLowerCase(), Emoji.fromUnicode(t.selEmoji()));
        }
        action.addActionRow(sm.build());
        action.queue();
        return true;
    }

    private void startStatusScheduler() {
        AtomicInteger currentMessage = new AtomicInteger();
        statusScheduler.scheduleAtFixedRate(() -> {
            StatusMessage message = config.getStatusMessages().get(currentMessage.get());
            jda.getPresence().setPresence(message.status(), Activity.of(message.type(), message.message()));
            currentMessage.getAndIncrement();
            if (currentMessage.get() == config.getStatusMessages().size()) currentMessage.set(0);
        }, 0, config.getStatusChangeInterval(), TimeUnit.MINUTES);
    }
}