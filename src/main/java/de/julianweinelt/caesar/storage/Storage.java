package de.julianweinelt.caesar.storage;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.julianweinelt.caesar.Caesar;
import de.julianweinelt.caesar.auth.CPermission;
import de.julianweinelt.caesar.auth.User;
import de.julianweinelt.caesar.auth.UserRole;
import de.julianweinelt.caesar.discord.ticket.Ticket;
import de.julianweinelt.caesar.discord.ticket.TicketStatus;
import de.julianweinelt.caesar.discord.ticket.TicketType;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Getter
public abstract class Storage {
    private static final Logger log = LoggerFactory.getLogger(Storage.class);

    private final String host;
    private final int port;
    @Setter
    private String database;
    private final String user;
    private final String password;

    public Connection conn;

    protected Storage(String host, int port, String database, String user, String password) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.user = user;
        this.password = password;
    }

    public boolean executeScript(String script) throws SQLException {

        if (script == null || script.trim().isEmpty()) {
            log.error("Invalid SQL script.");
            throw new IllegalArgumentException("SQL script cannot be null or empty.");
        }

        if (conn == null || conn.isClosed()) {
            throw new SQLException("No connection to database");
        }

        try (Statement stmt = conn.createStatement()) {
            if (script.toLowerCase().contains("drop ")) {
                log.warn("Script contains dangerous DROP commands: {}", script);
                throw new SQLException("Scripts with DROP statements are not allowed.");
            }

            boolean result = stmt.execute(script.trim());
            log.info("SQL-Skript erfolgreich ausgeführt: {}", script);
            return result;
        } catch (SQLException e) {
            log.error("Fehler beim Ausführen des SQL-Skripts: {}", e.getMessage(), e);
            throw e;
        }
    }



    public abstract boolean connect();
    public abstract void connectSandBox(Runnable runnable);
    public abstract boolean hasSandboxPermissions();
    public abstract void disconnect();
    public abstract boolean checkConnection();
    public abstract boolean allTablesExist(String[] tables);
    public abstract boolean systemDataExist();

    public abstract void createDatabase(String name);

    @Deprecated(forRemoval = true, since = "0.0.2")
    public abstract void createTables();
    public abstract void insertDefaultData();

    public abstract boolean hasTables();

    public abstract User getUser(String username);
    public abstract void deleteUser(String username);
    public abstract void updateUser(User user);
    public abstract void createUser(User user);
    public abstract List<User> getAllUsers();


    public abstract void addRole(UserRole role);
    public abstract void removeRole(UserRole role);
    public abstract List<UserRole> getAllRoles();
    public abstract void updateRolePermissions(UserRole role);
    public abstract List<CPermission> getAllPermissions();

    // Tickets
    public abstract Ticket getTicket(UUID id);
    public abstract Ticket getTicket(String channel);
    public abstract List<TicketType> getAllTicketTypes();
    public abstract List<TicketStatus> getAllTicketStatuses();
    public abstract void addTicketType(TicketType ticketType);
    public abstract void deleteTicketType(TicketType ticketType);
    public abstract void addTicketStatus(TicketStatus ticketStatus);
    public abstract void deleteTicketStatus(TicketStatus ticketStatus);
    public abstract void addTicketMessage(Ticket ticket, String message, String sender);
    public abstract void updateTicketStatus(Ticket ticket, TicketStatus ticketStatus);
    public abstract void handleTicket(Ticket ticket, String handler);
    public abstract void deleteTicket(Ticket ticket);
    public abstract void createTicket(Ticket ticket);
    public abstract void saveTicketFeedback(UUID ticket, int rating, String feedback);

    public abstract UUID createPlayer();
    public abstract void createPlayer(UUID uuid, int number);
    public abstract void createPlayer(UUID uuid);
    public abstract void addMCAccount(UUID player, UUID mc);
    public abstract void removeMCAccount(UUID player, UUID mc);
    public abstract void deletePlayer(UUID player);
    public abstract JsonObject getPlayer(UUID player);
    public abstract UUID getPlayer(int player);
    public abstract UUID getPlayerByAccount(String mcName);
    public abstract JsonArray getProcessesForPlayer(UUID player);
    public abstract JsonArray getPunishmentsForPlayer(UUID player);
    public abstract JsonArray getPlayerNotes(UUID player);
    public abstract void createPlayerNote(UUID player, UUID user, String note);
    public abstract void deletePlayerNote(UUID player, UUID user, UUID note);
    public abstract String updateMCAccount(UUID player);

    public abstract UUID createProcess(UUID type, UUID initialStatus, UUID creator, Optional<String> comment);
    public abstract void assignPlayerToProcess(UUID process, UUID player);

    public abstract void createProcessType(String name, boolean usePattern, String pattern);
    public abstract void createProcessStatus(String name, String color, String description);
    public abstract JsonArray getProcessTypes();
    public abstract JsonArray getProcessStatuses();

    public abstract List<String> getUserPermissions(UUID uuid);

    public abstract JsonArray getMCAccounts(UUID player);
}