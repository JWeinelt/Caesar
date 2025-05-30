package de.julianweinelt.caesar.discord;

import de.julianweinelt.caesar.util.wrapping.DiscordEmbedWrapper;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class DiscordConfiguration {
    private boolean autoMod = false;
    private final List<String> blockedWords = new ArrayList<>();
    private boolean blockCaps = false;
    private int spamRateMinute = 0;
    private final List<String> autoThreadChannels = new ArrayList<>();

    private final List<StatusMessage> statusMessages = new ArrayList<>();

    private boolean useTicketSystem = false;
    private final List<DiscordEmbedWrapper> embeds = new ArrayList<>();
}