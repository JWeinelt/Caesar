package de.julianweinelt.caesar.storage.providers;

import de.julianweinelt.caesar.auth.CPermission;
import de.julianweinelt.caesar.auth.User;
import de.julianweinelt.caesar.auth.UserRole;
import de.julianweinelt.caesar.storage.Storage;
import de.julianweinelt.caesar.storage.StorageFactory;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

public class OracleSQLStorageProvider extends Storage {
    private static final Logger log = LoggerFactory.getLogger(OracleSQLStorageProvider.class);

    public OracleSQLStorageProvider(String host, int port, String database, String user, String password) {
        super(StorageFactory.StorageType.ORACLE, host, port, database, user, password);
    }

    @Override
    public boolean connect() {
        final String DRIVER = "oracle.jdbc.OracleDriver";
        final String URL = "jdbc:oracle:then:@//" + getHost() + ":" + getPort() + "/" + getDatabase();
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

    @Override
    public boolean allTablesExist(String[] tables) {
        return false;
    }

    @Override
    public boolean systemDataExist() {
        return false;
    }

    @Override
    public void createTables() {
        try {
            String sql = """
                    CREATE TABLE users (
                        UUID VARCHAR2(36) PRIMARY KEY,
                        Username VARCHAR2(20) NOT NULL UNIQUE,
                        PasswordHashed NUMBER NOT NULL,
                        CreationDate TIMESTAMP NULL,
                        Active NUMBER(1) DEFAULT 1 NOT NULL,
                        NewlyCreated NUMBER(1) DEFAULT 1 NOT NULL,
                        ApplyPasswordPolicy NUMBER(1) DEFAULT 0 NOT NULL
                    );
                    
                    CREATE TABLE permissions (
                        UUID VARCHAR2(36) PRIMARY KEY,
                        NameKey VARCHAR2(60) NOT NULL UNIQUE,
                        PermissionKey VARCHAR2(60) NOT NULL UNIQUE,
                        DefaultGranted NUMBER(1)
                    );
                    
                    CREATE TABLE user_permissions (
                        UserID VARCHAR2(36) NOT NULL,
                        PermissionID VARCHAR2(36) NOT NULL,
                        CONSTRAINT user_permissions__perm_fk FOREIGN KEY (PermissionID) REFERENCES permissions(UUID),
                        CONSTRAINT user_permissions_users_UUID_fk FOREIGN KEY (UserID) REFERENCES users(UUID)
                    );
                    
                    CREATE TABLE roles (
                        UUID VARCHAR2(36) PRIMARY KEY,
                        NameKey VARCHAR2(60),
                        DisplayColor VARCHAR2(16) DEFAULT '0;0;0;100' NOT NULL,
                        CreationDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
                    );
                    
                    CREATE TABLE user_roles (
                        UserID VARCHAR2(36) NOT NULL,
                        RoleID VARCHAR2(36) NOT NULL,
                        CONSTRAINT user_roles_roles_role_fk FOREIGN KEY (RoleID) REFERENCES roles(UUID),
                        CONSTRAINT user_roles_users_user_fk FOREIGN KEY (UserID) REFERENCES users(UUID)
                    );
                    
                    CREATE TABLE role_permissions (
                        RoleID VARCHAR2(36) NOT NULL,
                        PermissionID VARCHAR2(36) NOT NULL,
                        CONSTRAINT role_permissions_permissions_id_fk FOREIGN KEY (PermissionID) REFERENCES permissions(UUID),
                        CONSTRAINT role_permissions_roles_id_fk FOREIGN KEY (RoleID) REFERENCES roles(UUID)
                    );
                    
                    CREATE TABLE process_status_names (
                        UUID VARCHAR2(36) PRIMARY KEY,
                        StatusName VARCHAR2(36) NOT NULL UNIQUE,
                        Color VARCHAR2(16) DEFAULT '0;0;0;100' NOT NULL,
                        Description VARCHAR2(150)
                    );
                    
                    CREATE TABLE ticket_status_names (
                        UUID VARCHAR2(36) PRIMARY KEY,
                        StatusName VARCHAR2(36) NOT NULL UNIQUE,
                        Color VARCHAR2(16) DEFAULT '0;0;0;100' NOT NULL,
                        Description VARCHAR2(150)
                    );
                    
                    CREATE TABLE process_types (
                        TypeID VARCHAR2(36) PRIMARY KEY,
                        TypeName VARCHAR2(30) NOT NULL,
                        Active NUMBER(1) DEFAULT 1 NOT NULL,
                        UsePattern NUMBER(1) DEFAULT 0 NOT NULL,
                        PatternUsed VARCHAR2(36)
                    );
                    
                    CREATE TABLE processes (
                        ProcessID VARCHAR2(36) PRIMARY KEY,
                        CreatedBy VARCHAR2(36) NOT NULL,
                        Status VARCHAR2(36) NOT NULL,
                        ProcessType VARCHAR2(36) NOT NULL,
                        CreationDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                        Comment VARCHAR2(150) DEFAULT 'Nothing to see here' NOT NULL,
                        CONSTRAINT processes_process_status_names_UUID_fk FOREIGN KEY (Status) REFERENCES process_status_names(UUID),
                        CONSTRAINT processes_process_types_TypeID_fk FOREIGN KEY (ProcessType) REFERENCES process_types(TypeID),
                        CONSTRAINT processes_users_UUID_fk FOREIGN KEY (CreatedBy) REFERENCES users(UUID)
                    );
                    
                    CREATE TABLE tickets (
                        UUID VARCHAR2(36) PRIMARY KEY,
                        CreatedBy VARCHAR2(140),
                        HandledBy VARCHAR2(140),
                        CreationDate TIMESTAMP,
                        TicketStatus VARCHAR2(36) NOT NULL,
                        CONSTRAINT tickets_ticket_status_names_UUID_fk FOREIGN KEY (TicketStatus) REFERENCES ticket_status_names(UUID)
                    );
                    
                    CREATE TABLE ticket_transcripts (
                        TicketID VARCHAR2(36) NOT NULL,
                        SenderName VARCHAR2(50) NOT NULL,
                        MessageContent VARCHAR2(5000),
                        SentDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                        CONSTRAINT ticket_transcripts_tickets_UUID_fk FOREIGN KEY (TicketID) REFERENCES tickets(UUID)
                    );
                    
                    CREATE TABLE server_data (
                        UUID VARCHAR2(36) NOT NULL,
                        Name VARCHAR2(80) NOT NULL,
                        TimeStamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                        Players NUMBER DEFAULT 0 NOT NULL,
                        cpu FLOAT NOT NULL,
                        memory NUMBER NOT NULL,
                        TPS NUMBER DEFAULT 20 NOT NULL
                    );
                    """;
            conn.createStatement().executeUpdate(sql);
        } catch (SQLException e) {
            log.error("Error creating tables: {}", e.getMessage());
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

    @Override
    public void addRole(UserRole role) {

    }

    @Override
    public void removeRole(UserRole role) {

    }

    @Override
    public List<UserRole> getAllRoles() {
        return List.of();
    }

    @Override
    public void updateRolePermissions(UserRole role) {

    }

    @Override
    public List<CPermission> getAllPermissions() {
        return List.of();
    }
}
