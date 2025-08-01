package de.julianweinelt.caesar.storage.sandbox;

import de.julianweinelt.caesar.auth.User;
import de.julianweinelt.caesar.storage.LocalStorage;
import de.julianweinelt.caesar.storage.Storage;
import de.julianweinelt.caesar.storage.StorageFactory;
import de.julianweinelt.caesar.storage.StorageType;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
public class SandBox {
    private static final Logger log = LoggerFactory.getLogger(SandBox.class);

    private final UUID uniqueID;
    private final List<UUID> users = new ArrayList<>();
    private Storage usedStorage;
    private boolean active;
    private final long activeUntil;
    private final String schemaName;

    public SandBox(UUID uniqueID, long activeUntil) {
        this.uniqueID = uniqueID;
        this.activeUntil = activeUntil;
        schemaName = "sndbx_" + UUID.randomUUID().toString().replace("-", "").substring(0, 7);
    }

    public String create(StorageType type) {
        usedStorage = StorageFactory.getInstance().provideSandBox(type, LocalStorage.getInstance().getData());
        usedStorage.connectSandBox(() -> {
            try {
                if (usedStorage.hasSandboxPermissions()) {
                    usedStorage.executeScript("CALL create_sandbox('" + LocalStorage.getInstance().getData().getDatabaseName()
                            + "', '" + schemaName + "');");
                }
                usedStorage.setDatabase(schemaName);
                usedStorage.executeScript("USE " + schemaName + ";");
            } catch (SQLException e) {
                log.error("Failed to execute USE script: {}", e.getMessage(), e);
            }
        });
        active = true;
        return schemaName;
    }

    public void addUser(User user) {
        users.add(user.getUuid());
    }
    public void addUser(UUID uuid) {
        users.add(uuid);
    }
    public void removeUser(UUID uuid) {
        users.remove(uuid);
    }
    public void removeUser(User user) {
        users.remove(user.getUuid());
    }

    public void deactivate() {
        active = false;
        usedStorage.disconnect();
    }
    public void activate() {
        active = true;
        usedStorage.connect();
    }
    public void sync() {
        //TODO: Add syncing from productive system into sandbox
    }

    public void delete() {
        users.clear();
        try {
            usedStorage.executeScript("CALL delete_sandbox('" + schemaName + "');");
        } catch (SQLException e) {
            log.error("Failed to execute drop sandbox script: {}", e.getMessage(), e);
        }
    }
}