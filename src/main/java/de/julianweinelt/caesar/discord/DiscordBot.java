package de.julianweinelt.caesar.discord;

import de.julianweinelt.caesar.Caesar;
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

    public static DiscordBot getInstance() {
        return Caesar.getInstance().getDiscordBot();
    }

    private JDA jda;

    public void start() {
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