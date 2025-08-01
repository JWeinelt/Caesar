package de.julianweinelt.caesar.auth;

import de.julianweinelt.caesar.Caesar;
import de.julianweinelt.caesar.plugin.Registry;
import de.julianweinelt.caesar.plugin.event.Event;
import de.julianweinelt.caesar.storage.StorageFactory;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UserManager {
    private static final Logger log = LoggerFactory.getLogger(UserManager.class);

    @Getter
    private final List<User> users = new ArrayList<>();

    @Getter
    private final List<UserRole> userRoles = new ArrayList<>();

    @Getter
    private final List<CPermission> permissions = new ArrayList<>();

    public static UserManager getInstance() {
        return Caesar.getInstance().getUserManager();
    }

    private final ThreadLocal<User> nextActionUser = new ThreadLocal<>();


    public UserManager with(User user) {
        nextActionUser.set(user);
        return this;
    }

    private User consumeCurrentUser() {
        User user = nextActionUser.get();
        nextActionUser.remove();
        return user;
    }


    public void overrideUsers(List<User> users) {
        this.users.clear();
        this.users.addAll(users);
    }

    public void createUser(String username, String password) {
        createUser(username, password, "0");
    }

    public void createUser(String username, String password, String discord) {
        UUID userID = UUID.randomUUID();
        User user = new User(userID, username,
                password.hashCode(), discord);
        users.add(user);
        StorageFactory.getInstance().getUsedStorage(consumeCurrentUser()).createUser(user);
        Registry.getInstance().callEvent(new Event("UserCreateEvent")
                .set("username", username)
                .set("uuid", userID));
    }

    public User getUser(String username) {
        log.debug("Users found: {}", users.size());
        for (User user : users) {
            if (user.getUsername().equals(username)) {
                log.debug("Found user: {}", username);
                return user;
            }
        }
        log.debug("User {} not found in memory. Loading from database", username);
        User u = StorageFactory.getInstance().getUsedStorage(consumeCurrentUser()).getUser(username);
        users.add(u);
        return u;
    }

    public User getUser(UUID uuid) {
        for (User user : users) {
            if (user.getUuid().equals(uuid)) return user;
        }
        return null;
    }

    public void deleteUser(String username) {
        User user = getUser(username);
        if (user != null) {
            StorageFactory.getInstance().getUsedStorage(consumeCurrentUser()).deleteUser(username);
            users.remove(user);
        }
    }
    public void deleteUser(UUID uuid) {
        User user = getUser(uuid);
        if (user != null) {
            StorageFactory.getInstance().getUsedStorage(consumeCurrentUser()).deleteUser(user.getUsername());
            users.remove(user);
        }
    }

    public void addRole(UserRole role) {
        userRoles.add(role);
    }

    public UserRole getRole(String name) {
        for (UserRole role : userRoles) if (role.getName().equals(name)) return role;
        return null;
    }

    public UserRole getRole(UUID uniqueID) {
        for (UserRole role : userRoles) if (role.getUniqueID().equals(uniqueID)) return role;
        return null;
    }

    public void getAllRoles() {
        userRoles.clear();
        userRoles.addAll(StorageFactory.getInstance().getUsedStorage(consumeCurrentUser()).getAllRoles());
    }

    public void getAllPermissions() {
        permissions.clear();
        permissions.addAll(StorageFactory.getInstance().getUsedStorage(consumeCurrentUser()).getAllPermissions());
    }

    public void addPermission(CPermission permission) {
        permissions.add(permission);
    }

    public UUID getPermissionID(String key) {
        for (CPermission p : permissions) if (p.permissionKey().equals(key)) return p.uniqueID();
        return null;
    }

    public CPermission getPermission(String key) {
        for (CPermission p : permissions) if (p.permissionKey().equals(key)) return p;
        return null;
    }
    public CPermission getPermission(UUID id) {
        for (CPermission p : permissions) if (p.uniqueID().equals(id)) return p;
        return null;
    }

    public void syncUserPermissions() {
        for (User u : users) {
            List<String> permissions = StorageFactory.getInstance().getUsedStorage(consumeCurrentUser()).getUserPermissions(u.getUuid());
            u.getPermissions().clear();
            for (String permission : permissions) u.addPermission(permission);
        }
    }

    public void setUserActive(String username, boolean active) {
        getUser(username).setActive(active);
        StorageFactory.getInstance().getUsedStorage(consumeCurrentUser()).updateUser(getUser(username));
    }

    public void setUserActive(UUID uuid, boolean active) {
        getUser(uuid).setActive(active);
        StorageFactory.getInstance().getUsedStorage(consumeCurrentUser()).updateUser(getUser(uuid));
    }

    public void createSupportUser(int code) {

    }
}