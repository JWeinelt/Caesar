package de.julianweinelt.caesar.storage;

import lombok.Getter;

import java.sql.Connection;

@Getter
public abstract class Storage {
    private final StorageFactory.StorageType type;

    private final String host;
    private final int port;
    private final String database;
    private final String user;
    private final String password;

    public Connection conn;

    protected Storage(StorageFactory.StorageType type, String host, int port, String database, String user, String password) {
        this.type = type;
        this.host = host;
        this.port = port;
        this.database = database;
        this.user = user;
        this.password = password;
    }



    public abstract void connect();
    public abstract void disconnect();
    public abstract void checkConnection();
}