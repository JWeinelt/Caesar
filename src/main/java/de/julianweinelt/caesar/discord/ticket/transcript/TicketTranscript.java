package de.julianweinelt.caesar.discord.ticket.transcript;

import lombok.Getter;
import net.dv8tion.jda.api.entities.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
public class TicketTranscript {
    private final UUID ticketID;

    private final List<TSMessage> messages = new ArrayList<>();

    public TicketTranscript(UUID ticketID) {
        this.ticketID = ticketID;
    }

    public void processMessage(Message message) {
        messages.add(TSMessage.of(message));
    }
}
