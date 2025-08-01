package de.julianweinelt.caesar.storage;

import de.julianweinelt.caesar.Caesar;
import de.julianweinelt.caesar.auth.User;
import de.julianweinelt.caesar.storage.providers.*;
import de.julianweinelt.caesar.storage.sandbox.SandBoxManager;

public class StorageFactory {
    private Storage usedStorage;

    public static final StorageType MYSQL = new StorageType("MYSQL", 3306, config -> new MySQLStorageProvider(
            config.getDatabaseHost(),
            config.getDatabasePort(),
            config.getDatabaseName(),
            config.getDatabaseUser(),
            config.getDatabasePassword()
    ));

    public static final StorageType MSSQL = new StorageType("MSSQL", 1434, config -> new MSSQLStorageProvider(
            config.getDatabaseHost(),
            config.getDatabasePort(),
            config.getDatabaseName(),
            config.getDatabaseUser(),
            config.getDatabasePassword()
    ));



    public static StorageFactory getInstance() {
        return Caesar.getInstance().getStorageFactory();
    }

    public StorageFactory() {

    }

    public Storage getUsedStorage(User user) {
        if (SandBoxManager.getInstance().workingInSandBox(user)) {
            return SandBoxManager.getInstance().getSandBox(user).getUsedStorage();
        }
        return usedStorage;
    }

    public Storage provide(StorageType type, Configuration config) {
        this.usedStorage = type.createProvider(config);
        return this.usedStorage;
    }

    public Storage provideSandBox(StorageType type, Configuration config) {
        return type.createProvider(config);
    }

    public boolean connect() {
        if (usedStorage != null) {
            return usedStorage.connect();
        }
        return false;
    }
}
