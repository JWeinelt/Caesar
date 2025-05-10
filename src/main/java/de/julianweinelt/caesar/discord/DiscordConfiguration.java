package de.julianweinelt.caesar.discord;

import java.util.ArrayList;
import java.util.List;

public class DiscordConfiguration {
    private boolean autoMod;
    private final List<String> blockedWords = new ArrayList<>();
    private boolean blockCaps;
    private boolean spamRateMinute;
    private final List<String> autoThreadChannels = new ArrayList<>();
}
