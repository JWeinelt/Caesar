package de.julianweinelt.caesar.auth;

import de.julianweinelt.caesar.Caesar;
import de.julianweinelt.caesar.plugin.Registry;
import de.julianweinelt.caesar.plugin.event.Event;
import de.julianweinelt.caesar.storage.StorageFactory;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UserManager {
    private static final Logger log = LoggerFactory.getLogger(UserManager.class);

    public static UserManager getInstance() {
        return Caesar.getInstance().getUserManager();
    }

    @Getter
    private final List<User> users = new ArrayList<>();

    @Getter
    private final List<UserRole> userRoles = new ArrayList<>();

    @Getter
    private final List<CPermission> permissions = new ArrayList<>();

    public void overrideUsers(List<User> users) {
        this.users.clear();
        this.users.addAll(users);
    }

    /**
     * Creates a new user with default discord ID "0"
     * @param username The username of the new user as a {@link String}
     * @param password The password of the new user as a {@link String}
     */
    public void createUser(String username, String password) {
        createUser(username, password, "0");
    }

    /**
     * Creates a new user and hashes the given password
     * @param username The username of the new user as a {@link String}
     * @param password The password of the new user as a {@link String}
     * @param discord The discord ID of the new user as a {@link String}
     */
    //TODO: Salted hashing
    public void createUser(String username, String password, String discord) {
        UUID userID = UUID.randomUUID();
        User user = new User(userID, username,
                password.hashCode(), discord);
        users.add(user);
        StorageFactory.getInstance().getUsedStorage().createUser(user);
        Registry.getInstance().callEvent(new Event("UserCreateEvent")
                .set("username", username)
                .set("uuid", userID));
    }


    /**
     * Gets a user by their username. If the user is not found in memory, it will be loaded from the database.
     * @param username The username of the user as a {@link String}
     * @return The user as a {@link User} object
     */
    @Nullable
    public User getUser(String username) {
        log.debug("Users found: {}", users.size());
        for (User user : users) {
            if (user.getUsername().equals(username)) {
                log.debug("Found user: {}", username);
                return user;
            }
        }
        log.debug("User {} not found in memory. Loading from database", username);
        User u = StorageFactory.getInstance().getUsedStorage().getUser(username); //TODO: Change to Optional
        // null could come from Storage implementation if user does not exist
        users.add(u);
        return u;
    }

    /**
     * Gets a user by their UUID.
     * @param uuid The UUID of the user as a {@link UUID}
     * @return The user as a {@link User} object
     */
    @Nullable
    public User getUser(UUID uuid) { //TODO: Improve with Optional
        for (User user : users) {
            if (user.getUuid().equals(uuid)) return user;
        }
        return null;
    }

    /**
     * Deletes a user by their username.
     * @param username The username of the user as a {@link String}
     */
    public void deleteUser(String username) {
        User user = getUser(username);
        if (user != null) {
            StorageFactory.getInstance().getUsedStorage().deleteUser(username);
            users.remove(user);
        }
    }

    /**
     * Deletes a user by their UUID.
     * @param uuid The UUID of the user as a {@link UUID}
     */
    public void deleteUser(UUID uuid) {
        User user = getUser(uuid);
        if (user != null) {
            StorageFactory.getInstance().getUsedStorage().deleteUser(user.getUsername());
            users.remove(user);
        }
    }

    /**
     * Adds a role to the user manager.
     * @param role The role to add as a {@link UserRole}
     */
    public void addRole(UserRole role) {
        userRoles.add(role);
    }

    /**
     * Gets a role by its name.
     * @param name The name of the role as a {@link String}
     * @return The role as a {@link UserRole} object
     */
    @Nullable
    public UserRole getRole(String name) { //TODO: Improve with Optional
        for (UserRole role : userRoles) if (role.getName().equals(name)) return role;
        return null;
    }

    /**
     * Gets a role by its unique ID.
     * @param uniqueID The unique ID of the role as a {@link UUID}
     * @return The role as a {@link UserRole} object
     */
    @Nullable
    public UserRole getRole(UUID uniqueID) { //TODO: Improve with Optional
        for (UserRole role : userRoles) if (role.getUniqueID().equals(uniqueID)) return role;
        return null;
    }

    /**
     * Loads all roles from the storage into memory.
     */
    public void getAllRoles() {
        userRoles.clear();
        userRoles.addAll(StorageFactory.getInstance().getUsedStorage().getAllRoles());
    }

    /**
     * Loads all permissions from the storage into memory.
     */
    public void getAllPermissions() {
        permissions.clear();
        permissions.addAll(StorageFactory.getInstance().getUsedStorage().getAllPermissions());
    }

    /**
     * Adds a permission to the user manager.
     * @param permission The permission to add as a {@link CPermission}
     */
    public void addPermission(CPermission permission) {
        permissions.add(permission);
    }

    /**
     * Gets a permission ID by its key.
     * @param key The key of the permission as a {@link String}
     * @return The unique ID of the permission as a {@link UUID}
     */
    @Nullable
    public UUID getPermissionID(String key) { //TODO: Improve with Optional
        for (CPermission p : permissions) if (p.permissionKey().equals(key)) return p.uniqueID();
        return null;
    }

    /**
     * Gets a permission by its key.
     * @param key The key of the permission as a {@link String}
     * @return The permission as a {@link CPermission} object
     */
    @Nullable
    public CPermission getPermission(String key) { //TODO: Improve with Optional
        for (CPermission p : permissions) if (p.permissionKey().equals(key)) return p;
        return null;
    }

    /**
     * Gets a permission by its unique ID.
     * @param id The unique ID of the permission as a {@link UUID}
     * @return The permission as a {@link CPermission} object
     */
    @Nullable
    public CPermission getPermission(UUID id) { //TODO: Improve with Optional
        for (CPermission p : permissions) if (p.uniqueID().equals(id)) return p;
        return null;
    }

    /**
     * Synchronizes user permissions from the storage to the in-memory user objects.
     */
    public void syncUserPermissions() {
        for (User u : users) {
            List<String> permissions = StorageFactory.getInstance().getUsedStorage().getUserPermissions(u.getUuid());
            u.getPermissions().clear();
            for (String permission : permissions) u.addPermission(permission);
        }
    }

    /**
     * Sets a user's active status by their username.
     * @param username The username of the user as a {@link String}
     * @param active The active status as a {@link boolean}
     */
    public void setUserActive(String username, boolean active) {
        getUser(username).setActive(active);
        StorageFactory.getInstance().getUsedStorage().updateUser(getUser(username));
    }

    /**
     * Sets a user's active status by their UUID.
     * @param uuid The UUID of the user as a {@link UUID}
     * @param active The active status as a {@link boolean}
     */
    public void setUserActive(UUID uuid, boolean active) {
        getUser(uuid).setActive(active);
        StorageFactory.getInstance().getUsedStorage().updateUser(getUser(uuid));
    }

    /**
     * Creates a support user with a specific code.
     * @param code The support code as an {@link int}
     */
    public void createSupportUser(int code) {
        //TODO: Implement support user creation
    }
}