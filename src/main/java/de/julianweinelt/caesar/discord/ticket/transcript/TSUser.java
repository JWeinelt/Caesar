package de.julianweinelt.caesar.discord.ticket.transcript;

import net.dv8tion.jda.api.entities.User;

public record TSUser(String name, String id, String avatarURL, boolean isBot, String roleColor) {
    public static TSUser of(User user) {
        String avatarUrl = user.getAvatarUrl();
        if (avatarUrl == null) {
            avatarUrl = user.getDefaultAvatarUrl();
        }
        return new TSUser(user.getName(), user.getId(), avatarUrl, user.isBot(), null);
    }
}