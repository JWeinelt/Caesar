package de.julianweinelt.caesar.auth;

import java.util.ArrayList;
import java.util.List;

public class User {
    private String username;
    private int password;
    private final List<String> permissions = new ArrayList<>();

    private String discordID;
}