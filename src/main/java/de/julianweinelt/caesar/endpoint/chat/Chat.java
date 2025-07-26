package de.julianweinelt.caesar.endpoint.chat;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Chat {
    private transient final ChatServer server;

    @Getter
    private final UUID uniqueID;

    @Getter
    @Setter
    private String customName;

    @Getter
    private final List<UUID> users = new ArrayList<>();
    @Getter
    private final List<Message> messages = new ArrayList<>();

    @Getter
    @Setter
    private boolean publicChat = false;

    public Chat(ChatServer server, UUID uniqueID) {
        this.server = server;
        this.uniqueID = uniqueID;
    }

    public Chat(ChatServer server) {
        this.server = server;
        this.uniqueID = UUID.randomUUID();
    }

    public void addUser(UUID uuid) {
        users.add(uuid);
    }
    public void removeUser(UUID uuid) {
        users.remove(uuid);
    }


    public void registerNewMessage(Message message) {
        messages.add(message);
    }
}