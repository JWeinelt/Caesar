package de.julianweinelt.caesar.auth;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class User {
    private final UUID uuid;
    private String username;
    private int password;
    private boolean active;
    private boolean newlyCreated;
    private boolean applyPasswordPolicy;
    private final List<String> permissions = new ArrayList<>();

    private String discordID;

    public User(UUID uuid) {
        this.uuid = uuid;
    }

    public User(UUID uuid, String username, int password, String discordID) {
        this.uuid = uuid;
        this.username = username;
        this.password = password;
        this.discordID = discordID;
    }
}