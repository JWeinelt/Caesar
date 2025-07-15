package de.julianweinelt.caesar.integration;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ServerConnection {
    private final String name;
    private String address;
    private int port;
    private boolean encrypted = false;
    private final String key;

    public ServerConnection(String name, String key) {
        this.name = name;
        this.key = key;
    }

    public ServerConnection(String name, String address, int port, String key) {
        this.name = name;
        this.address = address;
        this.port = port;
        this.key = key;
    }

    public ServerConnection(String name, String address, int port, boolean encrypted, String key) {
        this.name = name;
        this.address = address;
        this.port = port;
        this.encrypted = encrypted;
        this.key = key;
    }
}