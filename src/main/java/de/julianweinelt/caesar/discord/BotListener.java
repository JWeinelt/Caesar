package de.julianweinelt.caesar.discord;

import com.google.gson.JsonObject;
import de.julianweinelt.caesar.ai.DiscordMessageContextChecker;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ShutdownEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

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
        if (e.getMessage().getMentions().isMentioned(jda.getSelfUser()) && !e.getAuthor().isBot()) {
            e.getChannel().sendTyping().queue();
            JsonObject data = DiscordMessageContextChecker.getMessageType(e.getMessage().getContentRaw());
            Member mentionedUser = null;

            switch (data.get("type").getAsString().toLowerCase()) {
                case "help" -> {
                    e.getMessage().reply("""
                            Hey, %s!
                            I will give you a short tutorial on how you should use me.
                            
                            ## Commands
                            I'm not working directly with commands. Just ping me and write e.g. "help me!".
                            I'll interpret this like a ``help`` command and will send this message.
                            Or, if you want to kick a player here on Discord, just say it!
                            """).queue();
                } case "timeout" -> {
                    if (e.getMessage().getMentions().getMentions(Message.MentionType.USER).size() != 2) {
                        e.getMessage().reply("Please also @Ping the user you want to timeout!").queue();
                        return;
                    }
                    try {
                        mentionedUser = e.getMessage().getMentions().getMembers().get(1);
                    } catch (IndexOutOfBoundsException ex) {
                        e.getMessage().reply("Please also @Ping the user you want to timeout!").queue();
                    }
                    if (mentionedUser == null) return;

                    long time = data.get("time").getAsLong();

                    String reason = (data.get("reason") == null) ? "" : data.get("reason").getAsString();
                    if (!(time == -1))
                        e.getGuild().timeoutFor(mentionedUser, time, TimeUnit.SECONDS).reason(reason).queue();
                    else e.getMessage().reply("Please also say how long the user should be muted.").queue();
                } case "kick" -> {
                    if (e.getMessage().getMentions().getMentions(Message.MentionType.USER).size() != 2) {
                        e.getMessage().reply("Please also @Ping the user you want to kick!").queue();
                        return;
                    }
                    try {
                        mentionedUser = e.getMessage().getMentions().getMembers().get(1);
                    } catch (IndexOutOfBoundsException ex) {
                        e.getMessage().reply("Please also @Ping the user you want to kick!").queue();
                    }
                    if (mentionedUser == null) return;
                    if (data.get("reason") == null)
                        e.getGuild().kick(mentionedUser).queue();
                    else e.getGuild().kick(mentionedUser).reason(data.get("reason").getAsString()).queue();
                }
            }
        }

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
        if (!e.getJDA().getGuilds().isEmpty()) {
            if (e.getGuild().getFeatures().contains("COMMUNITY"))
                e.getGuild().getCommunityUpdatesChannel().sendMessage("""
                # Important
                You are trying to install Caesar on your Discord server.
                *By now, Caesar is **not meant to be run with multiple Discord guilds.***
                Your bot will leave this server in 1 minute, except you click the button below.
                """)
                        .addActionRow(Button.danger("accept-multiple-guilds", "I know what I'm doing"))
                        .queue();
        }
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