package de.julianweinelt.caesar.storage.providers;

import de.julianweinelt.caesar.storage.Storage;
import de.julianweinelt.caesar.storage.StorageFactory;
import lombok.extern.slf4j.Slf4j;

import java.sql.DriverManager;
import java.sql.SQLException;

@Slf4j
public class OracleSQLStorageProvider extends Storage {
    public OracleSQLStorageProvider(String host, int port, String database, String user, String password) {
        super(StorageFactory.StorageType.ORACLE, host, port, database, user, password);
    }

    @Override
    public void connect() {
        final String DRIVER = "oracle.jdbc.OracleDriver";
        final String URL = "jdbc:oracle:then:@//" + getHost() + ":" + getPort() + "/" + getDatabase();
        final String USER = getUser();
        final String PASSWORD = getPassword();

        try {
            Class.forName(DRIVER);

            conn = DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (Exception e) {
            log.error("Failed to connect to Oracle database: {}", e.getMessage());
        }
    }

    @Override
    public void disconnect() {
        try {
            conn.close();
        } catch (SQLException e) {
            log.error("Failed to disconnect from Oracle database: {}", e.getMessage());
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
