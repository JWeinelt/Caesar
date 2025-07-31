package de.julianweinelt.caesar.endpoint.chat;

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

    public String message() {
        return message;
    }
    public String sender() {
        return sender;
    }
    public long date() {
        return date;
    }
    public UUID uniqueID() {
        return uniqueID;
    }
    public boolean edited() {
        return edited;
    }
    public long editDate() {
        return editDate;
    }

    public void edit(String newMessage) {
        message = newMessage;
        edited = true;
        editDate = System.currentTimeMillis();
    }


    public void addReaction(String emoji, UUID user) {
        reactions.computeIfAbsent(emoji, e -> new HashSet<>()).add(user);
    }

    public void removeReaction(String emoji, UUID user) {
        Set<UUID> users = reactions.get(emoji);
        if (users != null) {
            users.remove(user);
            if (users.isEmpty()) reactions.remove(emoji);
        }
    }

    public int getReactionCount(String emoji) {
        return reactions.getOrDefault(emoji, Set.of()).size();
    }
}