package de.julianweinelt.caesar.endpoint.chat;

import de.julianweinelt.caesar.auth.User;
import lombok.Getter;
import lombok.Setter;
import org.java_websocket.WebSocket;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Represents a chat instance containing users, moderators and messages.
 */
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

    /**
     * Adds a user to the chat.
     * @param uuid The {@link UUID} of the user to add.
     */
    public void addUser(UUID uuid) {
        users.add(uuid);
    }

    /**
     * Removes a user from the chat.
     * @param uuid The {@link UUID} of the user to remove.
     */
    public void removeUser(UUID uuid) {
        users.remove(uuid);
    }

    /**
     * Checks if a user is part of the chat.
     * @param u The {@link User} to check.
     * @return {@code true} if the user is part of the chat, {@code false} otherwise.
     */
    public boolean hasUser(User u) {
        return hasUser(u.getUuid());
    }
    /**
     * Checks if a user is part of the chat.
     * @param uuid The {@link UUID} of the user to check.
     * @return {@code true} if the user is part of the chat, {@code false} otherwise.
     */
    public boolean hasUser(UUID uuid) {
        return users.contains(uuid);
    }

    /**
     * Grants moderator privileges to a user in the chat.
     * The user must already be part of the chat.
     * @param uuid The {@link UUID} of the moderator to add.
     * @throws IllegalArgumentException if the user is not part of the chat.
     */
    public void addModerator(UUID uuid) throws IllegalArgumentException{
        if (!users.contains(uuid)) throw new IllegalArgumentException("User must be part of the chat to be added as moderator.");
        moderators.add(uuid);
    }

    /**
     * Revokes moderator privileges from a user in the chat.
     * @param uuid The {@link UUID} of the moderator to remove.
     */
    public void removeModerator(UUID uuid) {
        moderators.remove(uuid);
    }

    /**
     * Checks if a user is a moderator in the chat.
     * @param uuid The {@link UUID} of the user to check.
     * @return {@code true} if the user is a moderator, {@code false} otherwise.
     */
    public boolean isModerator(UUID uuid) {
        return moderators.contains(uuid);
    }

    /**
     * Registers a new message in the chat.
     * @param message The {@link Message} to register.
     */
    @ApiStatus.Internal
    public void registerNewMessage(Message message) {
        messages.add(message);
    }

    /**
     * Checks if the chat is a group chat.
     * @return {@code true} if the chat is a group chat, {@code false} if it is a direct message.
     */
    public boolean isGroupChat() {
        return !isDirectMessage;
    }

    /**
     * Retrieves a message by its unique ID.
     * @param uuid The {@link UUID} of the message to retrieve.
     * @return The {@link Message} with the specified ID, or {@code null} if not found.
     */
    @Nullable
    //TODO: Change to Optional<Message> in future version
    public Message getMessageByID(UUID uuid) {
        for (Message message : messages) {
            if (message.uniqueID().equals(uuid)) return message;
        }
        return null;
    }

    /**
     * Sends an action to all users in the chat.
     * @param action The action to perform on each user's {@link WebSocket}.
     */
    public void sendToUsers(Consumer<WebSocket> action) {
        for (UUID uuid : users) {
            WebSocket conn = server.getConnection(uuid);
            if (conn != null) action.accept(conn);
        }
    }
}