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

    public ServerConnection(String name) {
        this.name = name;
    }

    public ServerConnection(String name, String address, int port) {
        this.name = name;
        this.address = address;
        this.port = port;
    }

    public ServerConnection(String name, String address, int port, boolean encrypted) {
        this.name = name;
        this.address = address;
        this.port = port;
        this.encrypted = encrypted;
    }
}