package de.julianweinelt.caesar.storage.providers;

import de.julianweinelt.caesar.storage.Storage;
import de.julianweinelt.caesar.storage.StorageFactory;
import lombok.extern.slf4j.Slf4j;

import java.sql.DriverManager;
import java.sql.SQLException;

@Slf4j
public class MariaDBStorageProvider extends Storage {
    public MariaDBStorageProvider(String host, int port, String database, String user, String password) {
        super(StorageFactory.StorageType.Mariadb, host, port, database, user, password);
    }

    @Override
    public void connect() {
        final String DRIVER = "org.mariadb.jdbc.Driver";
        final String PARAMETERS = "?useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC";
        final String URL = "jdbc:mariadb://" + getHost() + ":" + getPort() + "/" + getDatabase() + PARAMETERS;
        final String USER = getUser();
        final String PASSWORD = getPassword();

        try {
            Class.forName(DRIVER);

            conn = DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (Exception e) {
            log.error("Failed to connect to MariaDB database: {}", e.getMessage());
        }
    }

    @Override
    public void disconnect() {
        try {
            conn.close();
        } catch (SQLException e) {
            log.error("Failed to disconnect from MariaDB database: {}", e.getMessage());
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
