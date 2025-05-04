package de.julianweinelt.caesar.auth;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.julianweinelt.caesar.Caesar;
import de.julianweinelt.caesar.storage.LocalStorage;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.HashMap;

public class CaesarLinkServer extends WebSocketServer {
    private final HashMap<String, WebSocket> connections = new HashMap<>();

    public CaesarLinkServer() {
        super(new InetSocketAddress(LocalStorage.getInstance().getData().getConnectionServerPort()));
    }


    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {

    }

    @Override
    public void onClose(WebSocket webSocket, int i, String s, boolean b) {

    }

    @Override
    public void onMessage(WebSocket webSocket, String s) {
        handleAction(s, webSocket);
    }

    @Override
    public void onError(WebSocket webSocket, Exception e) {

    }

    @Override
    public void onStart() {

    }

    public void handleAction(String json, WebSocket webSocket) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        Action action = Action.valueOf(root.get("action").getAsString());
        switch (action) {
            case HANDSHAKE -> {
                String name = root.get("serverName").getAsString();
                connections.put(name, webSocket);
                JsonObject o = new JsonObject();
                o.addProperty("action", Action.HANDSHAKE.name());
                o.addProperty("serverVersion", Caesar.systemVersion);
                webSocket.send(o.toString());
            }
        }
    }

    public enum Action {
        HANDSHAKE,
        DISCONNECT,
        TRANSFER_CONFIG
    }
}
