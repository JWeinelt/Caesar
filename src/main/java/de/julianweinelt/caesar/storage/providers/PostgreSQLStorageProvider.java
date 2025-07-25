package de.julianweinelt.caesar.storage.providers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.julianweinelt.caesar.auth.CPermission;
import de.julianweinelt.caesar.auth.User;
import de.julianweinelt.caesar.auth.UserRole;
import de.julianweinelt.caesar.discord.ticket.Ticket;
import de.julianweinelt.caesar.discord.ticket.TicketStatus;
import de.julianweinelt.caesar.discord.ticket.TicketType;
import de.julianweinelt.caesar.storage.Storage;
import de.julianweinelt.caesar.storage.StorageFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class PostgreSQLStorageProvider extends Storage {
    private static final Logger log = LoggerFactory.getLogger(PostgreSQLStorageProvider.class);

    public PostgreSQLStorageProvider(String host, int port, String database, String user, String password) {
        super(host, port, database, user, password);
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
    public boolean checkConnection() {
        try {
            if (conn == null || conn.isClosed()) {
                connect();
                return false;
            }
            return true;
        } catch (SQLException e) {
            log.error("Failed to check connection: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public UUID createProcess(UUID type, UUID initialStatus, UUID creator, Optional<String> comment) {
        return null;
    }

    @Override
    public void assignPlayerToProcess(UUID process, UUID player) {

    }

    @Override
    public List<String> getUserPermissions(UUID uuid) {
        return List.of();
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

    @Override
    public UUID createPlayer() {
        return null;
    }

    @Override
    public void createPlayer(UUID uuid, int number) {

    }

    @Override
    public void createPlayer(UUID uuid) {

    }

    @Override
    public void addMCAccount(UUID player, UUID mc) {

    }

    @Override
    public void removeMCAccount(UUID player, UUID mc) {

    }

    @Override
    public void deletePlayer(UUID player) {

    }

    @Override
    public JsonObject getPlayer(UUID player) {
        return null;
    }

    @Override
    public UUID getPlayer(int player) {
        return null;
    }

    @Override
    public UUID getPlayerByAccount(String mcName) {
        return null;
    }

    @Override
    public JsonArray getProcessesForPlayer(UUID player) {
        return null;
    }

    @Override
    public JsonArray getPunishmentsForPlayer(UUID player) {
        return null;
    }

    @Override
    public JsonArray getPlayerNotes(UUID player) {
        return null;
    }

    @Override
    public void createPlayerNote(UUID player, UUID user, String note) {

    }

    @Override
    public String updateMCAccount(UUID player) {
        return "";
    }

    @Override
    public void createProcessType(String name, boolean usePattern, String pattern) {

    }

    @Override
    public void createProcessStatus(String name, String color, String description) {

    }

    @Override
    public JsonArray getProcessTypes() {
        return null;
    }

    @Override
    public JsonArray getProcessStatuses() {
        return null;
    }

    @Override
    public JsonArray getMCAccounts(UUID player) {
        return null;
    }
}
