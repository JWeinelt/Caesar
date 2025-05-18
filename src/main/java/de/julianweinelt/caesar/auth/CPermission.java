package de.julianweinelt.caesar.auth;

import java.util.UUID;

public record CPermission(UUID uniqueID, String permissionKey) {}