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

public class PostgreSQLStorageProvider extends Storage {
    private static final Logger log = LoggerFactory.getLogger(PostgreSQLStorageProvider.class);

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
    public boolean allTablesExist(String[] tables) {
        return false;
    }

    @Override
    public void createTables() {
        try {
            Statement statement = conn.createStatement();
            String sql = """
                    CREATE TABLE IF NOT EXISTS users (
                        UUID varchar(36) PRIMARY KEY,
                        Username varchar(20) NOT NULL UNIQUE,
                        PasswordHashed integer NOT NULL,
                        CreationDate timestamp NULL,
                        Active smallint NOT NULL DEFAULT 1,
                        NewlyCreated smallint NOT NULL DEFAULT 1,
                        ApplyPasswordPolicy smallint NOT NULL DEFAULT 0
                    );
                    
                    CREATE TABLE IF NOT EXISTS permissions (
                        UUID varchar(36) PRIMARY KEY,
                        NameKey varchar(60) NOT NULL UNIQUE,
                        PermissionKey varchar(60) NOT NULL UNIQUE,
                        DefaultGranted smallint NULL
                    );
                    
                    CREATE TABLE IF NOT EXISTS user_permissions (
                        UserID varchar(36) NOT NULL,
                        PermissionID varchar(36) NOT NULL,
                        FOREIGN KEY (PermissionID) REFERENCES permissions(UUID),
                        FOREIGN KEY (UserID) REFERENCES users(UUID)
                    );
                    
                    CREATE TABLE IF NOT EXISTS roles (
                        UUID varchar(36) PRIMARY KEY,
                        NameKey varchar(60),
                        DisplayColor varchar(16) NOT NULL DEFAULT '0;0;0;100',
                        CreationDate timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
                    );
                    
                    CREATE TABLE IF NOT EXISTS user_roles (
                        UserID varchar(36) NOT NULL,
                        RoleID varchar(36) NOT NULL,
                        FOREIGN KEY (RoleID) REFERENCES roles(UUID),
                        FOREIGN KEY (UserID) REFERENCES users(UUID)
                    );
                    
                    CREATE TABLE IF NOT EXISTS role_permissions (
                        RoleID varchar(36) NOT NULL,
                        PermissionID varchar(36) NOT NULL,
                        FOREIGN KEY (PermissionID) REFERENCES permissions(UUID),
                        FOREIGN KEY (RoleID) REFERENCES roles(UUID)
                    );
                    
                    CREATE TABLE IF NOT EXISTS process_status_names (
                        UUID varchar(36) PRIMARY KEY,
                        StatusName varchar(36) NOT NULL UNIQUE,
                        Color varchar(16) NOT NULL DEFAULT '0;0;0;100',
                        Description varchar(150)
                    );
                    
                    CREATE TABLE IF NOT EXISTS ticket_status_names (
                        UUID varchar(36) PRIMARY KEY,
                        StatusName varchar(36) NOT NULL UNIQUE,
                        Color varchar(16) NOT NULL DEFAULT '0;0;0;100',
                        Description varchar(150)
                    );
                    
                    CREATE TABLE IF NOT EXISTS process_types (
                        TypeID varchar(36) PRIMARY KEY,
                        TypeName varchar(30) NOT NULL,
                        Active smallint NOT NULL DEFAULT 1,
                        UsePattern smallint NOT NULL DEFAULT 0,
                        PatternUsed varchar(36)
                    );
                    
                    CREATE TABLE IF NOT EXISTS processes (
                        ProcessID varchar(36) PRIMARY KEY,
                        CreatedBy varchar(36) NOT NULL,
                        Status varchar(36) NOT NULL,
                        ProcessType varchar(36) NOT NULL,
                        CreationDate timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        Comment varchar(150) NOT NULL DEFAULT 'Nothing to see here',
                        FOREIGN KEY (Status) REFERENCES process_status_names(UUID),
                        FOREIGN KEY (ProcessType) REFERENCES process_types(TypeID),
                        FOREIGN KEY (CreatedBy) REFERENCES users(UUID)
                    );
                    
                    CREATE TABLE IF NOT EXISTS tickets (
                        UUID varchar(36) PRIMARY KEY,
                        CreatedBy varchar(140),
                        HandledBy varchar(140),
                        CreationDate timestamp,
                        TicketStatus varchar(36) NOT NULL,
                        FOREIGN KEY (TicketStatus) REFERENCES ticket_status_names(UUID)
                    );
                    
                    CREATE TABLE IF NOT EXISTS ticket_transcripts (
                        TicketID varchar(36) NOT NULL,
                        SenderName varchar(50) NOT NULL,
                        MessageContent varchar(5000),
                        SentDate timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (TicketID) REFERENCES tickets(UUID)
                    );
                    
                    CREATE TABLE IF NOT EXISTS server_data (
                        UUID varchar(36) NOT NULL,
                        Name varchar(80) NOT NULL,
                        TimeStamp timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        Players integer NOT NULL DEFAULT 0,
                        cpu real NOT NULL,
                        memory integer NOT NULL,
                        TPS integer NOT NULL DEFAULT 20
                    );
                    """;
            statement.executeUpdate(sql);
        } catch (SQLException e) {
            log.error("Failed to create tables: {}", e.getMessage());
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
