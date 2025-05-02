package de.julianweinelt.caesar.auth;

import de.julianweinelt.caesar.Caesar;
import de.julianweinelt.caesar.storage.StorageFactory;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UserManager {
    public static UserManager getInstance() {
        return Caesar.getInstance().getUserManager();
    }

    @Getter
    private final List<User> users = new ArrayList<>();

    public void overrideUsers(List<User> users) {
        this.users.clear();
        this.users.addAll(users);
    }

    public void createUser(String username, String password) {
        User user = new User(UUID.randomUUID(), username,
                password.hashCode(), "0");
        users.add(user);
        StorageFactory.getInstance().getUsedStorage().createUser(user);
    }

    public void createUser(String username, String password, String discord) {
        User user = new User(UUID.randomUUID(), username,
                password.hashCode(), discord);
        users.add(user);
        StorageFactory.getInstance().getUsedStorage().createUser(user);
    }

    public User getUser(String username) {
        for (User user : users) {
            if (user.getUsername().equals(username)) return user;
        }
        return null;
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
            users.remove(user);
            StorageFactory.getInstance().getUsedStorage().deleteUser(username);
        }
    }
    public void deleteUser(UUID uuid) {
        User user = getUser(uuid);
        if (user != null) {
            users.remove(user);
            StorageFactory.getInstance().getUsedStorage().deleteUser(user.getUsername());
        }
    }

    public void setUserActive(String username, boolean active) {
        getUser(username).setActive(active);
        StorageFactory.getInstance().getUsedStorage().updateUser(getUser(username));
    }

    public void setUserActive(UUID uuid, boolean active) {
        getUser(uuid).setActive(active);
        StorageFactory.getInstance().getUsedStorage().updateUser(getUser(uuid));
    }
}