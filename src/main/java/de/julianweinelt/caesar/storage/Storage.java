package de.julianweinelt.caesar.storage;

import de.julianweinelt.caesar.auth.CPermission;
import de.julianweinelt.caesar.auth.User;
import de.julianweinelt.caesar.auth.UserRole;
import lombok.Getter;

import java.sql.Connection;
import java.util.List;

@Getter
public abstract class Storage {
    private final StorageFactory.StorageType type;

    private final String host;
    private final int port;
    private final String database;
    private final String user;
    private final String password;

    public Connection conn;

    protected Storage(StorageFactory.StorageType type, String host, int port, String database, String user, String password) {
        this.type = type;
        this.host = host;
        this.port = port;
        this.database = database;
        this.user = user;
        this.password = password;
    }



    public abstract boolean connect();
    public abstract void disconnect();
    public abstract void checkConnection();
    public abstract boolean allTablesExist(String[] tables);
    public abstract boolean systemDataExist();

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
}