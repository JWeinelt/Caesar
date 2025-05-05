package de.julianweinelt.caesar.storage.providers;

import de.julianweinelt.caesar.auth.User;
import de.julianweinelt.caesar.storage.Storage;
import de.julianweinelt.caesar.storage.StorageFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public class SQLiteStorageProvider extends Storage {
    private static final Logger log = LoggerFactory.getLogger(SQLiteStorageProvider.class);

    public SQLiteStorageProvider(String database) {
        super(StorageFactory.StorageType.SQLite, "", 0, database, "", "");
    }

    @Override
    public boolean connect() {
        final String DRIVER = "org.sqlite.JDBC";
        final String URL = "jdbc:sqlite:" + getDatabase() + ".db";
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
            log.error("Failed to disconnect from SQLite database: {}", e.getMessage());
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
    public boolean allTablesExist(String[] tables) {
        return false;
    }

    @Override
    public void createTables() {
        try {
            conn.setAutoCommit(false);
            Statement statement = conn.createStatement();

            String users = """
            CREATE TABLE IF NOT EXISTS users (
                UUID TEXT PRIMARY KEY NOT NULL,
                Username TEXT NOT NULL UNIQUE,
                PasswordHashed INTEGER NOT NULL,
                CreationDate TEXT,
                Active INTEGER NOT NULL DEFAULT 1,
                NewlyCreated INTEGER NOT NULL DEFAULT 1,
                ApplyPasswordPolicy INTEGER NOT NULL DEFAULT 0
            );
            """;

            String permissions = """
            CREATE TABLE IF NOT EXISTS permissions (
                UUID TEXT PRIMARY KEY NOT NULL,
                NameKey TEXT NOT NULL UNIQUE,
                PermissionKey TEXT NOT NULL UNIQUE,
                DefaultGranted INTEGER
            );
            """;

            String userPermissions = """
            CREATE TABLE IF NOT EXISTS user_permissions (
                UserID TEXT NOT NULL,
                PermissionID TEXT NOT NULL,
                FOREIGN KEY (UserID) REFERENCES users(UUID),
                FOREIGN KEY (PermissionID) REFERENCES permissions(UUID)
            );
            """;

            String roles = """
            CREATE TABLE IF NOT EXISTS roles (
                UUID TEXT PRIMARY KEY NOT NULL,
                NameKey TEXT,
                DisplayColor TEXT NOT NULL DEFAULT '0;0;0;100',
                CreationDate TEXT NOT NULL DEFAULT (datetime('now'))
            );
            """;

            String userRoles = """
            CREATE TABLE IF NOT EXISTS user_roles (
                UserID TEXT NOT NULL,
                RoleID TEXT NOT NULL,
                FOREIGN KEY (UserID) REFERENCES users(UUID),
                FOREIGN KEY (RoleID) REFERENCES roles(UUID)
            );
            """;

            String tickets = """
            CREATE TABLE IF NOT EXISTS tickets (
                UUID TEXT PRIMARY KEY NOT NULL,
                UserID TEXT NOT NULL,
                Server TEXT,
                Title TEXT,
                Status INTEGER,
                CreationDate TEXT,
                FOREIGN KEY (UserID) REFERENCES users(UUID)
            );
            """;

            String processes = """
            CREATE TABLE IF NOT EXISTS processes (
                UUID TEXT PRIMARY KEY NOT NULL,
                NameKey TEXT,
                Description TEXT,
                CreationDate TEXT NOT NULL DEFAULT (datetime('now'))
            );
            """;

            String ticketTranscripts = """
            CREATE TABLE IF NOT EXISTS ticket_transcripts (
                UUID TEXT PRIMARY KEY NOT NULL,
                TicketID TEXT NOT NULL,
                UserID TEXT NOT NULL,
                Content TEXT,
                Timestamp TEXT NOT NULL DEFAULT (datetime('now')),
                FOREIGN KEY (TicketID) REFERENCES tickets(UUID),
                FOREIGN KEY (UserID) REFERENCES users(UUID)
            );
            """;

            String serverData = """
            CREATE TABLE IF NOT EXISTS server_data (
                Server TEXT PRIMARY KEY NOT NULL,
                ServerName TEXT,
                ExtraData TEXT,
                CreationDate TEXT NOT NULL DEFAULT (datetime('now'))
            );
            """;

            statement.executeUpdate(users);
            statement.executeUpdate(permissions);
            statement.executeUpdate(userPermissions);
            statement.executeUpdate(roles);
            statement.executeUpdate(userRoles);
            statement.executeUpdate(tickets);
            statement.executeUpdate(processes);
            statement.executeUpdate(ticketTranscripts);
            statement.executeUpdate(serverData);

            conn.commit();
            conn.setAutoCommit(true);
            log.info("Alle SQLite-Tabellen erfolgreich erstellt.");
        } catch (SQLException e) {
            log.error("Fehler beim Erstellen der SQLite-Tabellen: {}", e.getMessage());
            try {
                conn.rollback();
            } catch (SQLException ex) {
                log.error("Rollback fehlgeschlagen: {}", ex.getMessage());
            }
        }
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
    public void deleteUser(String username) {

    }

    @Override
    public void updateUser(User user) {

    }

    @Override
    public void createUser(User user) {

    }

    @Override
    public List<User> getAllUsers() {
        return List.of();
    }
}
