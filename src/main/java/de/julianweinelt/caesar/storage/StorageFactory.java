package de.julianweinelt.caesar.storage;

import de.julianweinelt.caesar.Caesar;
import de.julianweinelt.caesar.storage.providers.*;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

/**
 * Factory class for creating and managing storage providers.
 * @author JulianWeinelt
 * @version 1.0
 */
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

    /**
     * Provides a storage provider based on the given type and configuration.
     * @param type The {@link StorageType} to create the provider for.
     * @param config The {@link Configuration} to use for the provider.
     * @return The created {@link Storage} provider.
     */
    public Storage provide(StorageType type, Configuration config) {
        this.usedStorage = type.createProvider(config);
        return this.usedStorage;
    }

    /**
     * Connects to the used storage provider.
     * @return True if the connection was successful, false otherwise.
     */
    public boolean connect() {
        if (usedStorage != null) {
            return usedStorage.connect();
        }
        return false;
    }
}
