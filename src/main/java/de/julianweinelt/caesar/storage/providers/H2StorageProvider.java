package de.julianweinelt.caesar.storage.providers;

import de.julianweinelt.caesar.storage.Storage;
import de.julianweinelt.caesar.storage.StorageFactory;
import lombok.extern.slf4j.Slf4j;

import java.sql.DriverManager;
import java.sql.SQLException;

@Slf4j
public class H2StorageProvider extends Storage {
    public H2StorageProvider(String database) {
        super(StorageFactory.StorageType.H2, "", 0, database, "", "");
    }

    @Override
    public void connect() {
        final String DRIVER = "org.h2.Driver";
        final String URL = "jdbc:h2:./data/" + getDatabase();
        final String USER = getUser();
        final String PASSWORD = getPassword();

        try {
            Class.forName(DRIVER);

            conn = DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (Exception e) {
            log.error("Failed to connect to H2 database: {}", e.getMessage());
        }
    }

    @Override
    public void disconnect() {
        try {
            conn.close();
        } catch (SQLException e) {
            log.error("Failed to disconnect from H2 database: {}", e.getMessage());
        }
    }

    @Override
    public void checkConnection() {
        try {
            if (conn == null || conn.isClosed()) connect();
        } catch (SQLException e) {
            log.error("Failed to check connection: {}", e.getMessage());
        }
    }
}
