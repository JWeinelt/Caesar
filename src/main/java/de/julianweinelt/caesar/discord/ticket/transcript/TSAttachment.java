package de.julianweinelt.caesar.discord.ticket.transcript;

import net.dv8tion.jda.api.entities.Message;

public record TSAttachment(String url, String fileName, String contentType, String fileExtension) {
    public static TSAttachment of(Message.Attachment attachment) {
        return new TSAttachment(attachment.getUrl(), attachment.getFileName(),
                attachment.getContentType(), attachment.getFileExtension());
    }
}