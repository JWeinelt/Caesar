package de.julianweinelt.caesar.discord.ticket.transcript;

import net.dv8tion.jda.api.entities.Message;

import java.util.List;

public record TSMessage(long timestamp, TSUser author, String content, List<TSAttachment> attachments) {
    public static TSMessage of(Message msg) {
        return new TSMessage(
                System.currentTimeMillis(),
                TSUser.of(msg.getAuthor()),
                msg.getContentDisplay(),
                msg.getAttachments().stream().map(TSAttachment::of).toList()
        );
    }
}