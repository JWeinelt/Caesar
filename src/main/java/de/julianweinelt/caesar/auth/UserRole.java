package de.julianweinelt.caesar.auth;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
public class UserRole {
    private final String name;
    private final String color;
    private final UUID uniqueID;

    private final List<String> permissions = new ArrayList<>();

    public UserRole(String name, String color, UUID uniqueID) {
        this.name = name;
        this.color = color;
        this.uniqueID = uniqueID;
    }

    public UserRole(String name, String color, List<String> permissions, UUID uniqueID) {
        this.name = name;
        this.color = color;
        this.uniqueID = uniqueID;
        this.permissions.addAll(permissions);
    }

    public UserRole(String name, String color, UUID uniqueID, String... permissions) {
        this.name = name;
        this.color = color;
        this.uniqueID = uniqueID;
        this.permissions.addAll(List.of(permissions));
    }

    public void addPermission(String permission) {
        permissions.add(permission);
    }
}