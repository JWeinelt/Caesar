package de.julianweinelt.caesar.endpoint.chat;

import org.jetbrains.annotations.ApiStatus;

import java.util.*;

public class Message {
    private final UUID uniqueID = UUID.randomUUID();
    private String message;
    private final String sender;
    private final long date;
    private boolean edited = false;
    private long editDate;

    private final Map<String, Set<UUID>> reactions = new HashMap<>();

    public Message(String message, String sender, long date) {
        this.sender = sender;
        this.date = date;
        this.message = message;
    }

    /**
     * Get the message content.
     * @return The message content.
     */
    public String message() {
        return message;
    }
    /**
     * Get the sender of the message.
     * @return The sender of the message.
     */
    public String sender() {
        return sender;
    }
    /**
     * Get the date the message was sent.
     * @return The date the message was sent as a {@link Long}.
     */
    public long date() {
        return date;
    }
    /**
     * Get the unique ID of the message.
     * @return The unique ID of the message as a {@link UUID}.
     */
    public UUID uniqueID() {
        return uniqueID;
    }
    /**
     * Check if the message has been edited.
     * @return True if the message has been edited, false otherwise.
     */
    public boolean edited() {
        return edited;
    }
    /**
     * Get the date the message was last edited.
     * @return The date the message was last edited as a {@link Long}.
     */
    public long editDate() {
        return editDate;
    }

    /**
     * Edit the message content.
     * @param newMessage The new message content.
     */
    @ApiStatus.Experimental
    public void edit(String newMessage) {
        message = newMessage;
        edited = true;
        editDate = System.currentTimeMillis();
    }


    /**
     * Add a reaction to the message.
     * @param emoji The emoji representing the reaction.
     * @param user The {@link UUID} of the user adding the reaction.
     */
    @ApiStatus.Experimental
    public void addReaction(String emoji, UUID user) {
        reactions.computeIfAbsent(emoji, e -> new HashSet<>()).add(user);
    }

    /**
     * Remove a reaction from the message.
     * @param emoji The emoji representing the reaction.
     * @param user The {@link UUID} of the user removing the reaction.
     */
    @ApiStatus.Experimental
    public void removeReaction(String emoji, UUID user) {
        Set<UUID> users = reactions.get(emoji);
        if (users != null) {
            users.remove(user);
            if (users.isEmpty()) reactions.remove(emoji);
        }
    }

    /**
     * Get the count of reactions for a specific emoji.
     * @param emoji The emoji representing the reaction.
     * @return The count of reactions for the specified emoji.
     */
    @ApiStatus.Experimental
    public int getReactionCount(String emoji) {
        return reactions.getOrDefault(emoji, Set.of()).size();
    }
}