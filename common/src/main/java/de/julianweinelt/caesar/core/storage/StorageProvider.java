package de.julianweinelt.caesar.core.storage;

import java.sql.Connection;
import java.sql.SQLException;

public abstract class StorageProvider {
    public Connection conn;

    public abstract void connect();
    public abstract void disconnect();
    public void checkConnection() throws SQLException {
        if (conn == null) {connect();}
        if (conn.isClosed()) connect();
    }

    public abstract void generateTables();
    public abstract void loadUserWithPermissions();
}