package de.julianweinelt.caesar.core.authentication;


import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Getter
public class CUser {
    private UUID uniqueId;
    private String username;
    private String passwordHashed;

    private final List<String> permissions = new ArrayList<>();

    public void addPermission(String permission) {
        permissions.add(permission);
    }
    public void addPermissions(List<String> permissions) {
        this.permissions.addAll(permissions);
    }
}