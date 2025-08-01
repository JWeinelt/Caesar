package de.julianweinelt.caesar.integration;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class ServerConnection {
    private final UUID uuid;

    private final String name;
    private String address;
    private int port;
    private boolean encrypted = false;

    public ServerConnection(String name) {
        this.uuid = UUID.randomUUID();
        this.name = name;
    }

    public ServerConnection(UUID uuid, String name, String address, int port) {
        this.uuid = uuid;
        this.name = name;
        this.address = address;
        this.port = port;
    }

    public ServerConnection(UUID uuid, String name, String address, int port, boolean encrypted) {
        this.uuid = uuid;
        this.name = name;
        this.address = address;
        this.port = port;
        this.encrypted = encrypted;
    }
}