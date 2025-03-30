package de.julianweinelt.caesar.core.authentication;


import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Getter
@Setter
public class CUser {
    private final UUID uniqueId;
    private final String username;
    private String passwordHashed;
    private boolean isActive = true;

    public CUser(String username, String passwordHashed) {
        this.username = username;
        this.passwordHashed = passwordHashed;
        this.uniqueId = UUID.randomUUID();
    }

    public CUser(UUID uniqueId, String username, String passwordHashed) {
        this.uniqueId = uniqueId;
        this.username = username;
        this.passwordHashed = passwordHashed;
    }

    private final List<String> permissions = new ArrayList<>();

    public void addPermission(String permission) {
        permissions.add(permission);
    }
    public void addPermissions(List<String> permissions) {
        this.permissions.addAll(permissions);
    }
}