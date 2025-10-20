package de.julianweinelt.caesar.auth;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import de.julianweinelt.caesar.Caesar;
import de.julianweinelt.caesar.integration.ServerConnection;
import de.julianweinelt.caesar.storage.APIKeySaver;
import de.julianweinelt.caesar.storage.Configuration;
import de.julianweinelt.caesar.storage.LocalStorage;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;

public class CaesarLinkServer extends WebSocketServer {
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;
    private static final Logger log = LoggerFactory.getLogger(CaesarLinkServer.class);

    private boolean encrypted = false;

    private final HashMap<String, WebSocket> connections = new HashMap<>();
    private final HashMap<WebSocket, UUID> serverIDs = new HashMap<>();


    public CaesarLinkServer(boolean encrypted) {
        super(new InetSocketAddress(LocalStorage.getInstance().getData().getConnectionServerPort()));
        this.encrypted = encrypted;
    }

    public static CaesarLinkServer getInstance() {
        return Caesar.getInstance().getConnectionServer();
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
        log.info("Listening on {}:{}.", getAddress().getHostName(), getPort());
    }

    public void handleAction(String json, WebSocket webSocket) {
        String data;
        log.info(json);
        UUID serverID = serverIDs.getOrDefault(webSocket, null);
        ServerConnection connection = (serverID != null) ? LocalStorage.getInstance().getConnection(serverID) : null;
        if (connection == null) log.warn("Connection is null");
        if (encrypted && isEncrypted(json) && connection != null) {
            log.info("Decrypting message...");
            log.info("Received: {}", json);
            data = decrypt(json, APIKeySaver.getInstance().loadKey(connection.getName()));
            log.info("Decrypted: {}", data);
        } else if (!encrypted && isEncrypted(json)) {
            log.warn("Received encrypted message while server is in non-encrypted mode. Ignoring message.");
            return;
        } else {
            data = json;
        }
        if (isEncrypted(data)) {
            log.warn("The received data seems to be invalid.");
            return;
        }
        JsonObject root = JsonParser.parseString(data).getAsJsonObject();
        Action action = Action.valueOf(root.get("action").getAsString());
        log.info("Received action {} from {}", action, webSocket.getRemoteSocketAddress().getAddress().getHostAddress());
        byte[] key = (connection != null) ? APIKeySaver.getInstance().loadKey(connection.getName()) : "key".getBytes();
        switch (action) {
            case HANDSHAKE -> {
                String name = root.get("serverName").getAsString();
                serverIDs.put(webSocket, UUID.fromString(root.get("serverId").getAsString()));
                connections.put(name, webSocket);
                JsonObject o = new JsonObject();
                o.addProperty("action", Action.HANDSHAKE.name());
                o.addProperty("serverVersion", Caesar.systemVersion);
                o.addProperty("useEncryptedConnection", encrypted);
                webSocket.send(o.toString());

                log.info("Handshake complete for {}", name);
                webSocket.send(createConfig().toString());
            }
            case PLAYER_BANNED -> {
                UUID banned = UUID.fromString(root.get("banned").getAsString());
                String reason = root.get("reason").getAsString();
                UUID bannedBy = UUID.fromString(root.get("punisher").getAsString());
                String bannedByName = root.get("bannedByName").getAsString();
                Date activeUntil = Date.from(Instant.now());
                List<String> effectiveServers = new ArrayList<>();
                List<UUID> effectiveServerIds = new ArrayList<>();
                if (root.has("effectiveServers")) {
                    effectiveServers.addAll(Arrays.stream(root.get("effectiveOn").getAsString().split(",")).toList());
                    for (String s : effectiveServers) {
                        ServerConnection c = LocalStorage.getInstance().getConnection(s);
                        if (c != null) effectiveServerIds.add(c.getUuid());
                    }
                }
                if (effectiveServers.isEmpty()) effectiveServers.add("*");
                if (root.get("temp").getAsBoolean()) {
                    activeUntil = Date.from(Instant.parse(root.get("activeUntil").getAsString()));
                }

                JsonObject a = new JsonObject();
                a.addProperty("action", Action.PLAYER_BANNED.name());
                a.addProperty("banned", banned.toString());
                a.addProperty("reason", reason);
                a.addProperty("bannedBy", bannedBy.toString());
                a.addProperty("bannedByName", bannedByName);
                a.addProperty("temp", root.get("temp").getAsBoolean());
                a.addProperty("activeUntil", activeUntil.toInstant().toString());
                a.addProperty("effectiveServers", String.join(",", effectiveServers));
                a.add("effectiveServerIds", JsonParser.parseString(effectiveServerIds.toString()));
                sendAll(a.toString(), key);
            }
            case PING -> {
                JsonObject o = new JsonObject();
                o.addProperty("action", Action.PONG.name());
                send(webSocket, o.toString(), key);
            }
        }
    }

