package de.julianweinelt.caesar.endpoint.chat;

import java.util.List;
import java.util.UUID;

public record ChatMention(UUID mentioned, UUID chat, UUID sender) {

    /**
     * Checks whether a particular UUID is mentioned in a list of ChatMentions.
     * @param seek The {@link UUID} to search for.
     * @param mentions The {@link List} of {@link ChatMention}s to search within.
     * @return {@code true} if the {@link UUID} is mentioned, {@code false} otherwise.
     */
    public static boolean isMentioned(UUID seek, List<ChatMention> mentions) {
        for (ChatMention mention : mentions) {
            if (mention.mentioned.equals(seek)) return true;
        }
        return false;
    }
}