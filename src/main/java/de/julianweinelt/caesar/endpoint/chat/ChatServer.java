package de.julianweinelt.caesar.endpoint.chat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.julianweinelt.caesar.auth.User;
import de.julianweinelt.caesar.auth.UserManager;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

public class ChatServer extends WebSocketServer {
    private final HashMap<UUID, WebSocket> connections = new HashMap<>();
    private final ChatManager chatManager;


    public ChatServer(ChatManager chatManager) {
        this.chatManager = chatManager;
    }

    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {

    }

    @Override
    public void onClose(WebSocket webSocket, int i, String s, boolean b) {

    }

    @Override
    public void onMessage(WebSocket webSocket, String s) {
        // Unused, as Godot works with ByteBuffer
    }

    @Override
    public void onMessage(WebSocket conn, ByteBuffer message) {
        if (message.hasArray()) {
            String msg = new String(message.array(), StandardCharsets.UTF_8);
            JsonObject rootOBJ = JsonParser.parseString(msg).getAsJsonObject();
            ChatAction action = parseAction(rootOBJ.get("type").getAsString());
            if (action.equals(ChatAction.UNKNOWN)) {
                sendError("Unknown action", conn);
                return;
            }
            switch (action) {
                case SEND_MESSAGE -> {
                    sendMessageBy(
                            UUID.fromString(rootOBJ.get("sender")
                                .getAsJsonObject().get("uuid").getAsString()),
                            rootOBJ.get("message").getAsString(),
                        UUID.fromString(rootOBJ.get("chat").getAsString()));
                }
                case LEAVE -> {
                    UUID user = UUID.fromString(rootOBJ.get("user").getAsString());
                    UUID chat = UUID.fromString(rootOBJ.get("chat").getAsString());
                }
            }
        }
    }

    @Override
    public void onError(WebSocket webSocket, Exception e) {

    }

    @Override
    public void onStart() {

    }

    public void sendMessageBy(UUID sender, String message, UUID chat) {
        JsonObject o = new JsonObject();
        o.addProperty("type", ChatAction.MESSAGE.name());
        o.add("sender", createSenderOBJ(sender));
        o.addProperty("message", message);
        o.addProperty("chat", chat.toString());
        o.addProperty("timestamp", System.currentTimeMillis());
        Chat c = chatManager.getChat(chat);
        for (UUID uuid : c.getUsers()) {
            if (uuid.equals(sender)) continue;
            WebSocket conn = getConnection(uuid);
            if (conn != null) {
                conn.send(o.toString());
            }
        }
    }

    public void sendMessageSystem(String message, UUID chat) {
        JsonObject o = new JsonObject();
        o.addProperty("type", ChatAction.SYSTEM.name());
        o.addProperty("message", message);
        o.addProperty("chat", chat.toString());
        o.addProperty("timestamp", System.currentTimeMillis());
        Chat c = chatManager.getChat(chat);
        for (UUID uuid : c.getUsers()) {
            WebSocket conn = getConnection(uuid);
            if (conn != null) {
                conn.send(o.toString());
            }
        }
        c.registerNewMessage(new Message(message, "SYSTEM", Date.from(Instant.now())));
    }

    public JsonObject createSenderOBJ(UUID sender) {
        JsonObject o = new JsonObject();
        o.addProperty("uuid", sender.toString());
        User user = UserManager.getInstance().getUser(sender);
        if (user == null) {
            o.addProperty("username", "Unknown");
        } else {
            o.addProperty("username", user.getUsername());
        }
        return o;
    }

    public WebSocket getConnection(UUID uuid) {
        return connections.getOrDefault(uuid, null);
    }

    public ChatAction parseAction(String input) {
        try {
            return ChatAction.valueOf(input);
        } catch (IllegalArgumentException ignored) {
            return ChatAction.UNKNOWN;
        }
    }

    public void sendError(String message, WebSocket conn) {
        JsonObject o = new JsonObject();
        o.addProperty("message", message);
        o.addProperty("type", ChatAction.SEND_ERROR.name());
        conn.send(o.toString());
    }
}