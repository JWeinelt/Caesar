package de.julianweinelt.caesar.endpoint.chat;

import de.julianweinelt.caesar.endpoint.ClientOnly;
import de.julianweinelt.caesar.endpoint.ServerOnly;
import lombok.Setter;

import java.lang.reflect.Field;

public enum ChatAction {
        @ClientOnly
        AUTHENTICATE, // When a client authenticates to the server
        @ClientOnly
        CLOSE_REQUEST, // Sent by the client to close connection
        LEAVE, // When a client leaves the chat
        MESSAGE, // When the server sends a message to a client
        SEND_MESSAGE, // When a client sends a message to the server
        MESSAGE_REACTION_ADD, // When a client react to a message
        MESSAGE_REACTION_REMOVE, // When a client removes their reaction or an admin removes it.
        MESSAGE_REACTION_REMOVE_ALL, // When an admin client removed all reactions from a message
        SYSTEM, // When the server sends a system message to a chat
        USER_LIST, // When the client wants to get the user list of a chat
        CREATE_CHAT, // When a client creates a chat
        RENAME_CHAT, // When a client renames a chat
        DELETE_CHAT, // When a client wants to delete a chat
        @ServerOnly
        USER_MENTION, // When a client mentioned a user in a chat
        REQUEST_USER_DATA,
        ADD_USER, // When a client adds a user to a chat
        KICK_USER, // When a client kicks a user from a chat
        MUTE_USER, // When a client mutes a user in a chat
        UNMUTE_USER, // When a client unmutes a user in a chat
        JOIN_WITH_INVITE, // When a client joins a chat with an invitation
        SEND_ERROR, // When the server sends an error message to a client
        @ServerOnly
        HANDSHAKE, // For sending a handshake to the client
        SEND_CHAT_LIST, // For sending all chats of a client
        START_VOICE_CALL,
        END_VOICE_CALL,
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