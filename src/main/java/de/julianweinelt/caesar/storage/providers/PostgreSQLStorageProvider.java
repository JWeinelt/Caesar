package de.julianweinelt.caesar.storage.providers;

import de.julianweinelt.caesar.auth.User;
import de.julianweinelt.caesar.storage.Storage;
import de.julianweinelt.caesar.storage.StorageFactory;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.sql.DriverManager;
import java.sql.SQLException;

@Slf4j
public class PostgreSQLStorageProvider extends Storage {
    public PostgreSQLStorageProvider(String host, int port, String database, String user, String password) {
        super(StorageFactory.StorageType.POSTGRESQL, host, port, database, user, password);
    }

    @Override
    public boolean connect() {
        final String DRIVER = "org.postgresql.Driver";
        final String PARAMETERS = "?ssl=false";
        final String URL = "jdbc:postgresql://" + getHost() + ":" + getPort() + "/" + getDatabase() + PARAMETERS;
        final String USER = getUser();
        final String PASSWORD = getPassword();

        try {
            Class.forName(DRIVER);

            conn = DriverManager.getConnection(URL, USER, PASSWORD);
            return true;
        } catch (Exception e) {
            log.error("Failed to connect to H2 database: {}", e.getMessage());
        }
        return false;
    }

    @Override
    public void disconnect() {
        try {
            conn.close();
        } catch (SQLException e) {
            log.error("Failed to disconnect from PostgreSQL database: {}", e.getMessage());
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

    @Override
    public void createTables() {

    }

    @Override
    public void insertDefaultData() {

    }

    @Override
    public boolean hasTables() {
        return false;
    }

    @Override
    public User getUser(String username) {
        return null;
    }

    @Override
    public void saveUser(User user) {

    }

    @Override
    public void deleteUser(String username) {

    }

    @Override
    public void updateUser(User user) {

    }

    @Override
    public void createUser(User user) {

    }

    @Override
    public void createAdminUser() {

    }
}
