package de.julianweinelt.caesar.endpoint.chat;

import de.julianweinelt.caesar.endpoint.ClientOnly;
import de.julianweinelt.caesar.endpoint.ServerOnly;

import java.lang.reflect.Field;

public enum ChatAction {
        @ClientOnly
        AUTHENTICATE, // When a client authenticates to the server
        @ClientOnly
        CLOSE_REQUEST, // Sent by the client to close connection
        LEAVE, // When a client leaves the chat
        MESSAGE, // When the server sends a message to a client
        SEND_MESSAGE, // When a client sends a message to the server
        SYSTEM, // When the server sends a system message to a chat
        USER_LIST, // When the client wants to get the user list of a chat
        CREATE_CHAT, // When a client creates a chat
        RENAME_CHAT,
        ADD_USER, // When a client adds a user to a chat
        KICK_USER, // When a client kicks a user from a chat
        MUTE_USER, // When a client mutes a user in a chat
        UNMUTE_USER, // When a client unmutes a user in a chat
        JOIN_WITH_INVITE, // When a client joins a chat with an invitation
        SEND_ERROR, // When the server sends an error message to a client
        @ServerOnly
        HANDSHAKE, // For sending a handshake to the client
        SEND_CHAT_LIST, // For sending all chats of a client
        UNKNOWN; // When the server receives an unknown action

        public static boolean isServerOnly(ChatAction action) {
                try {
                        Field field = ChatAction.class.getField(action.name());
                        return field.isAnnotationPresent(ServerOnly.class);
                } catch (NoSuchFieldException e) {
                        return false;
                }
        }

        public static boolean isClientOnly(ChatAction action) {
                try {
                        Field field = ChatAction.class.getField(action.name());
                        return field.isAnnotationPresent(ClientOnly.class);
                } catch (NoSuchFieldException e) {
                        return false;
                }
        }
}