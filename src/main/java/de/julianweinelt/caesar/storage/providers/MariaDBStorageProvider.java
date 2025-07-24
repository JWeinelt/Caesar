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
import de.julianweinelt.caesar.storage.StorageHelperInitializer;
import de.julianweinelt.caesar.util.DatabaseColorParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MariaDBStorageProvider extends Storage {
    private static final Logger log = LoggerFactory.getLogger(MariaDBStorageProvider.class);

    public MariaDBStorageProvider(String host, int port, String database, String user, String password) {
        super(StorageFactory.StorageType.Mariadb, host, port, database, user, password);
    }

    @Override
    public boolean connect() {
        final String DRIVER = "org.mariadb.jdbc.Driver";
        final String PARAMETERS = "?useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC";
        final String URL = "jdbc:mariadb://" + getHost() + ":" + getPort() + "/" + getDatabase() + PARAMETERS;
        final String USER = getUser();
        final String PASSWORD = getPassword();

        try {
            Class.forName(DRIVER);

            conn = DriverManager.getConnection(URL, USER, PASSWORD);
            return true;
        } catch (Exception e) {
            log.error("Failed to connect to MariaDB database: {}", e.getMessage());
        }
        return false;
    }

    @Override
    public void disconnect() {
        try {
            conn.close();
        } catch (SQLException e) {
            log.error("Failed to disconnect from MySQL database: {}", e.getMessage());
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
    public boolean allTablesExist(String[] tables) {
        checkConnection();
        try {
            DatabaseMetaData meta = conn.getMetaData();
            for (String table : tables) {
                try (ResultSet rs = meta.getTables(null, null, table, new String[]{"TABLE"})) {
                    if (!rs.next()) {
                        log.warn("Table '{}' does not exist!", table);
                        return false;
                    }
                }
            }
            return true;
        } catch (SQLException e) {
            log.error("Error while checking database: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean systemDataExist() {
        return false;
    }

    @Override
    public void createTables() {
        try {
            conn.setAutoCommit(false);

            String users = "create table if not exists users" +
                    "(" +
                    "    UUID                varchar(36)       not null" +
                    "        primary key," +
                    "    Username            varchar(20)       not null," +
                    "    PasswordHashed      int               not null," +
                    "    CreationDate        datetime          null," +
                    "    Active              tinyint default 1 not null," +
                    "    NewlyCreated        tinyint default 1 not null," +
                    "    ApplyPasswordPolicy tinyint default 0 not null," +
                    "    constraint users_pk_2" +
                    "        unique (Username)" +
                    ");";
            Statement statement = conn.createStatement();
            String permissions = "create table if not exists permissions" +
                    "(" +
                    "    UUID           varchar(36) not null" +
                    "        primary key," +
                    "    NameKey        varchar(60) not null," +
                    "    PermissionKey  varchar(60) not null," +
                    "    DefaultGranted tinyint     null," +
                    "    constraint permissions_pk_2" +
                    "        unique (NameKey)," +
                    "    constraint permissions_pk_3" +
                    "        unique (PermissionKey)" +
                    ");";
            String userPermissions = "create table if not exists user_permissions" +
                    "(" +
                    "    UserID       varchar(36) not null," +
                    "    PermissionID varchar(36) not null," +
                    "    constraint user_permissions__perm_fk" +
                    "        foreign key (PermissionID) references permissions (UUID)," +
                    "    constraint user_permissions_users_UUID_fk" +
                    "        foreign key (UserID) references users (UUID)" +
                    ");";
            String roles = "create table if not exists roles" +
                    "(" +
                    "    UUID         varchar(36)                           not null" +
                    "        primary key," +
                    "    NameKey      varchar(60)                           null," +
                    "    DisplayColor varchar(16) default '0;0;0;100'       not null," +
                    "    CreationDate datetime    default CURRENT_TIMESTAMP not null" +
                    ");";
            String userRoles = "create table if not exists user_roles" +
                    "(" +
                    "    UserID varchar(36) not null," +
                    "    RoleID varchar(36) not null," +
                    "    constraint user_roles_roles_role_fk" +
                    "        foreign key (RoleID) references roles (UUID)," +
                    "    constraint user_roles_users_user_fk" +
                    "        foreign key (UserID) references users (UUID)" +
                    ");";
            String rolePermissions = "create table if not exists role_permissions" +
                    "(" +
                    "    RoleID       varchar(36) not null," +
                    "    PermissionID varchar(36) not null," +
                    "    constraint role_permissions_permissions_id_fk" +
                    "        foreign key (PermissionID) references permissions (UUID)," +
                    "    constraint role_permissions_roles_id_fk" +
                    "        foreign key (RoleID) references roles (UUID)" +
                    ");";

            String processStatusNames = "create table if not exists process_status_names" +
                    "(" +
                    "    UUID        varchar(36)                     not null" +
                    "        primary key," +
                    "    StatusName  varchar(36)                     not null," +
                    "    Color       varchar(16) default '0;0;0;100' not null," +
                    "    Description varchar(150)                    null," +
                    "    constraint process_status_names_pk_2" +
                    "        unique (StatusName)" +
                    ");";
            String ticketStatusNames = "create table if not exists ticket_status_names" +
                    "(" +
                    "    UUID        varchar(36)                     not null" +
                    "        primary key," +
                    "    StatusName  varchar(36)                     not null," +
                    "    Color       varchar(16) default '0;0;0;100' not null," +
                    "    Description varchar(150)                    null," +
                    "    constraint ticket_status_names_pk_2" +
                    "        unique (StatusName)" +
                    ");";
            String processTypes = "create table if not exists process_types" +
                    "(" +
                    "    TypeID      varchar(36)       not null" +
                    "        primary key," +
                    "    TypeName    varchar(30)       not null," +
                    "    Active      tinyint default 1 not null," +
                    "    UsePattern  tinyint default 0 not null," +
                    "    PatternUsed varchar(36)       null" +
                    ");";
            String processes = "create table if not exists processes" +
                    "(" +
                    "    ProcessID    varchar(36)                                not null " +
                    "        primary key," +
                    "    CreatedBy    varchar(36)                                not null," +
                    "    Status       varchar(36)                                not null," +
                    "    ProcessType  varchar(36)                                not null," +
                    "    CreationDate datetime     default current_timestamp()   not null," +
                    "    Comment      varchar(150) default 'Nothing to see here' not null," +
                    "    constraint processes_process_status_names_UUID_fk" +
                    "        foreign key (Status) references process_status_names (UUID)," +
                    "    constraint processes_process_types_TypeID_fk" +
                    "        foreign key (ProcessType) references process_types (TypeID)," +
                    "    constraint processes_users_UUID_fk" +
                    "        foreign key (CreatedBy) references users (UUID)" +
                    ");";
            String tickets = "create table if not exists tickets" +
                    "(" +
                    "    UUID         varchar(36) not null" +
                    "        primary key," +
                    "    CreatedBy    varchar(140) null," +
                    "    HandledBy    varchar(140) null," +
                    "    CreationDate datetime     null," +
                    "    TicketStatus varchar(36)  not null," +
                    "    constraint tickets_ticket_status_names_UUID_fk" +
                    "        foreign key (TicketStatus) references ticket_status_names (UUID)" +
                    ");";
            String ticketTranscripts = "create table if not exists ticket_transcripts" +
                    "(" +
                    "    TicketID       varchar(36)                        not null," +
                    "    SenderName     varchar(50)                        not null," +
                    "    MessageContent varchar(5000)                      null," +
                    "    SentDate       datetime default CURRENT_TIMESTAMP not null," +
                    "    constraint ticket_transcripts_tickets_UUID_fk" +
                    "        foreign key (TicketID) references tickets (UUID)" +
                    ");";
            String serverData = "create table if not exists server_data" +
                    "(" +
                    "    UUID      varchar(36)                        not null," +
                    "    Name      varchar(80)                        not null," +
                    "    TimeStamp datetime default CURRENT_TIMESTAMP not null," +
                    "    Players   int      default 0                 not null," +
                    "    cpu       float                              not null," +
                    "    memory    int                                not null," +
                    "    TPS       int      default 20                not null" +
                    ");";

            statement.executeUpdate(users);
            statement.executeUpdate(permissions);
            statement.executeUpdate(roles);
            statement.executeUpdate(ticketStatusNames);
            statement.executeUpdate(processStatusNames);
            statement.executeUpdate(processTypes);

            statement.executeUpdate(userPermissions);
            statement.executeUpdate(userRoles);
            statement.executeUpdate(rolePermissions);

            statement.executeUpdate(tickets);
            statement.executeUpdate(processes);
            statement.executeUpdate(ticketTranscripts);
            statement.executeUpdate(serverData);
        } catch (SQLException e) {
            log.error("Failed to create tables: {}", e.getMessage());
        }
    }
    @Override
    public void insertDefaultData() {
        try {
            log.info("Creating default permission data...");
            PreparedStatement permissionPS = conn.prepareStatement("INSERT INTO permissions " +
                    "(UUID, NameKey, PermissionKey, DefaultGranted) VALUES (?, ?, ?, ?)");
            for (String p : StorageHelperInitializer.PERMISSIONS) {
                permissionPS.setString(1, UUID.randomUUID().toString());
                permissionPS.setString(2, "permissions." + p);
                permissionPS.setString(3, p);
                permissionPS.setBoolean(4, false);
                permissionPS.addBatch();
            }
            permissionPS.executeBatch();

            log.info("Creating default ticket status names...");
            PreparedStatement ticketStatusPS = conn.prepareStatement("INSERT INTO ticket_status_names " +
                    "(UUID, StatusName, Color, Description) VALUES (?, ?, ?, ?)");
            for (TicketStatus s : StorageHelperInitializer.getDefaultTicketStatusList()) {
                ticketStatusPS.setString(1, s.uniqueID().toString());
                ticketStatusPS.setString(2, s.statusName());
                ticketStatusPS.setString(3, DatabaseColorParser.parseColor(s.statusColor()));
                ticketStatusPS.setString(4, s.statusDescription());
                ticketStatusPS.addBatch();
            }
            ticketStatusPS.executeBatch();


            log.info("Creating default user roles...");
            PreparedStatement pSRoles = conn.prepareStatement("INSERT INTO roles (UUID, NameKey, DisplayColor)" +
                    " VALUES (?, ?, ?)");
            pSRoles.setString(1, UUID.randomUUID().toString());
            pSRoles.setString(2, "admin");
            pSRoles.setString(3, DatabaseColorParser.parseColor(new Color(71, 130, 195,100)));
            pSRoles.addBatch();
            pSRoles.setString(1, UUID.randomUUID().toString());
            pSRoles.setString(2, "user");
            pSRoles.setString(3, DatabaseColorParser.parseColor(new Color(255, 255, 255,100)));
            pSRoles.executeBatch();
        } catch (SQLException e) {
            log.error("Failed to insert default data: {}", e.getMessage());
        }
    }

    @Override
    public boolean hasTables() {
        try {
            Statement statement = conn.createStatement();
            ResultSet set = statement.executeQuery("SHOW TABLES;");
            int tables = 0;
            while (set.next()) tables++;
            return tables != 0;
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public User getUser(String username) {
        try {
            PreparedStatement pS = conn.prepareStatement("SELECT * FROM users WHERE Username = ?");
            pS.setString(1, username);
            ResultSet set = pS.executeQuery();
            if (set.next()) {
                return new User(UUID.fromString(set.getString(1)));
            }
        } catch (SQLException e) {
            log.error("Failed to get user: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public void deleteUser(String username) {
        try {
            PreparedStatement pS = conn.prepareStatement("DELETE FROM users WHERE Username = ?");

            pS.setString(1, username);
            pS.execute();
        } catch (SQLException e) {
            log.error("Failed to delete user: {}", e.getMessage());
        }
    }

    @Override
    public void updateUser(User user) {
        User existing = getUser(user.getUsername());

        StringBuilder sql = new StringBuilder("UPDATE users SET ");

        List<String> fields = new ArrayList<>();
        List<Object> values = new ArrayList<>();

        if (!Objects.equals(user.getUsername(), existing.getUsername())) {
            fields.add("Username = ?");
            values.add(user.getUsername());
        }

        if (!Objects.equals(user.getPassword(), existing.getPassword())) {
            fields.add("PasswordHashed = ?");
            values.add(user.getPassword());
        }

        if (!Objects.equals(user.isActive(), existing.isActive())) {
            fields.add("Active = ?");
            values.add(user.isActive());
        }

        if (!Objects.equals(user.isNewlyCreated(), existing.isNewlyCreated())) {
            fields.add("NewlyCreated = ?");
            values.add(user.isNewlyCreated());
        }

        if (!Objects.equals(user.isApplyPasswordPolicy(), existing.isApplyPasswordPolicy())) {
            fields.add("ApplyPasswordPolicy = ?");
            values.add(user.isApplyPasswordPolicy());
        }

        if (fields.isEmpty()) {
            return; // Nichts zu ändern
        }

        sql.append(String.join(", ", fields));
        sql.append(" WHERE UUID = ?");
        values.add(user.getUuid().toString());

        try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < values.size(); i++) {
                stmt.setObject(i + 1, values.get(i));
            }

            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to update user: {}", e.getMessage());
        }
    }

    @Override
    public void createUser(User user) {
        try {
            PreparedStatement pS = conn.prepareStatement("INSERT INTO users (UUID, Username, PasswordHashed, " +
                    "CreationDate) VALUES (?, ?, ?, CURRENT_TIMESTAMP)");
            pS.setString(1, user.getUuid().toString());
            pS.setString(2, user.getUsername());
            pS.setInt(3, user.getPassword());
            int result = pS.executeUpdate();
            if (result == 0) {
                log.error("Failed to create user: {}", user.getUsername());
            }
        } catch (SQLException e) {
            log.error("Failed to create user: {}", e.getMessage());
        }
    }

    @Override
    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        try {
            ResultSet set = conn.createStatement().executeQuery("SELECT * FROM users");
            while (set.next()) {
                User user = new User(
                        UUID.fromString(set.getString(1)),
                        set.getString(2), set.getInt(3), ""
                );
                users.add(user);
            }
        } catch (SQLException e) {
            log.error("Failed to get all users: {}", e.getMessage());
        }

        return users;
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
