package de.julianweinelt.caesar.auth;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.julianweinelt.caesar.Caesar;
import de.julianweinelt.caesar.endpoint.CaesarServer;
import de.julianweinelt.caesar.storage.LocalStorage;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.HashMap;

public class CaesarLinkServer extends WebSocketServer {
    private static final Logger log = LoggerFactory.getLogger(CaesarLinkServer.class);

    private final HashMap<String, WebSocket> connections = new HashMap<>();

    public CaesarLinkServer() {
        super(new InetSocketAddress(LocalStorage.getInstance().getData().getConnectionServerPort()));
    }


    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
        log.debug("Received new connection from {}",
                webSocket.getRemoteSocketAddress().getAddress().getHostAddress() +
                ":" + webSocket.getRemoteSocketAddress().getPort());
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
        log.info("Started CaesarLinkServer on port {}", LocalStorage.getInstance().getData().getConnectionServerPort());
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
