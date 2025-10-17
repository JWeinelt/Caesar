package de.julianweinelt.caesar.discord;

import de.julianweinelt.caesar.util.wrapping.DiscordEmbedWrapper;
import de.julianweinelt.caesar.util.wrapping.MessageSentWrapper;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.OnlineStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Getter
@Setter
public class DiscordConfiguration {
    public static DiscordConfiguration getInstance() {
        return DiscordBot.getInstance().getConfig();
    }

    private OnlineStatus defaultOnlineStatus = OnlineStatus.ONLINE;
    private String discordBotToken = "secret";

    private boolean autoMod = false;
    private final List<String> blockedWords = new ArrayList<>();
    private boolean blockCaps = false;
    private int blockCapsPercent = 60;
    private int spamRateMinute = 0;
    private boolean blockLinks = false;
    private boolean blockDiscordInvites = false;
    private final List<String> whitelistedLinks = new ArrayList<>();
    private final List<String> autoThreadChannels = new ArrayList<>();

    private final List<StatusMessage> statusMessages = new ArrayList<>();
    private int statusChangeInterval = 2;

    private boolean useTicketSystem = true;
    private final List<String> ticketChannels = new ArrayList<>();
    private String ticketCategory = "1203373647134195733";
    private final List<DiscordEmbedWrapper> embeds = new ArrayList<>();
    private String waitingRoom = "1203373647134195736";
    private final List<MessageSentWrapper> embedMessages = new ArrayList<>();

    private String guild = "1203373645108355072";
    private String infoChannel = "1221472133352525865";

    public Optional<MessageSentWrapper> getMessageByEmbedID(String embedID) {
        return embedMessages.stream().filter(m -> m.embedID().equals(embedID)).findFirst();
    }
    public List<MessageSentWrapper> getMessagesByEmbedID(String embedID) {
        List<MessageSentWrapper> result = new ArrayList<>();
        for (MessageSentWrapper m : embedMessages) {
            if (m.embedID().equals(embedID)) result.add(m);
        }
        return result;
    }

    public DiscordEmbedWrapper getEmbedByID(String embedID) {
        for (DiscordEmbedWrapper e : embeds) {
            if (e.getEmbedID().equals(embedID)) return e;
        }
        return null;
    }
    public void deleteEmbedByID(String embedID) {
        embeds.removeIf(e -> e.getEmbedID().equals(embedID));
        embedMessages.removeIf(m -> m.embedID().equals(embedID));
    }

}