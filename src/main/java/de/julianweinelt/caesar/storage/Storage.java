package de.julianweinelt.caesar.storage;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.julianweinelt.caesar.auth.CPermission;
import de.julianweinelt.caesar.auth.User;
import de.julianweinelt.caesar.auth.UserRole;
import de.julianweinelt.caesar.discord.ticket.Ticket;
import de.julianweinelt.caesar.discord.ticket.TicketStatus;
import de.julianweinelt.caesar.discord.ticket.TicketType;
import de.julianweinelt.caesar.util.DatabaseColorParser;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

import java.awt.*;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@Getter
public abstract class Storage {
    private final String host;
    private final int port;
    private final String database;
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

    public boolean executeScript(String script) {
        if (conn == null) return false;
        try {
            conn.createStatement().execute(script);
        } catch (SQLException e) {
            return false;
        }
        return true;
    }


    /**
     * Creates a connection to the database. Will set the {@link #conn} field.<br><br>
     * <b>Important: </b>This method should always call {@link #executeAfterConnection()} after a successful connection!
     * @return True if the connection was successful, false otherwise.
     */
    public abstract boolean connect();

    /**
     * Disconnects from the database. Will set the {@link #conn} field to null.
     * Overriding this method should do something like {@link Connection#close()} to the {@link #conn} object.
     */
    public abstract void disconnect();

    /**
     * Checks if the connection to the database is still valid.
     * If not, it should try to reconnect.
     * @return True if the connection is valid, false otherwise.
     */
    public abstract boolean checkConnection();

    /**
     * Checks whether all tables in the specified array exist in the database.
     *
     * <p>An example implementation in MySQL could look like this:</p>
     *
     * <pre>{@code
     * try {
     *     DatabaseMetaData meta = conn.getMetaData();
     *     for (String table : tables) {
     *         try (ResultSet rs = meta.getTables(null, null, table, new String[] {"TABLE"})) {
     *             if (!rs.next()) {
     *                 log.warn("Table '{}' does not exist!", table);
     *                 return false;
     *             }
     *         }
     *     }
     *     return true;
     * } catch (SQLException e) {
     *     log.error("Error while checking database: {}", e.getMessage());
     *     return false;
     * }
     * }</pre>
     *
     * @param tables the table names to verify
     * @return {@code true} if all tables exist, {@code false} otherwise
     */

    public abstract boolean allTablesExist(String[] tables);

    /**
     * Checks if all necessary system data exists in the database.
     * This includes, but is not limited to: default roles, permissions, and initial configurations.
     * @return True if all necessary system data exists, false otherwise.
     */
    public abstract boolean systemDataExist();

    /**
     * Called after a successful connection to the database.
     * This method can be used to perform any setup or initialization tasks
     * that require an active database connection.
     */
    public abstract void executeAfterConnection();

    /**
     * This method should create all necessary tables in the database.
     * @deprecated Use migration scripts instead.
     */
    @Deprecated(forRemoval = true, since = "0.0.2")
    public abstract void createTables();

    /**
     * This should insert all necessary default data into the database.
     * This includes, but is not limited to: default roles, permissions, and initial configurations.
     */
    public abstract void insertDefaultData();

    public abstract boolean hasTables();

    /**
     * This method should load the user data by the given name from the database and construct a {@link User} object from it.
     * @param username The username of the user to load.
     * @return The {@link User} object representing the loaded user.
     */
    public abstract User getUser(String username);

    /**
     * This method should delete the user with the given username from the database.
     * @param username The username of the user to delete.
     */
    public abstract void deleteUser(String username);

    /**
     * This method should update the given user in the database.
     * Possible implementations could update fields like password, email, roles, etc.<br>
     * <b>IMPORTANT: </b>Under no circumstances should the unique ID of the user be changed!<br><br>
     * Update only changed fields to optimize performance, if possible.
     * @param user The {@link User} object representing the user to update.
     */
    public abstract void updateUser(User user);

    /**
     * This method should create a new user in the database based on the given {@link User} object.
     * <b>IMPORTANT: </b>Under no circumstances should the password be stored in plain text! It should be hashed and salted before storing.
     * @param user The {@link User} object representing the user to create.
     */
    public abstract void createUser(User user);

    /**
     * This method should return a list of all users in the database.
     * @return A {@link List} of {@link User} objects representing all users.
     */
    public abstract List<User> getAllUsers();


    /**
     * This method should add the given role to the database.
     * @param role The {@link UserRole} object representing the role to add.
     */
    public abstract void addRole(UserRole role);

    /**
     * This method should remove the given role from the database.
     * @param role The {@link UserRole} object representing the role to remove.
     */
    public abstract void removeRole(UserRole role);

    /**
     * This method should return a list of all roles in the database.
     * @return A {@link List} of {@link UserRole} objects representing all roles.
     */
    public abstract List<UserRole> getAllRoles();

