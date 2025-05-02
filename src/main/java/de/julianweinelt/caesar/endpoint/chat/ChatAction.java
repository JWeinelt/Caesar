package de.julianweinelt.caesar.endpoint.chat;

public enum ChatAction {
        AUTHENTICATE, // When a client authenticates to the server
        LEAVE, // When a client leaves the chat
        MESSAGE, // When the server sends a message to a client
        SEND_MESSAGE, // When a client sends a message to the server
        SYSTEM, // When the server sends a system message to a chat
        USER_LIST, // When the client wants to get the user list of a chat
        CREATE_CHAT, // When a client creates a chat
        ADD_USER, // When a client adds a user to a chat
        KICK_USER, // When a client kicks a user from a chat
        MUTE_USER, // When a client mutes a user in a chat
        UNMUTE_USER, // When a client unmutes a user in a chat
        JOIN_WITH_INVITE, // When a client joins a chat with an invitation
        SEND_ERROR, // When the server sends an error message to a client
        UNKNOWN; // When the server receives an unknown action
}