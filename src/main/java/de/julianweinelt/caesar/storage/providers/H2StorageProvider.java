package de.julianweinelt.caesar.storage.providers;

import de.julianweinelt.caesar.auth.CPermission;
import de.julianweinelt.caesar.auth.User;
import de.julianweinelt.caesar.auth.UserRole;
import de.julianweinelt.caesar.discord.ticket.Ticket;
import de.julianweinelt.caesar.discord.ticket.TicketStatus;
import de.julianweinelt.caesar.discord.ticket.TicketType;
import de.julianweinelt.caesar.storage.Storage;
import de.julianweinelt.caesar.storage.StorageFactory;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class H2StorageProvider extends Storage {
    private static final Logger log = LoggerFactory.getLogger(H2StorageProvider.class);

    public H2StorageProvider(String database) {
        super(StorageFactory.StorageType.H2, "", 0, database, "", "");
    }

    @Override
    public boolean connect() {
        final String DRIVER = "org.h2.Driver";
        final String URL = "jdbc:h2:./data/" + getDatabase();
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
                        UUID VARCHAR(36) PRIMARY KEY,
                        Username VARCHAR(20) NOT NULL UNIQUE,
                        PasswordHashed BIGINT NOT NULL,
                        CreationDate TIMESTAMP NULL,
                        Active BOOLEAN DEFAULT TRUE NOT NULL,
                        NewlyCreated BOOLEAN DEFAULT TRUE NOT NULL,
                        ApplyPasswordPolicy BOOLEAN DEFAULT FALSE NOT NULL
                    );
                    
                    CREATE TABLE permissions (
                        UUID VARCHAR(36) PRIMARY KEY,
                        NameKey VARCHAR(60) NOT NULL UNIQUE,
                        PermissionKey VARCHAR(60) NOT NULL UNIQUE,
                        DefaultGranted BOOLEAN
                    );
                    
                    CREATE TABLE user_permissions (
                        UserID VARCHAR(36) NOT NULL,
                        PermissionID VARCHAR(36) NOT NULL,
                        CONSTRAINT user_permissions__perm_fk FOREIGN KEY (PermissionID) REFERENCES permissions(UUID),
                        CONSTRAINT user_permissions_users_UUID_fk FOREIGN KEY (UserID) REFERENCES users(UUID)
                    );
                    
                    CREATE TABLE roles (
                        UUID VARCHAR(36) PRIMARY KEY,
                        NameKey VARCHAR(60),
                        DisplayColor VARCHAR(16) DEFAULT '0;0;0;100' NOT NULL,
                        CreationDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
                    );
                    
                    CREATE TABLE user_roles (
                        UserID VARCHAR(36) NOT NULL,
                        RoleID VARCHAR(36) NOT NULL,
                        CONSTRAINT user_roles_roles_role_fk FOREIGN KEY (RoleID) REFERENCES roles(UUID),
                        CONSTRAINT user_roles_users_user_fk FOREIGN KEY (UserID) REFERENCES users(UUID)
                    );
                    
                    CREATE TABLE role_permissions (
                        RoleID VARCHAR(36) NOT NULL,
                        PermissionID VARCHAR(36) NOT NULL,
                        CONSTRAINT role_permissions_permissions_id_fk FOREIGN KEY (PermissionID) REFERENCES permissions(UUID),
                        CONSTRAINT role_permissions_roles_id_fk FOREIGN KEY (RoleID) REFERENCES roles(UUID)
                    );
                    
                    CREATE TABLE process_status_names (
                        UUID VARCHAR(36) PRIMARY KEY,
                        StatusName VARCHAR(36) NOT NULL UNIQUE,
                        Color VARCHAR(16) DEFAULT '0;0;0;100' NOT NULL,
                        Description VARCHAR(150)
                    );
                    
                    CREATE TABLE ticket_status_names (
                        UUID VARCHAR(36) PRIMARY KEY,
                        StatusName VARCHAR(36) NOT NULL UNIQUE,
                        Color VARCHAR(16) DEFAULT '0;0;0;100' NOT NULL,
                        Description VARCHAR(150)
                    );
                    
                    CREATE TABLE process_types (
                        TypeID VARCHAR(36) PRIMARY KEY,
                        TypeName VARCHAR(30) NOT NULL,
                        Active BOOLEAN DEFAULT TRUE NOT NULL,
                        UsePattern BOOLEAN DEFAULT FALSE NOT NULL,
                        PatternUsed VARCHAR(36)
                    );
                    
                    CREATE TABLE processes (
                        ProcessID VARCHAR(36) PRIMARY KEY,
                        CreatedBy VARCHAR(36) NOT NULL,
                        Status VARCHAR(36) NOT NULL,
                        ProcessType VARCHAR(36) NOT NULL,
                        CreationDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                        Comment VARCHAR(150) DEFAULT 'Nothing to see here' NOT NULL,
                        CONSTRAINT processes_process_status_names_UUID_fk FOREIGN KEY (Status) REFERENCES process_status_names(UUID),
                        CONSTRAINT processes_process_types_TypeID_fk FOREIGN KEY (ProcessType) REFERENCES process_types(TypeID),
                        CONSTRAINT processes_users_UUID_fk FOREIGN KEY (CreatedBy) REFERENCES users(UUID)
                    );
                    
                    CREATE TABLE tickets (
                        UUID VARCHAR(36) PRIMARY KEY,
                        CreatedBy VARCHAR(140),
                        HandledBy VARCHAR(140),
                        CreationDate TIMESTAMP,
                        TicketStatus VARCHAR(36) NOT NULL,
                        CONSTRAINT tickets_ticket_status_names_UUID_fk FOREIGN KEY (TicketStatus) REFERENCES ticket_status_names(UUID)
                    );
                    
                    CREATE TABLE ticket_transcripts (
                        TicketID VARCHAR(36) NOT NULL,
                        SenderName VARCHAR(50) NOT NULL,
                        MessageContent VARCHAR(5000),
                        SentDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                        CONSTRAINT ticket_transcripts_tickets_UUID_fk FOREIGN KEY (TicketID) REFERENCES tickets(UUID)
                    );
                    
                    CREATE TABLE server_data (
                        UUID VARCHAR(36) NOT NULL,
                        Name VARCHAR(80) NOT NULL,
                        TimeStamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                        Players INT DEFAULT 0 NOT NULL,
                        cpu DOUBLE NOT NULL,
                        memory BIGINT NOT NULL,
                        TPS DOUBLE DEFAULT 20 NOT NULL
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

    @Override
    public Ticket getTicket(UUID id) {
        return null;
    }

    @Override
    public Ticket getTicket(String channel) {
        return null;
    }

    @Override
    public List<TicketType> getAllTicketTypes() {
        return List.of();
    }

    @Override
    public List<TicketStatus> getAllTicketStatuses() {
        return List.of();
    }

    @Override
    public void addTicketType(TicketType ticketType) {

    }

    @Override
    public void deleteTicketType(TicketType ticketType) {

    }

    @Override
    public void addTicketStatus(TicketStatus ticketStatus) {

    }

    @Override
    public void deleteTicketStatus(TicketStatus ticketStatus) {

    }

    @Override
    public void addTicketMessage(Ticket ticket, String message, String sender) {

    }

    @Override
    public void updateTicketStatus(Ticket ticket, TicketStatus ticketStatus) {

    }

    @Override
    public void handleTicket(Ticket ticket, String handler) {

    }

    @Override
    public void deleteTicket(Ticket ticket) {

    }

    @Override
    public void createTicket(Ticket ticket) {

    }
}
