package de.julianweinelt.caesar.endpoint.chat.voice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class VoiceServer {
    private static final Logger log = LoggerFactory.getLogger(VoiceServer.class);
    private final int SOCKET_TIMEOUT_MS = 1000;

    private final DatagramSocket socket;
    private final Map<UUID, Set<SocketAddress>> sessions = new ConcurrentHashMap<>();
    private final Map<SocketAddress, UUID> clientRooms = new ConcurrentHashMap<>();
    private final Map<SocketAddress, UUID> clientIDs = new ConcurrentHashMap<>();
    private final Map<SocketAddress, Integer> bitRates = new ConcurrentHashMap<>();

    private volatile boolean running = true;

    public VoiceServer(int port) throws SocketException {
        socket = new DatagramSocket(port);
        socket.setSoTimeout(SOCKET_TIMEOUT_MS);
    }

    public void start() {
        log.info("Starting VoiceServer on port {}", socket.getLocalPort());
        byte[] buffer = new byte[4096];

        while (running) {
            try {
                DatagramPacket udpPacket = new DatagramPacket(buffer, buffer.length);
                socket.receive(udpPacket);

                VoicePacket packet = VoicePacket.fromBytes(buffer, udpPacket.getLength());
                SocketAddress sender = udpPacket.getSocketAddress();

                switch (packet.type) {
                    case 0x00 -> handleAudio(sender, packet.payload);
                    case 0x01 -> handleEvent(sender, new String(packet.payload, StandardCharsets.UTF_8));
                    case 0x02 -> handleControl(sender, new String(packet.payload, StandardCharsets.UTF_8));
                    default -> log.warn("Unknown packet type {} from {}", packet.type, sender);
                }

            } catch (SocketTimeoutException e) {
                log.debug("Socket timeout on port {}", socket.getLocalPort());
            } catch (IOException e) {
                if (running) log.error("Error in VoiceServer loop: {}", e.getMessage(), e);
            }
        }

        socket.close();
        log.info("VoiceServer stopped.");
    }

    public void stop() {
        log.info("Received shutdown request.");
        running = false;
        socket.close();
    }


    private void handleControl(SocketAddress client, String msg) throws IOException {
        // join:<roomUUID>:<userUUID>
        String[] parts = msg.split(":");
        if (parts.length >= 2) {
            String action = parts[0];
            UUID room = UUID.fromString(parts[1]);
            UUID userID = parts.length >= 3 ? UUID.fromString(parts[2]) : UUID.randomUUID();
            int bitrate = parts.length >= 4 ? Integer.parseInt(parts[3]) : 32; // default

            switch (action) {
                case "join" -> {
                    clientRooms.put(client, room);
                    clientIDs.put(client, userID);
                    sessions.computeIfAbsent(room, r -> ConcurrentHashMap.newKeySet()).add(client);
                    bitRates.put(client, bitrate);

                    log.info("{} joined room {}", userID, room);
                    log.debug("{} is using {}kbps as bitrate", userID, bitrate);
                    broadcastEvent(room, "joined:" + userID, client);
                }
                case "leave" -> {
                    leaveRoom(client, room, userID);
                }
                default -> log.warn("Unknown control action '{}' from {}", action, client);
            }
        }
    }

    private void handleEvent(SocketAddress client, String msg) throws IOException {
        UUID room = clientRooms.get(client);
        if (room != null) {
            broadcastEvent(room, msg, client);
        }
    }

    private void handleAudio(SocketAddress sender, byte[] data) throws IOException {
        UUID room = clientRooms.get(sender);
        if (room == null) return;

        Set<SocketAddress> members = sessions.getOrDefault(room, Set.of());
        byte[] packet = VoicePacket.createAudio(data).toBytes();

        for (SocketAddress member : members) {
            if (!member.equals(sender)) {
                socket.send(new DatagramPacket(packet, packet.length, member));
            }
        }
    }

    private void leaveRoom(SocketAddress client, UUID room, UUID userID) throws IOException {
        clientRooms.remove(client);
        clientIDs.remove(client);

        Set<SocketAddress> members = sessions.get(room);
        if (members != null) {
            members.remove(client);
            broadcastEvent(room, "left:" + userID, client);
            if (members.isEmpty()) {
                sessions.remove(room);
            }
        }

        log.info("{} left room {}", userID, room);
    }

    private void broadcastEvent(UUID room, String event, SocketAddress except) throws IOException {
        VoicePacket packet = VoicePacket.createEvent(event);
        byte[] bytes = packet.toBytes();
        for (SocketAddress member : sessions.getOrDefault(room, Set.of())) {
            if (!member.equals(except)) {
                socket.send(new DatagramPacket(bytes, bytes.length, member));
            }
        }
    }
}
