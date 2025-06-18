package de.julianweinelt.caesar.endpoint.chat;

import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ChatManager {
    private final List<Chat> chats = new ArrayList<>();

    @Setter
    private ChatServer server;

    public Chat getChat(UUID uuid) {
        for (Chat c : chats) if (c.getUniqueID().equals(uuid)) return c;
        return null;
    }

    public List<Chat> getChatsUser(UUID uuid) {
        List<Chat> result = new ArrayList<>();
        for (Chat c : chats) if (c.getUsers().contains(uuid)) result.add(c);
        return result;
    }
}