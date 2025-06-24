package de.julianweinelt.caesar.endpoint.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.julianweinelt.caesar.endpoint.CaesarServiceProvider;
import de.julianweinelt.caesar.endpoint.ClientOnly;
import de.julianweinelt.caesar.endpoint.chat.ChatAction;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.UUID;

public class CaesarClientLinkServer extends WebSocketServer {

    private final HashMap<UUID, WebSocket> clients = new HashMap<>();

    public CaesarClientLinkServer(int port) {
        super(new InetSocketAddress(port));
    }

    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {

    }

    @Override
    public void onClose(WebSocket webSocket, int i, String s, boolean b) {

    }

    @Override
    public void onMessage(WebSocket webSocket, String s) {
        // Not used, as Godot uses ByteBuffers
    }

    @Override
    public void onMessage(WebSocket conn, ByteBuffer message) {
        if (message.hasArray()) {
            String msg = new String(message.array(), StandardCharsets.UTF_8);
            JsonObject rootOBJ = JsonParser.parseString(msg).getAsJsonObject();
            ClientAction action = parseAction(rootOBJ.get("type").getAsString().toUpperCase());
            UUID user = UUID.fromString(rootOBJ.get("myID").getAsString());
            switch (action) {
                case AUTHENTICATE -> {
                    String linkVersion = rootOBJ.get("myVersion").getAsString();
                    clients.put(user, conn);
                    sendHandShake(conn, new ComparableVersion(linkVersion));
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

    public void sendHandShake(WebSocket conn, ComparableVersion clientVersion) {
        JsonObject o = new JsonObject();
        ComparableVersion newestVersion = new ComparableVersion(CaesarServiceProvider.getInstance().getLatestClientVersion());
        o.addProperty("updateAvailable", clientVersion.compareTo(newestVersion) < 0);
        o.addProperty("type", ClientAction.HANDSHAKE.name());
        conn.send(o.toString());
    }

    public ClientAction parseAction(String input) {
        try {
            return ClientAction.valueOf(input);
        } catch (IllegalArgumentException ignored) {
            return ClientAction.UNKNOWN;
        }
    }
}