    /**
     * This method should update the permissions of the given role in the database.<br>
     * It should only use the {@link UserRole#getPermissions()} field to update the permissions.
     * @param role The {@link UserRole} object representing the role to update.
     */
    public abstract void updateRolePermissions(UserRole role);

    /**
     * This method should return a list of all permissions stored in the database.
     * @return A {@link List} of {@link CPermission} objects representing all permissions.
     */
    public abstract List<CPermission> getAllPermissions();

    // Tickets

    /**
     * Gets a ticket by its unique ID.
     * @param id The unique ID of the ticket as a {@link UUID} object.
     * @return The {@link Ticket} object representing the ticket with the given ID, or null if not found.
     */
    public abstract Ticket getTicket(UUID id);

    /**
     * Gets a ticket by its associated channel ID.
     * @param channel The channel ID of the ticket as a {@link String}.
     * @return The {@link Ticket} object representing the ticket with the given channel ID, or null if not found.
     */
    public abstract Ticket getTicket(String channel);

    /**
     * Gets all ticket types from the database.
     * @return A {@link List} of {@link TicketType} objects representing all ticket types.
     */
    public abstract List<TicketType> getAllTicketTypes();

    /**
     * Gets all ticket statuses from the database.
     * @return A {@link List} of {@link TicketStatus} objects representing all ticket statuses.
     */
    public abstract List<TicketStatus> getAllTicketStatuses();

    /**
     * Adds a new ticket type to the database.<br>
     * This method is also being used to update existing ticket types. Please acknowledge that in implementation.
     * @param ticketType The {@link TicketType} object representing the ticket type to add.
     */
    public abstract void addTicketType(TicketType ticketType);

    /**
     * Deletes a ticket type from the database.
     * @param ticketType The {@link TicketType} object representing the ticket type to delete.
     */
    public abstract void deleteTicketType(TicketType ticketType);
    /**
     * Adds a new ticket status to the database.<br>
     * This method is also being used to update existing ticket statuses. Please acknowledge that in implementation.
     * @param ticketStatus The {@link TicketStatus} object representing the ticket status to add.
     */
    public abstract void addTicketStatus(TicketStatus ticketStatus);
    /**
     * Deletes a ticket status from the database.
     * @param ticketStatus The {@link TicketStatus} object representing the ticket status to delete.
     */
    public abstract void deleteTicketStatus(TicketStatus ticketStatus);

    /**
     * Adds a message to the given ticket's message history.<br><br>
     * This method is being called whenever a new message is sent in a ticket channel.
     * @param ticket A {@link Ticket} object representing the ticket to which the message should be added.
     * @param message The message content as a {@link String}.
     * @param sender The sender of the message as a {@link String} representing Discord's user ID.
     */
    public abstract void addTicketMessage(Ticket ticket, String message, String sender);

    /**
     * Updates the status of the given ticket in the database.
     * @param ticket The {@link Ticket} object representing the ticket to update.
     * @param ticketStatus The new {@link TicketStatus} to set for the ticket.
     */
    public abstract void updateTicketStatus(Ticket ticket, TicketStatus ticketStatus);

    /**
     * Sets the handling user of the given ticket in the database.
     * @param ticket The {@link Ticket} object representing the ticket to update.
     * @param handler The handler of the ticket as a {@link String} representing Discord's user ID.
     */
    public abstract void handleTicket(Ticket ticket, String handler); //TODO: Also support Caesar user IDs

    /**
     * Deletes the given ticket from the database.<br>
     * By default, users should never fully delete tickets. Instead, they should be archived.
     * This method is primarily intended for administrative purposes.
     * @param ticket The {@link Ticket} object representing the ticket to delete.
     */
    public abstract void deleteTicket(Ticket ticket);

    /**
     * Creates a new ticket in the database.
     * @param ticket The {@link Ticket} object representing the ticket to create.
     */
    public abstract void createTicket(Ticket ticket);

    /**
     * Creates a new player row in the database.
     * Players usually have to index fields: UUID and the player's number.
     * This method should automatically assign the next available player number
     * and generate a new UUID using {@link UUID#randomUUID()} that will be returned afterward.
     * @return The {@link UUID} of the newly created player.
     */
    public abstract UUID createPlayer();

    /**
     * Creates a new player row in the database with the given UUID and player number.
     * @param uuid A {@link UUID} representing the player's unique ID.
     * @param number An {@code int} representing the player's number.
     */
    public abstract void createPlayer(UUID uuid, int number);

    /**
     * Creates a new player row in the database with the given UUID.
     * A player number should be generated with a randomizer such as {@link SecureRandom#nextInt()}.
     * @param uuid A {@link UUID} representing the player's unique ID.
     */
    public abstract void createPlayer(UUID uuid);

