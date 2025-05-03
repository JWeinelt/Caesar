package de.julianweinelt.caesar.storage.providers;

import de.julianweinelt.caesar.auth.User;
import de.julianweinelt.caesar.auth.UserManager;
import de.julianweinelt.caesar.endpoint.wrapper.TicketStatus;
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

public class MySQLStorageProvider extends Storage {
    private static final Logger log = LoggerFactory.getLogger(MySQLStorageProvider.class);

    public MySQLStorageProvider(String host, int port, String database, String user, String password) {
        super(StorageFactory.StorageType.MYSQL, host, port, database, user, password);
    }

    @Override
    public boolean connect() {
        final String DRIVER = "com.mysql.cj.jdbc.Driver";
        final String PARAMETERS = "?useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC";
        final String URL = "jdbc:mysql://" + getHost() + ":" + getPort() + "/" + getDatabase() + PARAMETERS;
        final String USER = getUser();
        final String PASSWORD = getPassword();

        try {
            Class.forName(DRIVER);

            conn = DriverManager.getConnection(URL, USER, PASSWORD);
            log.info("Connected to MySQL database: {}", URL);
            conn.createStatement().execute("USE " + getDatabase());
            return true;
        } catch (Exception e) {
            log.error("Failed to connect to MySQL database: {}", e.getMessage());
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
    public void checkConnection() {
        try {
            if (conn == null || conn.isClosed()) connect();
        } catch (SQLException e) {
            log.error("Failed to check connection: {}", e.getMessage());
        }
    }

    @Override
    public void createTables() {
        try {
            conn.setAutoCommit(false);

            String users = "create table users" +
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
            statement.addBatch(users);
            String permissions = "create table permissions" +
                    "(" +
                    "    UUID           varchar(36) null" +
                    "        primary key," +
                    "    NameKey        varchar(60) not null," +
                    "    PermissionKey  varchar(60) not null," +
                    "    DefaultGranted tinyint     null," +
                    "    constraint permissions_pk_2" +
                    "        unique (NameKey)," +
                    "    constraint permissions_pk_3" +
                    "        unique (PermissionKey)" +
                    ");";
            statement.addBatch(permissions);
            String userPermissions = "create table user_permissions" +
                    "(" +
                    "    UserID       varchar(36) null," +
                    "    PermissionID varchar(36) null," +
                    "    constraint user_permissions__perm_fk" +
                    "        foreign key (PermissionID) references permissions (UUID)," +
                    "    constraint user_permissions_users_UUID_fk" +
                    "        foreign key (UserID) references users (UUID)" +
                    ");";
            statement.addBatch(userPermissions);
            String roles = "create table roles" +
                    "(" +
                    "    UUID         varchar(36)                           null" +
                    "        primary key," +
                    "    NameKey      varchar(60)                           null," +
                    "    DisplayColor varchar(16) default '0;0;0;100'       not null," +
                    "    CreationDate datetime    default CURRENT_TIMESTAMP not null" +
                    ");";
            statement.addBatch(roles);
            String userRoles = "create table user_roles" +
                    "(" +
                    "    UserID varchar(36) null," +
                    "    RoleID varchar(36) null," +
                    "    constraint user_roles_roles_role_fk" +
                    "        foreign key (RoleID) references roles (UUID)," +
                    "    constraint user_roles_users_user_fk" +
                    "        foreign key (UserID) references users (UUID)" +
                    ");";
            statement.addBatch(userRoles);
            String rolePermissions = "create table role_permissions" +
                    "(" +
                    "    RoleID       varchar(36) null," +
                    "    PermissionID varchar(36) null," +
                    "    constraint role_permissions_permissions_id_fk" +
                    "        foreign key (PermissionID) references permissions (UUID)," +
                    "    constraint role_permissions_roles_id_fk" +
                    "        foreign key (RoleID) references roles (UUID)" +
                    ");";
            statement.addBatch(rolePermissions);
            
            String processStatusNames = "create table process_status_names" +
                    "(" +
                    "    UUID        varchar(36)                     not null" +
                    "        primary key," +
                    "    StatusName  varchar(36)                     not null," +
                    "    Color       varchar(16) default '0;0;0;100' not null," +
                    "    Description varchar(150)                    null," +
                    "    constraint process_status_names_pk_2" +
                    "        unique (StatusName)" +
                    ");";
            statement.addBatch(processStatusNames);
            String ticketStatusNames = "create table ticket_status_names" +
                    "(" +
                    "    UUID        varchar(36)                     not null" +
                    "        primary key," +
                    "    StatusName  varchar(36)                     not null," +
                    "    Color       varchar(16) default '0;0;0;100' not null," +
                    "    Description varchar(150)                    null," +
                    "    constraint ticket_status_names_pk_2" +
                    "        unique (StatusName)" +
                    ");";
            String processTypes = "create table process_types" +
                    "(" +
                    "    TypeID      varchar(36)       not null" +
                    "        primary key," +
                    "    TypeName    varchar(30)       not null," +
                    "    Active      tinyint default 1 not null," +
                    "    UsePattern  tinyint default 0 not null," +
                    "    PatternUsed varchar(36)       null" +
                    ");";
            statement.addBatch(processTypes);
            statement.addBatch(ticketStatusNames);
            String processes = "create table processes" +
                    "(" +
                    "    ProcessID    varchar(36)                                not null " +
                    "        primary key," +
                    "    CreatedBy    varchar(36)                                null," +
                    "    Status       varchar(36)                                null," +
                    "    ProcessType  varchar(36)                                null," +
                    "    CreationDate datetime     default current_timestamp()   not null," +
                    "    Comment      varchar(150) default 'Nothing to see here' not null," +
                    "    constraint processes_process_status_names_UUID_fk" +
                    "        foreign key (Status) references process_status_names (UUID)," +
                    "    constraint processes_process_types_TypeID_fk" +
                    "        foreign key (ProcessType) references process_types (TypeID)," +
                    "    constraint processes_users_UUID_fk" +
                    "        foreign key (CreatedBy) references users (UUID)" +
                    ");";
            statement.addBatch(processes);
            String tickets = "create table tickets" +
                    "(" +
                    "    UUID         varchar(36)  null" +
                    "        primary key," +
                    "    CreatedBy    varchar(140) null," +
                    "    HandledBy    varchar(140) null," +
                    "    CreationDate datetime     null," +
                    "    TicketStatus varchar(36)  null," +
                    "    constraint tickets_ticket_status_names_UUID_fk" +
                    "        foreign key (TicketStatus) references ticket_status_names (UUID)" +
                    ");";
            statement.addBatch(tickets);
            String ticketTranscripts = "create table ticket_transcripts" +
                    "(" +
                    "    TicketID       varchar(36)                        null," +
                    "    SenderName     varchar(50)                        not null," +
                    "    MessageContent varchar(5000)                      null," +
                    "    SentDate       datetime default CURRENT_TIMESTAMP not null," +
                    "    constraint ticket_transcripts_tickets_UUID_fk" +
                    "        foreign key (TicketID) references tickets (UUID)" +
                    ");";
            statement.addBatch(ticketTranscripts);

            statement.executeBatch();
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
                log.error("Failed to create user (db): {}", user.getUsername());
            } else log.info("Created user: {}", user.getUsername());
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
                user.setActive(set.getBoolean(5));
                user.setNewlyCreated(set.getBoolean(6));
                user.setApplyPasswordPolicy(set.getBoolean(7));
                users.add(user);
            }
        } catch (SQLException e) {
            log.error("Failed to get all users: {}", e.getMessage());
        }

        if (users.isEmpty()) {
            log.warn("No users found in database!");
            log.warn("That doesn't look correct. We will create a default user for you...");
            UserManager.getInstance().createUser("admin", "admin");
        }

        return users;
    }
}