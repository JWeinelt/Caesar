package de.julianweinelt.caesar.storage.providers;

import de.julianweinelt.caesar.auth.User;
import de.julianweinelt.caesar.storage.Storage;
import de.julianweinelt.caesar.storage.StorageFactory;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public class MSSQLStorageProvider extends Storage {
    private static final Logger log = LoggerFactory.getLogger(MSSQLStorageProvider.class);

    public MSSQLStorageProvider(String host, int port, String database, String user, String password) {
        super(StorageFactory.StorageType.MSSQL, host, port, database, user, password);
    }

    @Override
    public boolean connect() {
        final String DRIVER = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
        final String PARAMETERS = ";databaseName=network;encrypt=false;trustServerCertificate=true;";
        final String URL = "jdbc:sqlserver://" + getHost() + "\\" + getDatabase() + PARAMETERS;
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
            log.error("Failed to disconnect from MSSQL database: {}", e.getMessage());
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
            IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='users' AND xtype='U')
            CREATE TABLE users (
                UUID UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
                Username NVARCHAR(20) NOT NULL UNIQUE,
                PasswordHashed INT NOT NULL,
                CreationDate DATETIME NULL,
                Active BIT NOT NULL DEFAULT 1,
                NewlyCreated BIT NOT NULL DEFAULT 1,
                ApplyPasswordPolicy BIT NOT NULL DEFAULT 0
            );
            """;

            String permissions = """
            IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='permissions' AND xtype='U')
            CREATE TABLE permissions (
                UUID UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
                NameKey NVARCHAR(60) NOT NULL UNIQUE,
                PermissionKey NVARCHAR(60) NOT NULL UNIQUE,
                DefaultGranted BIT NULL
            );
            """;

            String userPermissions = """
            IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='user_permissions' AND xtype='U')
            CREATE TABLE user_permissions (
                UserID UNIQUEIDENTIFIER NOT NULL,
                PermissionID UNIQUEIDENTIFIER NOT NULL,
                FOREIGN KEY (UserID) REFERENCES users(UUID),
                FOREIGN KEY (PermissionID) REFERENCES permissions(UUID)
            );
            """;

            String roles = """
            IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='roles' AND xtype='U')
            CREATE TABLE roles (
                UUID UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
                NameKey NVARCHAR(60),
                DisplayColor NVARCHAR(16) NOT NULL DEFAULT '0;0;0;100',
                CreationDate DATETIME NOT NULL DEFAULT GETDATE()
            );
            """;

            String userRoles = """
            IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='user_roles' AND xtype='U')
            CREATE TABLE user_roles (
                UserID UNIQUEIDENTIFIER NOT NULL,
                RoleID UNIQUEIDENTIFIER NOT NULL,
                FOREIGN KEY (UserID) REFERENCES users(UUID),
                FOREIGN KEY (RoleID) REFERENCES roles(UUID)
            );
            """;

            String tickets = """
            IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='tickets' AND xtype='U')
            CREATE TABLE tickets (
                UUID UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
                UserID UNIQUEIDENTIFIER NOT NULL,
                Server NVARCHAR(64),
                Title NVARCHAR(64),
                Status INT,
                CreationDate DATETIME,
                FOREIGN KEY (UserID) REFERENCES users(UUID)
            );
            """;

            String processes = """
            IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='processes' AND xtype='U')
            CREATE TABLE processes (
                UUID UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
                NameKey NVARCHAR(64),
                Description NVARCHAR(MAX),
                CreationDate DATETIME NOT NULL DEFAULT GETDATE()
            );
            """;

            String ticketTranscripts = """
            IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='ticket_transcripts' AND xtype='U')
            CREATE TABLE ticket_transcripts (
                UUID UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
                TicketID UNIQUEIDENTIFIER NOT NULL,
                UserID UNIQUEIDENTIFIER NOT NULL,
                Content NVARCHAR(MAX),
                Timestamp DATETIME NOT NULL DEFAULT GETDATE(),
                FOREIGN KEY (TicketID) REFERENCES tickets(UUID),
                FOREIGN KEY (UserID) REFERENCES users(UUID)
            );
            """;

            String serverData = """
            IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='server_data' AND xtype='U')
            CREATE TABLE server_data (
                Server NVARCHAR(64) NOT NULL PRIMARY KEY,
                ServerName NVARCHAR(64),
                ExtraData NVARCHAR(MAX),
                CreationDate DATETIME NOT NULL DEFAULT GETDATE()
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
            log.info("Alle MSSQL-Tabellen erfolgreich erstellt (sofern nicht bereits vorhanden).");
        } catch (SQLException e) {
            log.error("Fehler beim Erstellen der MSSQL-Tabellen: {}", e.getMessage());
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
