package de.julianweinelt.caesar.discord;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class BotListener extends ListenerAdapter {
    private final DiscordConfiguration config;

    public BotListener(DiscordConfiguration config) {
        this.config = config;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent e) {
        if (!config.isAutoMod()) {
            return;
        }

        for (String blockedWord : config.getBlockedWords()) {
            if (e.getMessage().getContentRaw().toLowerCase().contains(blockedWord.toLowerCase())) {
                e.getMessage().delete().queue();
            }
        }
    }
}