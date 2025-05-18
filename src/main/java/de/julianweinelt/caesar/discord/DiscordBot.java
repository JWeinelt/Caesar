package de.julianweinelt.caesar.discord;

import de.julianweinelt.caesar.Caesar;
import de.julianweinelt.caesar.plugin.event.Event;
import de.julianweinelt.caesar.plugin.event.EventListener;
import de.julianweinelt.caesar.plugin.event.Subscribe;
import de.julianweinelt.caesar.storage.LocalStorage;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class DiscordBot {
    private static final Logger log = LoggerFactory.getLogger(DiscordBot.class);

    private DiscordConfiguration config = null;

    public static DiscordBot getInstance() {
        return Caesar.getInstance().getDiscordBot();
    }

    private JDA jda;

    @Subscribe("StorageReadyEvent")
    public void onStorageReady(Event event) {
        config = LocalStorage.getInstance().load("discord.json", DiscordConfiguration.class);
        start();
    }

    public void start() {
        if (config == null) {
            log.warn("Discord configuration not loaded yet.");
            return;
        }
        JDABuilder builder = JDABuilder.create(LocalStorage.getInstance().getData().getDiscordBotToken(),
                Arrays.asList(GatewayIntent.values()));
        jda = builder.build();
    }

    public void stop() {
        if (jda != null) {
            jda.shutdown();
        }
    }

    public void restart() {
        stop();
        start();
    }
}