    /**
     * Assigns a Minecraft account UUID to the internal player UUID.
     * @param player The player's {@link UUID} used in Caesar.
     * @param mc The Minecraft account's {@link UUID} to link to the player.
     */
    public abstract void addMCAccount(UUID player, UUID mc);

    /**
     * Removes a Minecraft account UUID from the internal player UUID.
     * @param player The player's {@link UUID} used in Caesar.
     * @param mc The Minecraft account's {@link UUID} to unlink from the player.
     */
    public abstract void removeMCAccount(UUID player, UUID mc);

    /**
     * Deletes the player with the given UUID from the database.<br><br>
     * For developers:
     * Note that you may have to delete data related to the player in multiple tables, and may have to
     * anonymize certain data in tables for processes, notes, etc.
     * @param player The player's {@link UUID} used in Caesar.
     */
    public abstract void deletePlayer(UUID player);

    /**
     * Gets the player data as a {@link JsonObject} by the given player's UUID.
     * @param player The player's {@link UUID} used in Caesar.
     * @return A {@link JsonObject} representing the player's data.
     * @apiNote
     * The returned JsonObject should look similar to this:
     * <pre>{@code
     * {
     *     "uuid": "player-uuid",
     *     "playerNumber": 12345,
     *     "mcAccounts": [],
     *     "processes": [],
     *     "punishments": [],
     *     "notes": []
     * }
     * }</pre>
     */
    public abstract JsonObject getPlayer(UUID player);

    /**
     * Gets the player's UUID by their player number.
     * @param player The player's number as an {@code int}.
     * @return The player's {@link UUID} used in Caesar.
     */
    public abstract UUID getPlayer(int player);

    /**
     * Gets the player's UUID by their Minecraft account name.
     * @param mcName The Minecraft account's name as a {@link String}.
     * @return The player's {@link UUID} used in Caesar.
     * @apiNote Sometimes
     */
    public abstract UUID getPlayerByAccount(String mcName);

    /**
     * Gets all processes associated with the given player UUID.
     * @param player The player's {@link UUID} used in Caesar.
     * @return A {@link JsonArray} containing all processes related to the player.
     * @apiNote The returned JsonArray should contain JsonObjects similar to this:
     * <pre>{@code
     * {
     *     "processID": "process-uuid",
     *     "type": "process-type-uuid",
     *     "status": "process-status-uuid",
     *     "createdBy": "creator-user-uuid",
     *     "comment": "optional-comment",
     * }
     * }</pre>
     */
    public abstract JsonArray getProcessesForPlayer(UUID player);

    /**
     * Gets all punishments associated with the given player UUID.
     * @param player The player's {@link UUID} used in Caesar.
     * @return A {@link JsonArray} containing all punishments related to the player.
     * @apiNote The returned JsonArray should contain JsonObjects similar to this:
     * <pre>{@code
     * {
     *     "RecordID": "punishment-uuid",
     *     "PunishmentType": "punishment-type",
     *     "PlayerID": "player-uuid",
     *     "CreateUserType": "user-type-uuid",
     *     "CreatedBy": "creator-user-uuid",
     *     "ActionUntil": "timestamp-or-null",
     *     "Reason": "reason-text",
     *     "Punishment-Name": "punishment-name",
     *     "TimedPossible": true/false,
     *     "MarkDeleted": true/false
     * }</pre>
     */
    public abstract JsonArray getPunishmentsForPlayer(UUID player);

    /**
     * Gets all notes associated with the given player UUID.
     * @param player The player's {@link UUID} used in Caesar.
     * @return A {@link JsonArray} containing all notes related to the player.
     * @apiNote The returned JsonArray should contain JsonObjects similar to this:
     * <pre>{@code
     * {
     *     "RecordID": "note-uuid",
     *     "PlayerID": "player-uuid",
     *     "UserID": "user-uuid",
     *     "Note": "note-text",
     *     "CreationDate": long
     * }
     */
    public abstract JsonArray getPlayerNotes(UUID player);

    /**
     * Creates a new note for the given player UUID.
     * @param player The {@link UUID} of the player to whom the note will be added.
     * @param user The {@link UUID} of the user creating the note.
     * @param note The content of the note as a {@link String}.
     */
    public abstract void createPlayerNote(UUID player, UUID user, String note);

    /**
     * Deletes a note for the given player UUID.
     * @param player The {@link UUID} of the player from whom the note will be deleted.
     * @param user The {@link UUID} of the user deleting the note.
     * @param note The {@link UUID} of the note to be deleted.
     * @apiNote For logging purposes, the user deleting the note is also provided.
     */
    public abstract void deletePlayerNote(UUID player, UUID user, UUID note);

