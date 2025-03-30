package de.julianweinelt.caesar.core.authentication;

import de.julianweinelt.caesar.core.Caesar;

import java.util.ArrayList;
import java.util.List;

public class UserManager {
    private final List<CUser> users = new ArrayList<>();

    public static UserManager getInstance() {
        return Caesar.getInstance().getUserManager();
    }

    public CUser getUser(String username) {
        for (CUser user : users) {
            if (user.getUsername().equals(username)) return user;
        }
        return null;
    }

    public boolean verify(String username, String password) {
        CUser user = getUser(username);
        if (user == null) return false;
        return user.getPasswordHashed().equals(password.hashCode() + "");
    }

    public void createUser(String username, String password) {
        CUser user = new CUser(username, password.hashCode() + "");
        users.add(user);
    }
    public void addUserPermissions(String[] permissions, String username) {
        CUser user = getUser(username);
        if (user == null) return;
        for (String permission : permissions) {
            if (user.getPermissions().contains(permission)) continue;
            user.getPermissions().add(permission);
        }
    }
}