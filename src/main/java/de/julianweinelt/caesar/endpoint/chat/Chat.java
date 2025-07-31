package de.julianweinelt.caesar.endpoint.chat;

import de.julianweinelt.caesar.auth.User;
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
    private final List<UUID> moderators = new ArrayList<>();
    @Getter
    private final List<Message> messages = new ArrayList<>();

    @Getter
    @Setter
    private boolean voiceChat = false;

    @Getter
    @Setter
    private boolean publicChat = false;
    @Getter @Setter
    private boolean isDirectMessage = false;

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
    public boolean hasUser(User u) {
        return hasUser(u.getUuid());
    }
    public boolean hasUser(UUID uuid) {
        return users.contains(uuid);
    }
    public void addModerator(UUID uuid) {
        moderators.add(uuid);
    }
    public void removeModerator(UUID uuid) {
        moderators.remove(uuid);
    }
    public boolean isModerator(UUID uuid) {
        return moderators.contains(uuid);
    }

    public void registerNewMessage(Message message) {
        messages.add(message);
    }

    public boolean isGroupChat() {
        return !isDirectMessage;
    }
}