package de.julianweinelt.caesar.storage.providers;

import de.julianweinelt.caesar.Caesar;
import de.julianweinelt.caesar.auth.CPermission;
import de.julianweinelt.caesar.auth.User;
import de.julianweinelt.caesar.auth.UserManager;
import de.julianweinelt.caesar.auth.UserRole;
import de.julianweinelt.caesar.discord.ticket.Ticket;
import de.julianweinelt.caesar.discord.ticket.TicketManager;
import de.julianweinelt.caesar.discord.ticket.TicketStatus;
import de.julianweinelt.caesar.discord.ticket.TicketType;
import de.julianweinelt.caesar.exceptions.TicketSystemNotUsedException;
import de.julianweinelt.caesar.storage.DatabaseVersionManager;
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


//TODO: Add database downtime detection
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

            Caesar.getInstance().setDbVersionManager(new DatabaseVersionManager());

            String[] requiredTables = {
                    "users",
                    "permissions",
                    "roles",
                    "ticket_status_names",
                    "process_status_names",
                    "process_types",
                    "user_permissions",
                    "user_roles",
                    "role_permissions",
                    "tickets",
                    "ticket_types",
                    "processes",
                    "ticket_transcripts",
                    "server_data",
                    "punishments",
                    "punishment_types",
                    "user_types",
                    "reports",
                    "reports_update_history",
                    "reports_types",
                    "reports_status",
            };

            createTables();
            if (!systemDataExist()) insertDefaultData();
            log.info("Loading data into memory... This may take a while. Please wait...");
            UserManager.getInstance().getAllPermissions();
            UserManager.getInstance().getAllRoles();
            UserManager.getInstance().overrideUsers(getAllUsers());
            TicketManager.execute(manager -> manager.startUp(getAllTicketStatuses(), getAllTicketTypes()));
            return true;
        } catch (Exception e) {
            log.error("Failed to connect to MySQL database: {}", e.getMessage());
        }
        return false;
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
        try {
            PreparedStatement pS = conn.prepareStatement("SELECT * FROM permissions");
            ResultSet set = pS.executeQuery();
            int i = 0;
            while (set.next()) i++;
            return i != 0;
        } catch (SQLException e) {
            log.error("Failed to check system storage data: {}", e.getMessage());
            return false;
        }
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
        User user = null;
        try {
            PreparedStatement pS = conn.prepareStatement("SELECT * FROM users WHERE Username = ?");
            pS.setString(1, username);
            ResultSet set = pS.executeQuery();
            if (set.next()) {
                user =  new User(UUID.fromString(set.getString(1)));
            }
        } catch (SQLException e) {
            log.error("Failed to get user: {}", e.getMessage());
        }
        if (user == null) return null;
        try {
            PreparedStatement pS = conn.prepareStatement("SELECT * FROM user_permissions WHERE UserID = ?");
            pS.setString(1, user.getUuid().toString());
            ResultSet set = pS.executeQuery();
            while (set.next()) {
                CPermission p = UserManager.getInstance().getPermission(UUID.fromString(set.getString(2)));
                if (p != null) user.addPermission(p.permissionKey());
            }
        } catch (SQLException e) {
            log.error("Failed to get user permissions: {}", e.getMessage());
        }
        try {
            PreparedStatement pS = conn.prepareStatement("SELECT * FROM user_roles WHERE UserID = ?");
            pS.setString(1, user.getUuid().toString());
            ResultSet set = pS.executeQuery();
            while (set.next()) {
                UserRole r = UserManager.getInstance().getRole(UUID.fromString(set.getString(2)));
                if (r != null) user.addRole(r);
            }
        } catch (SQLException e) {
            log.error("Failed to get user roles: {}", e.getMessage());
        }
        return user;
    }

    @Override
    public void deleteUser(String username) {
        User user = UserManager.getInstance().getUser(username);
        try {
            PreparedStatement pS = conn.prepareStatement("DELETE FROM users WHERE Username = ?");

            pS.setString(1, username);
            pS.execute();
            log.info("Deleted user: {}. Performing clean-up now...", username);
        } catch (SQLException e) {
            log.error("Failed to delete user: {}", e.getMessage());
            return;
        }
        try {
            PreparedStatement pS = conn.prepareStatement("DELETE FROM user_roles WHERE UserID = ?");
            pS.setString(1, user.getUuid().toString());
            pS.execute();
        } catch (SQLException e) {
            log.error("Failed to delete user roles: {}", e.getMessage());
            return;
        }
        try {
            PreparedStatement pS = conn.prepareStatement("DELETE FROM user_permissions WHERE UserID = ?");
            pS.setString(1, user.getUuid().toString());
            pS.execute();
        } catch (SQLException e) {
            log.error("Failed to delete user permissions: {}", e.getMessage());
            return;
        }
        log.info("Clean-up finished. User {} and all references deleted.", username);
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

        try {
            conn.setAutoCommit(false);
            PreparedStatement pS = conn.prepareStatement("INSERT INTO user_permissions " +
                    "(UserID, PermissionID) VALUES (?, ?) ON DUPLICATE KEY UPDATE PermissionID = PermissionID");
            for (String perm : user.getPermissions()) {
                CPermission permission = UserManager.getInstance().getPermission(perm);
                if (permission == null) continue;
                pS.setString(1, user.getUuid().toString());
                pS.setString(2, permission.uniqueID().toString());
                pS.addBatch();
            }
            pS.executeBatch();
        } catch (SQLException e) {
            log.error("Failed to update user permissions: {}", e.getMessage());
        }
        try {
            conn.setAutoCommit(false);
            PreparedStatement pS = conn.prepareStatement("INSERT INTO user_roles (UserID, RoleID) VALUES (?, ?)" +
                    " ON DUPLICATE KEY UPDATE RoleID = RoleID");
            for (UserRole role : user.getRoles()) {
                pS.setString(1, user.getUuid().toString());
                pS.setString(2, role.getUniqueID().toString());
                pS.addBatch();
            }
            pS.executeBatch();
        } catch (SQLException e) {
            log.error("Failed to update user roles: {}", e.getMessage());
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

    @Override
    public void addRole(UserRole role) {
        try {
            PreparedStatement pS = conn.prepareStatement("INSERT INTO roles (UUID, NameKey, DisplayColor, " +
                    "CreationDate) VALUES (?, ?, ?, CURRENT_TIMESTAMP)");
            pS.setString(1, UUID.randomUUID().toString());
            pS.setString(2, role.getName());
            pS.setString(3, role.getColor());
            pS.execute();
        } catch (SQLException e) {
            log.error("Failed to add role: {}", e.getMessage());
        }
    }

    @Override
    public void removeRole(UserRole role) {

    }

    @Override
    public List<UserRole> getAllRoles() {
        List<UserRole> roles = new ArrayList<>();
        try {
            PreparedStatement pS = conn.prepareStatement("SELECT * FROM roles");
            ResultSet set = pS.executeQuery();
            while (set.next()) {
                UserRole role = new UserRole(set.getString(2), set.getString(3),
                        UUID.fromString(set.getString(1)));
                PreparedStatement p = conn.prepareStatement("SELECT PermissionID FROM role_permissions WHERE RoleID = ?");
                p.setString(1, role.getUniqueID().toString());
                ResultSet s = p.executeQuery();
                while (s.next()) {
                    role.addPermission(
                            UserManager.getInstance().getPermission(UUID.fromString(s.getString(1)))
                                    .permissionKey()
                    );
                }
                s.close();
                p.close();
                roles.add(role);
            }
            set.close();
            pS.close();
        } catch (SQLException e) {
            log.error("Failed to get all roles: {}", e.getMessage());
        }
        return roles;
    }

    @Override
    public void updateRolePermissions(UserRole role) {
        try {
            conn.setAutoCommit(false);
            PreparedStatement pS = conn.prepareStatement("INSERT INTO role_permissions (RoleID, PermissionID) " +
                    "VALUES (?, ?) ON DUPLICATE KEY UPDATE PermissionID = PermissionID");
            for (String p : role.getPermissions()) {
                UUID u = UserManager.getInstance().getPermissionID(p);
                pS.setString(1, u.toString());
                pS.setString(2, role.getUniqueID().toString());
                pS.addBatch();
            }
            pS.executeBatch();
        } catch (SQLException e) {
            log.error("Failed to update role permissions: {}", e.getMessage());
        }
    }

    @Override
    public List<CPermission> getAllPermissions() {
        List<CPermission> permissions = new ArrayList<>();
        try {
            ResultSet set = conn.createStatement().executeQuery("SELECT * FROM permissions");
            while (set.next()) {
                CPermission p = new CPermission(UUID.fromString(set.getString(1)),
                        set.getString(3));
                permissions.add(p);
            }
        } catch (SQLException e) {
            log.error("Failed to get all permissions: {}", e.getMessage());
        }
        return permissions;
    }

    @Override
    public Ticket getTicket(UUID id) {
        try {
            PreparedStatement pS = conn.prepareStatement("SELECT UUID, CreatedBy, HandledBy, CreationDate," +
                    " TicketStatus, TicketType, ChannelID" +
                    " FROM tickets WHERE UUID = ?");

            pS.setString(1, id.toString());

            ResultSet set = pS.executeQuery();
            if (set.next()) {
                TicketStatus status;
                TicketType type;
                status = TicketManager.getInstance().getTicketStatus(UUID.fromString(set.getString("TicketStatus")));
                type = TicketManager.getInstance().getTicketType(UUID.fromString(set.getString("TicketStatus")));



                return new Ticket(id, set.getString("CreatedBy"),
                        set.getString("HandledBy"), set.getString("ChannelID"), status, type);
            }
        } catch (SQLException e) {
            log.error(e.getMessage());
        } catch (TicketSystemNotUsedException ignored) {}
        return null;
    }

    @Override
    public Ticket getTicket(String channel) {
        try {
            PreparedStatement pS = conn.prepareStatement("SELECT UUID, CreatedBy, HandledBy, CreationDate," +
                    " TicketStatus, TicketType, ChannelID" +
                    " FROM tickets WHERE ChannelID = ?");

            pS.setString(1, channel);

            ResultSet set = pS.executeQuery();
            if (set.next()) {
                TicketStatus status;
                TicketType type;
                status = TicketManager.getInstance().getTicketStatus(UUID.fromString(set.getString("TicketStatus")));
                type = TicketManager.getInstance().getTicketType(UUID.fromString(set.getString("TicketStatus")));



                return new Ticket(UUID.fromString(set.getString("UUID")), set.getString("CreatedBy"),
                        set.getString("HandledBy"), channel, status, type);
            }
        } catch (SQLException e) {
            log.error(e.getMessage());
        } catch (TicketSystemNotUsedException ignored) {}
        return null;
    }

    @Override
    public List<TicketType> getAllTicketTypes() {
        List<TicketType> ticketTypes = new ArrayList<>();
        try {
            PreparedStatement pS = conn.prepareStatement("SELECT TypeID, TypeName, Prefix, ShowInSelection, " +
                    "SelectionEmoji, SelectionText FROM ticket_types");

            ResultSet set = pS.executeQuery();
            while (set.next()) {
                ticketTypes.add(new TicketType(UUID.fromString(set.getString(1)),
                        set.getString(2), set.getString(3), set.getBoolean(4),
                        set.getString(5), set.getString(6)));
            }
        } catch (SQLException e) {
            log.error(e.getMessage());
            return List.of();
        }
        return ticketTypes;
    }

    @Override
    public List<TicketStatus> getAllTicketStatuses() {
        List<TicketStatus> statuses = new ArrayList<>();
        try {
            PreparedStatement pS = conn.prepareStatement("SELECT UUID, StatusName, Color, Description" +
                    " FROM ticket_status_names");

            ResultSet set = pS.executeQuery();
            while (set.next()) {
                statuses.add(new TicketStatus(UUID.fromString(set.getString(1)), set.getString(2),
                        set.getString(4), DatabaseColorParser.parseColor(set.getString(3))));
            }
        } catch (SQLException e) {
            log.error(e.getMessage());
            return List.of();
        }
        return statuses;
    }

    @Override
    public void addTicketType(TicketType ticketType) {
        try {
            PreparedStatement pS = conn.prepareStatement("INSERT INTO ticket_types " +
                    "(TypeID, TypeName, Prefix, ShowInSelection, SelectionEmoji, SelectionText) " +
                    "VALUES (?, ?, ?, ?, ?, ?)");

            pS.setString(1, ticketType.uniqueID().toString());
            pS.setString(2, ticketType.name());
            pS.setString(3, ticketType.prefix());
            pS.setBoolean(4, ticketType.showInSel());
            pS.setString(5, ticketType.selEmoji());
            pS.setString(6, ticketType.selText());
            pS.execute();
        } catch (SQLException e) {
            log.error("Failed to add ticket type: {}", e.getMessage());
        }
    }

    @Override
    public void deleteTicketType(TicketType ticketType) {
        try {
            PreparedStatement pS = conn.prepareStatement("DELETE FROM ticket_types WHERE TypeID = ?");
            pS.setString(1, ticketType.uniqueID().toString());
            pS.execute();
        } catch (SQLException e) {
            log.error("Failed to delete ticket type: {}", e.getMessage());
        }
    }

    @Override
    public void addTicketStatus(TicketStatus ticketStatus) {
        try {
            PreparedStatement pS = conn.prepareStatement("INSERT INTO ticket_status_names" +
                    " (UUID, StatusName, Color, Description) VALUES (?, ?, ?, ?)");
            pS.setString(1, ticketStatus.uniqueID().toString());
            pS.setString(2, ticketStatus.statusName());
            pS.setString(3, DatabaseColorParser.parseColor(ticketStatus.statusColor()));
            pS.setString(4, ticketStatus.statusDescription());
            pS.execute();
        } catch (SQLException e) {
            log.error("Failed to add ticket status: {}", e.getMessage());
        }
    }

    @Override
    public void deleteTicketStatus(TicketStatus ticketStatus) {
        try {
            PreparedStatement pS = conn.prepareStatement("DELETE FROM ticket_status_names WHERE UUID = ?");
            pS.setString(1, ticketStatus.uniqueID().toString());
            pS.execute();
        } catch (SQLException e) {
            log.error("Failed to delete ticket status: {}", e.getMessage());
        }
    }

    @Override
    public void addTicketMessage(Ticket ticket, String message, String sender) {
        try {
            PreparedStatement pS = conn.prepareStatement("INSERT INTO ticket_transcripts " +
                    "(TicketID, SenderName, MessageContent) VALUES (?, ?, ?)");
            pS.setString(1, ticket.getUniqueID().toString());
            pS.setString(2, sender);
            pS.setString(3, message);
            pS.execute();
        } catch (SQLException e) {
            log.error("Failed to add ticket message: {}", e.getMessage());
        }
    }

    @Override
    public void updateTicketStatus(Ticket ticket, TicketStatus ticketStatus) {
        try {
            PreparedStatement pS = conn.prepareStatement("UPDATE tickets SET TicketStatus = ? WHERE UUID = ?");

            pS.setString(1, ticketStatus.uniqueID().toString());
            pS.setString(2, ticket.getUniqueID().toString());

            pS.execute();
        } catch (SQLException e) {
            log.error("Failed to update ticket status: {}", e.getMessage());
        }
    }

    @Override
    public void handleTicket(Ticket ticket, String handler) {
        try {
            PreparedStatement pS = conn.prepareStatement("UPDATE tickets SET HandledBy = ? WHERE UUID = ?");

            pS.setString(1, handler);
            pS.setString(2, ticket.getUniqueID().toString());

            pS.execute();
        } catch (SQLException e) {
            log.error("Failed to update ticket handler: {}", e.getMessage());
        }
    }

    @Override
    public void deleteTicket(Ticket ticket) {
        try {
            PreparedStatement pS = conn.prepareStatement("DELETE FROM tickets WHERE UUID = ?");
            pS.setString(1, ticket.getUniqueID().toString());
            pS.execute();
        } catch (SQLException e) {
            log.error("Failed to delete ticket: {}", e.getMessage());
        }
    }

    @Override
    public void createTicket(Ticket ticket) {

    }
}