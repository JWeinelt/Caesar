package de.julianweinelt.caesar.storage;

import de.julianweinelt.caesar.Caesar;
import de.julianweinelt.caesar.storage.providers.*;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

@Getter
public class StorageFactory {
    private Storage usedStorage;

    public static final StorageType MYSQL = new StorageType("MYSQL", 3306, config -> new MySQLStorageProvider(
            config.getDatabaseHost(),
            config.getDatabasePort(),
            config.getDatabaseName(),
            config.getDatabaseUser(),
            config.getDatabasePassword()
    ));



    public static StorageFactory getInstance() {
        return Caesar.getInstance().getStorageFactory();
    }

    public StorageFactory() {}

    public Storage provide(StorageType type, Configuration config) {
        this.usedStorage = type.createProvider(config);
        return this.usedStorage;
    }

    public boolean connect() {
        if (usedStorage != null) {
            return usedStorage.connect();
        }
        return false;
    }
}
