package de.julianweinelt.caesar.auth;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.julianweinelt.caesar.Caesar;
import de.julianweinelt.caesar.storage.Configuration;
import de.julianweinelt.caesar.storage.LocalStorage;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.*;

public class CaesarLinkServer extends WebSocketServer {
    private static final Logger log = LoggerFactory.getLogger(CaesarLinkServer.class);

    private final HashMap<String, WebSocket> connections = new HashMap<>();

    public CaesarLinkServer() {
        super(new InetSocketAddress(LocalStorage.getInstance().getData().getConnectionServerPort()));
    }


    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
        log.info("Received new connection from {}",
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
        log.info("Received action {} from {}", action, webSocket.getRemoteSocketAddress().getAddress().getHostAddress());
        switch (action) {
            case HANDSHAKE -> {
                String name = root.get("serverName").getAsString();
                connections.put(name, webSocket);
                JsonObject o = new JsonObject();
                o.addProperty("action", Action.HANDSHAKE.name());
                o.addProperty("serverVersion", Caesar.systemVersion);
                webSocket.send(o.toString());

                log.info("Handshake complete for {}", name);
                JsonObject config = new JsonObject();
                config.addProperty("discordEnabled", Configuration.getInstance().isUseDiscord());
                config.addProperty("action", Action.TRANSFER_CONFIG.name());
                webSocket.send(config.toString());
            }
            case PLAYER_BANNED -> {
                UUID banned = UUID.fromString(root.get("banned").getAsString());
                String reason = root.get("reason").getAsString();
                UUID bannedBy = UUID.fromString(root.get("punisher").getAsString());
                Date activeUntil = Date.from(Instant.now());
                List<String> effectiveServers = new ArrayList<>();
                if (root.has("effectiveServers")) {
                    effectiveServers.addAll(Arrays.stream(root.get("effectiveOn").getAsString().split(",")).toList());
                }
                if (effectiveServers.isEmpty()) effectiveServers.add("*");
                if (root.get("temp").getAsBoolean()) {
                    activeUntil = Date.from(Instant.parse(root.get("activeUntil").getAsString()));
                }
            }
        }
    }

    public enum Action {
        HANDSHAKE,
        DISCONNECT,
        TRANSFER_CONFIG,
        REPORT_CREATED,
        REPORT_EDITED,
        PLAYER_BANNED,
        PLAYER_UNBANNED,
        PLAYER_KICKED,
        PLAYER_MUTED,
        PLAYER_UNMUTED,
        PLAYER_WARNED,
    }
}