    public byte[] generateKey() {
        try {
            String password = LocalStorage.getInstance().getData().getConnectionAPISecret();
            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);

            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, 256);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            log.info("Generated new key");
            return factory.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            log.error("Error generating key", e);
        }
        return null;
    }

    private void send(WebSocket socket, String data, byte[] key) {
        if (encrypted) socket.send(encrypt(data, key));
        else socket.send(data);
    }
    private void sendAll(String data, byte[] key) {
        for (WebSocket socket : connections.values()) {
            send(socket, data, key);
        }
    }

    private String getNameFromConnection(WebSocket socket) {
        for (String s : connections.keySet()) {
            if (connections.get(s).equals(socket)) return s;
        }
        return null;
    }

    private JsonObject createConfig() {
        JsonObject config = new JsonObject();
        config.addProperty("discordEnabled", Configuration.getInstance().isUseDiscord());
        config.addProperty("action", Action.TRANSFER_CONFIG.name());
        config.addProperty("useReports", true);

        JsonObject o = new JsonObject();
        o.addProperty("viewPortSize", 3);
        o.addProperty("enableReportViewOnline", false);
        o.addProperty("reportViewURL", "");
        JsonArray menus = new JsonArray();
        menus.add(createMenu("Main"));
        o.add("menus", menus);
        config.add("reports", o);
        return config;
    }

    private JsonObject createMenu(String name) {
        JsonObject menu = new JsonObject();
        menu.addProperty("name", name);
        JsonArray slots = new JsonArray();
        slots.add(JsonParser.parseString("""
                {"slot": 0,
                            "action": "CREATE_REPORT",
                            "display": "Skin Report",
                            "lore": [],
                            "glint": false,
                            "material": "PLAYER_HEAD",
                            "actionProperties": {
                              "reportType": "58e69752-83e4-4688-a4a4-4cf339451f1e"
                            }}"""));
        menu.add("slots", slots);
        return menu;
    }

    private boolean isEncrypted(String input) {
        try {
            JsonParser.parseString(input);
            return false;
        } catch (JsonSyntaxException e) {
            return true;
        }
    }

    private String decrypt(String encryptedBase64, byte[] key) {
        try {
            byte[] encrypted = Base64.getDecoder().decode(encryptedBase64);

            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(encrypted, 0, iv, 0, iv.length);

            byte[] ciphertext = new byte[encrypted.length - iv.length];
            System.arraycopy(encrypted, iv.length, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);

            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

            byte[] plaintext = cipher.doFinal(ciphertext);

            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error(e.getMessage());
            log.error(e.getStackTrace().toString());
            return encryptedBase64;
        }
    }

    private String encrypt(String plaintext, byte[] key) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);

            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] encrypted = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, encrypted, 0, iv.length);
            System.arraycopy(ciphertext, 0, encrypted, iv.length, ciphertext.length);

            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            return plaintext;
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
        SERVER_STOP,
        SERVER_RESTART,
        SERVER_EXECUTE_COMMAND,
        SERVER_INFO,
        PING,
        PONG,
        SERVER_SHOW_CONSOLE
    }
}
