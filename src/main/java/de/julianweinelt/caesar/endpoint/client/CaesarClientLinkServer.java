package de.julianweinelt.caesar.endpoint.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.julianweinelt.caesar.Caesar;
import de.julianweinelt.caesar.discord.DiscordBot;
import de.julianweinelt.caesar.endpoint.CaesarServiceProvider;
import de.julianweinelt.caesar.endpoint.ClientOnly;
import de.julianweinelt.caesar.endpoint.chat.ChatAction;
import de.julianweinelt.caesar.storage.Storage;
import de.julianweinelt.caesar.storage.StorageFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.UUID;

@Slf4j
public class CaesarClientLinkServer extends WebSocketServer {

    private final HashMap<UUID, WebSocket> clients = new HashMap<>();

    public CaesarClientLinkServer(int port) {
        super(new InetSocketAddress(port));
    }
    public static CaesarClientLinkServer getInstance() {
        return Caesar.getInstance().getClientLinkServer();
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
            switch (action) {
                case AUTHENTICATE -> {
                    UUID user = UUID.fromString(rootOBJ.get("myID").getAsString());
                    String linkVersion = rootOBJ.get("myVersion").getAsString();
                    clients.put(user, conn);
                    sendHandShake(conn, new ComparableVersion(linkVersion));
                }
                case DC_LINK_REQUEST -> {
                    UUID user = getSocketUser(conn);
                    if (user == null) return;
                    int code = new SecureRandom().nextInt(1000, 9999);
                    sendWantedCode(code, conn);
                    DiscordBot.getInstance().getLinkValidator().put(user, code);
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
        boolean update = clientVersion.compareTo(newestVersion) < 0;
        if (update) log.info("Update for client is available.");
        o.addProperty("updateAvailable", update);
        o.addProperty("type", ClientAction.HANDSHAKE.name());
        o.addProperty("newVersion", CaesarServiceProvider.getInstance().getLatestClientVersion());
        conn.send(o.toString());
    }

    public void sendWaitingRoomUpdate() {
        JsonObject o = new JsonObject();
        o.addProperty("type", ClientAction.DC_WAITING_ROOM_UPDATE.name());
        o.add("data", DiscordBot.getInstance().getWaitingRoom());
        for (WebSocket socket : clients.values()) socket.send(o.toString());
    }

    public void sendWantedCode(int code, WebSocket socket) {
        JsonObject o = new JsonObject();
        o.addProperty("type", ClientAction.DC_LINK_REQUEST_CODE.name());
        o.addProperty("code", code);
        socket.send(o.toString());
    }

    public void sendCodeSuccess(int code) {
        JsonObject o = new JsonObject();
        o.addProperty("type", ClientAction.DC_LINK_SUCCESS.name());
        UUID user = DiscordBot.getInstance().getCodeUser(code);
        if (user == null) return;
        WebSocket webSocket = clients.getOrDefault(user, null);
        if (webSocket == null) return;
        webSocket.send(o.toString());
    }

    public void sendVoiceUpdate(UUID user, String channel) {
        log.info("Sending voice update");
        WebSocket w = clients.getOrDefault(user, null);
        if (w == null) return;
        JsonObject o = new JsonObject();
        o.addProperty("type", ClientAction.DC_VOICE_UPDATE.name());
        o.addProperty("channel", channel);
        o.addProperty("linked", DiscordBot.getInstance().getDCUserByUser(user) != null);
        w.send(o.toString());
    }

    public void sendVoiceUpdate(UUID user) {
        WebSocket w = clients.getOrDefault(user, null);
        if (w == null) return;
        JsonObject o = new JsonObject();
        o.addProperty("type", ClientAction.DC_VOICE_UPDATE.name());
        o.addProperty("linked", DiscordBot.getInstance().getDCUserByUser(user) != null);
        o.addProperty("channel", "");
        w.send(o.toString());
    }

    public ClientAction parseAction(String input) {
        try {
            return ClientAction.valueOf(input);
        } catch (IllegalArgumentException ignored) {
            return ClientAction.UNKNOWN;
        }
    }

    public UUID getSocketUser(WebSocket webSocket) {
        for (UUID uuid : clients.keySet()) {
            if (clients.get(uuid).equals(webSocket)) return uuid;
        }
        return null;
    }
}