    /**
     * Fetches the name of the given player by the {@link UUID} using {@link de.julianweinelt.caesar.endpoint.MinecraftUUIDFetcher#getByID(UUID)}.
     * Updates the fetched name in the database and returns the result of the operation as a {@link String}.
     * @param player The player's {@link UUID} used in Caesar.
     * @return A {@link String} containing the actual Minecraft name of the player.
     */
    public abstract String updateMCAccount(UUID player);

    /**
     * Creates a new process in the database.
     * @param type The {@link UUID} of the process type.
     * @param initialStatus The {@link UUID} of the initial process status (usually 'OPEN').
     * @param creator The {@link UUID} of the user creating the process.
     * @param comment An optional comment for the process as a {@link String}.
     * @return The {@link UUID} of the newly created process.
     */
    public abstract UUID createProcess(UUID type, UUID initialStatus, UUID creator, String comment);

    /**
     * Assigns a player to the given process in the database.
     * @param process The {@link UUID} of the process.
     * @param player The {@link UUID} of the player to assign to the process.
     */
    public abstract void assignPlayerToProcess(UUID process, UUID player);

    /**
     * Updates the status of the given process in the database.
     * @param process The {@link UUID} of the process.
     * @param status The new {@link UUID} of the process status to set.
     * @apiNote This method must only update the reference in database. Any further actions (like notifications) are handled elsewhere.
     */
    public abstract void updateProcessStatus(UUID process, UUID status);

    /**
     * Create a new process type in the database. Process types are used to categorize processes.
     * @param name The name of the process type as a {@link String}.
     * @param usePattern Whether to use a pattern for the name of the process type.
     * @param pattern The pattern to use for the name of the process type as a {@link String}. Can be null if {@code usePattern} is false.
     *                @apiNote Patterns can include placeholders like {player}, {date}, etc. to dynamically generate process names.
     */
    public abstract void createProcessType(String name, boolean usePattern, String pattern);

    /**
     * Create a new process status in the database. Process statuses are used to define the state of a process.
     * @param name The name of the process status as a {@link String}.
     * @param color The color of the process status as a {@link String}.
     *              The database format should be used by calling {@link DatabaseColorParser#parseColor(Color)}.
     * @param description The description of the process status as a {@link String}.
     */
    public abstract void createProcessStatus(String name, String color, String description);

    /**
     * Gets all process types from the database.
     * @return A {@link JsonArray} containing all process types.
     * @apiNote The returned JsonArray should contain JsonObjects similar to this:
     * <pre>{@code
     * {
     *     "id": "process-type-uuid",
     *     "name": "process-type-name",
     *     "active": true/false,
     *     "usePattern": true/false,
     *     "pattern": "process-name-pattern"
     * }
     * }</pre>
     */
    public abstract JsonArray getProcessTypes();

    /**
     * Gets all process statuses from the database.
     * @return A {@link JsonArray} containing all process statuses.
     * @apiNote The returned JsonArray should contain JsonObjects similar to this:
     * <pre>{@code
     * {
     *     "id": "process-status-uuid",
     *     "name": "process-status-name",
     *     "description": "process-status-description",
     *     "color": "255;255;255;100" // RGBA format
     * }
     * }</pre>
     */
    public abstract JsonArray getProcessStatuses();

    /**
     * Gets all permissions assigned to the user with the given UUID.
     * @param uuid The user's {@link UUID}.
     * @return A {@link List} of {@link String} representing the permissions assigned to the user.
     */
    public abstract List<String> getUserPermissions(UUID uuid);

    /**
     * Gets all Minecraft accounts linked to the given player UUID.
     * @param player The player's {@link UUID} used in Caesar.
     * @return A {@link JsonArray} containing all Minecraft account UUIDs linked to the player.
     * @apiNote The returned JsonArray should contain {@link JsonObject} fields like this:
     * <pre>{@code
     * {
     *     "name": "minecraft-username",
     *     "uuid": "minecraft-uuid"
     * }
     * }</pre>
     */
    public abstract JsonArray getMCAccounts(UUID player);

    /**
     * Gets the Discord ID mapped to the given user UUID.
     * @param user The user's {@link UUID}.
     * @return The Discord ID as a {@link String}, or {@code null} if no mapping exists.
     */
    @Nullable
    public abstract String getDiscordID(UUID user);
    /**
     * Gets the user UUID mapped to the given Discord ID.
     * @param discordID The Discord ID as a {@link String}.
     * @return The user's {@link UUID}, or {@code null} if no mapping exists.
     */
    @Nullable
    public abstract UUID getUserIDFromDiscordID(String discordID);

    /**
     * Maps a Discord ID to a user UUID in the database.
     * @param discord The Discord ID as a {@link String}.
     * @param user The user's {@link UUID}.
     */
    public abstract void mapUserDiscord(String discord, UUID user);
    /**
     * Removes the mapping of a Discord ID to a user UUID in the database.
     * @param user The user's {@link UUID}.
     */
    public abstract void removeMappingDCUser(UUID user);
}