package de.julianweinelt.caesar.discord;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ShutdownEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;

public class BotListener extends ListenerAdapter {
    private final DiscordConfiguration config;
    private TextChannel infoChannel;
    private final DiscordBot discordBot;
    private final JDA jda;

    public BotListener(DiscordConfiguration config, DiscordBot discordBot, JDA jda) {
        this.config = config;
        this.discordBot = discordBot;
        this.jda = jda;
        infoChannel = jda.getTextChannelById(config.getInfoChannel());
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent e) {
        if (config.getAutoThreadChannels().contains(e.getChannel().getId())) {
            e.getMessage().createThreadChannel(e.getAuthor().getEffectiveName()).queue();
        }
        
        if (!config.isAutoMod()) return;
        if (e.getAuthor().isBot()) return;

        
        if (config.isBlockCaps()) {
            int characters = 0;
            int upperCase = 0;
            for (char c : e.getMessage().getContentRaw().toCharArray()) {
                if (Character.isAlphabetic(c)) characters++;
                if (Character.isUpperCase(c)) upperCase++;
            }

            float percentUpper = upperCase * 1.0F / characters;
            if (percentUpper * 100F > config.getBlockCapsPercent()) e.getMessage().delete().queue();
        }

        for (String blockedWord : config.getBlockedWords()) {
            if (e.getMessage().getContentRaw().toLowerCase().contains(blockedWord.toLowerCase())) {
                e.getMessage().delete().queue();
            }
        }
    }

    @Override
    public void onGuildJoin(GuildJoinEvent e) {
        if (e.getGuild().getFeatures().contains("COMMUNITY"))
            e.getGuild().getCommunityUpdatesChannel().sendMessage("""
                    # Welcome to Caesar!
                    
                    Thanks for using Caesar with Discord integration!
                    I'll assist you in anything you need here.
                    The following features are currently supported:
                    
                    - ✔ Spam detection
                    - ✔ Caps lock detection
                    - ✔ Link prevention
                    - ✔ Auto-thread channels
                    - ✔ Custom status messages
                    - ✔ Tickets
                    - ✔ Creation of embeds
                    
                    To continue with the setup, please go into your panel and open the Discord settings.
                    """)
                    .addActionRow(Button.primary("pno-discord-settings", "Open in panel")
                            .withEmoji(Emoji.fromUnicode("📶")))
                    .queue();
    }
}