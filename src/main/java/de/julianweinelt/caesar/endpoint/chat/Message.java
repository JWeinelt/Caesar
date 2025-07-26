package de.julianweinelt.caesar.endpoint.chat;

public record Message(String message, String sender, long date) {}