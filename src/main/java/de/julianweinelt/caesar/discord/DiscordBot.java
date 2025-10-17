package de.julianweinelt.caesar.discord;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.julianweinelt.caesar.Caesar;
import de.julianweinelt.caesar.discord.ticket.TicketManager;
import de.julianweinelt.caesar.discord.ticket.TicketType;
import de.julianweinelt.caesar.discord.wrapping.ChannelType;
import de.julianweinelt.caesar.discord.wrapping.ChannelWrapper;
import de.julianweinelt.caesar.endpoint.client.CaesarClientLinkServer;
import de.julianweinelt.caesar.plugin.Registry;
import de.julianweinelt.caesar.plugin.event.Event;
import de.julianweinelt.caesar.plugin.event.Subscribe;
import de.julianweinelt.caesar.storage.LocalStorage;
import de.julianweinelt.caesar.storage.StorageFactory;
import de.julianweinelt.caesar.util.wrapping.DiscordEmbedWrapper;
import lombok.Getter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
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

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class DiscordBot {
    private static final Logger log = LoggerFactory.getLogger(DiscordBot.class);
    private ScheduledExecutorService statusScheduler = Executors.newScheduledThreadPool(1);


    @Getter
    private final HashMap<UUID, String> caesarUserChannels = new HashMap<>();
    @Getter
    private final HashMap<UUID, Integer> linkValidator = new HashMap<>();

    @Getter
    private DiscordConfiguration config = new DiscordConfiguration();
    @Getter
    private BotListener defaultListener;

    @Getter
    private JDA jda;

    @Getter
    private Guild mainGuild;

    public static DiscordBot getInstance() {
        return Caesar.getInstance().getDiscordBot();
    }

    public void validateCode(int code, String dc) {
        UUID user = getCodeUser(code);
        if (user == null) return;
        CaesarClientLinkServer.getInstance().sendCodeSuccess(code);
        StorageFactory.getInstance().getUsedStorage().mapUserDiscord(dc, user);
    }

    public UUID getCodeUser(int code) {
        for (UUID u : linkValidator.keySet()) {
            if (linkValidator.get(u) == code) return u;
        }
        return null;
    }

    public DiscordBot() {
        Registry.getInstance().registerListener(this, Registry.getInstance().getSystemPlugin());
        registerEvents();
    }

    public int getOnlineMembers() {
        int count = 0;
        for (Member m : mainGuild.getMembers()) {
            if (!m.getOnlineStatus().equals(OnlineStatus.OFFLINE)) {
                count++;
            }
        }
        return count;
    }

    public int getBotMembers() {
        int count = 0;
        for (Member m : mainGuild.getMembers()) {
            if (m.getUser().isBot()) count++;
        }
        return count;
    }

    public UUID getUserByID(String id) {
        for (UUID u : caesarUserChannels.keySet()) if (caesarUserChannels.get(u).equals(id)) return u;
        UUID u = StorageFactory.getInstance().getUsedStorage().getUserIDFromDiscordID(id);
        if (u != null) {
            caesarUserChannels.put(u, id);
            return u;
        }
        return null;
    }

    public String getDCUserByUser(UUID user) {
        String e = caesarUserChannels.getOrDefault(user, null);
        if (e == null) {
            String id = StorageFactory.getInstance().getUsedStorage().getDiscordID(user);
            if (id != null) {
                caesarUserChannels.put(user, id);
                return id;
            }
        }
        return e;
    }

    private void registerEvents() {
        Registry.getInstance().registerEvents(
                "DiscordReadyEvent",
                "DiscordStopEvent",
                "DiscordTicketCreateEvent",
                "DiscordTicketEditEvent",
                "DiscordStatusChangeEvent",
                "DiscordConnectionInterruptEvent",
                "DiscordConfigReloadEvent"
        );
    }


    @Subscribe("StorageReadyEvent")
    public void onStorageReady(Event event) {
        log.info("Preparing discord...");
        if (!new File("data", "discord.json").exists()) LocalStorage.getInstance().save(new DiscordConfiguration(), "discord");
        config = LocalStorage.getInstance().load("discord", DiscordConfiguration.class);
        start(false);
    }

    public DiscordEmbedWrapper getEmbedWrapped(String name) {
        for (DiscordEmbedWrapper w : config.getEmbeds()) if (w.getEmbedID().equals(name)) return w;
        return null;
    }

    public JsonArray getWaitingRoom() {
        JsonArray array = new JsonArray();
        for (Member m : mainGuild.getVoiceChannelById(config.getWaitingRoom()).getMembers()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("name", m.getUser().getName());
            obj.addProperty("avatar", m.getUser().getAvatarUrl());
            obj.addProperty("id", m.getUser().getId());
            obj.addProperty("effectiveName",  m.getEffectiveName());
            obj.addProperty("effectiveAvatarUrl", m.getEffectiveAvatarUrl());
            array.add(obj);
        }
        return array;
    }

    public void start(boolean wasRestart) {
        log.info("Starting Discord bot instance...");
        if (config == null) {
            log.warn("Discord configuration not loaded yet.");
            return;
        }

        JDABuilder builder = JDABuilder.create(config.getDiscordBotToken(),
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

        Registry.getInstance().callEvent(new Event("DiscordReadyEvent")
                .set("mainGuild", mainGuild.getId())
                .set("jda", jda)
                .set("config", config)
                .set("was-restart", wasRestart)
        );
    }

    public void stop() {
        try {
            if (!statusScheduler.isShutdown()) {
                log.info("Shutting down status scheduler...");
                if (statusScheduler.awaitTermination(3, TimeUnit.SECONDS)) log.info("Status scheduler has been shut down.");
                else {
                    log.error("Status scheduler failed to stop. Terminating...");
                    statusScheduler.shutdownNow();
                }
            }

            if (jda != null) {
                log.info("Shutting down JDA...");
                boolean success = jda.awaitShutdown(3, TimeUnit.SECONDS);
                if (success) {
                    log.info("JDA has been shut down.");
                    Registry.getInstance().callEvent(new Event("DiscordStopEvent")
                            .set("jda", jda)
                            .set("was-clean", true));
                } else {
                    log.error("JDA could not be shut down. Terminating...");
                    jda.shutdownNow();
                    Registry.getInstance().callEvent(new Event("DiscordStopEvent")
                            .set("jda", jda)
                            .set("was-clean", false));
                }
                defaultListener = null;
            }
        } catch (InterruptedException e) {
            log.error("A fatal error occurred while shutting down status message service.");
        }
    }

    public void restart() {
        restart(false);
    }

    public void restart(boolean reloadConfig) {
        log.info("Restarting Discord Bot...");
        stop();
        if (reloadConfig) {
            log.info("Reloading config");
            config = null;
            Registry.getInstance().callEvent(new Event("DiscordConfigReloadEvent")
                    .set("jda", jda)
                    .set("instance", this));
        }
        log.info("Done! Restarting scheduler...");
        statusScheduler = Executors.newScheduledThreadPool(1);
        start(true);
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
                            .withEmoji(Emoji.fromUnicode("🛜")))
                    .queue();
            channelID.complete(ticketChannel.getId());
        });
        return channelID;
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