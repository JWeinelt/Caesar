package de.julianweinelt.caesar.endpoint.chat;

import java.util.Date;

public record Message(String message, String sender, Date date) {}