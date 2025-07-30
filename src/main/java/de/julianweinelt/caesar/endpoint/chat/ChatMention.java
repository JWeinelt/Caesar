package de.julianweinelt.caesar.endpoint.chat;

import java.util.UUID;

public record ChatMention(UUID mentioned, UUID chat, UUID sender) {}