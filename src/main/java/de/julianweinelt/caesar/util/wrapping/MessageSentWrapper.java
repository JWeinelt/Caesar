package de.julianweinelt.caesar.util.wrapping;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.julianweinelt.caesar.discord.DiscordBot;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public record MessageSentWrapper(String embedID, String channelID, String messageID) {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public Optional<Message> toMessage() {
        TextChannel c = DiscordBot.getInstance().getMainGuild().getTextChannelById(channelID);
        if (c == null) return Optional.empty();
        return Optional.ofNullable(c.retrieveMessageById(messageID).complete());
    }

    @Override
    public @NotNull String toString() {
        return GSON.toJson(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        MessageSentWrapper that = (MessageSentWrapper) obj;
        return embedID.equals(that.embedID) && channelID.equals(that.channelID) && messageID.equals(that.messageID);
    }

    @Override
    public int hashCode() {
        return (embedID + channelID + messageID).hashCode();
    }
}