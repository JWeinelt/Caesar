package de.julianweinelt.caesar.endpoint.chat;

import java.util.List;
import java.util.UUID;

public record ChatMention(UUID mentioned, UUID chat, UUID sender) {

    public static boolean isMentioned(UUID seek, List<ChatMention> mentions) {
        for (ChatMention mention : mentions) {
            if (mention.mentioned.equals(seek)) return true;
        }
        return false;
    }
}