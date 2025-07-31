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
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class H2StorageProvider extends Storage {
    private static final Logger log = LoggerFactory.getLogger(H2StorageProvider.class);

    public H2StorageProvider(String database) {
        super("", 0, database, "", "");
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
    public void createTables() {}

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
    public void deletePlayerNote(UUID player, UUID user, UUID note) {

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
