package de.julianweinelt.caesar.storage;

import de.julianweinelt.caesar.Caesar;
import de.julianweinelt.caesar.storage.providers.*;
import lombok.Getter;

@Getter
public class StorageFactory {
    private Storage usedStorage;

    public static StorageFactory getInstance() {
        return Caesar.getInstance().getStorageFactory();
    }

    public Storage provide(StorageType type, Configuration config) {
        switch (type) {
            case MYSQL -> {
                usedStorage = new MySQLStorageProvider(
                        config.getDatabaseHost(),
                        config.getDatabasePort(),
                        config.getDatabaseName(),
                        config.getDatabaseUser(),
                        config.getDatabasePassword()
                );
                return usedStorage;
            }
            case MSSQL -> {
                usedStorage = new MSSQLStorageProvider(
                        config.getDatabaseHost(),
                        config.getDatabasePort(),
                        config.getDatabaseName(),
                        config.getDatabaseUser(),
                        config.getDatabasePassword()
                );
                return usedStorage;
            }
            case ORACLE -> {
                usedStorage = new OracleSQLStorageProvider(
                        config.getDatabaseHost(),
                        config.getDatabasePort(),
                        config.getDatabaseName(),
                        config.getDatabaseUser(),
                        config.getDatabasePassword()
                );
                return usedStorage;
            }
            case Mariadb -> {
                usedStorage = new MariaDBStorageProvider(
                        config.getDatabaseHost(),
                        config.getDatabasePort(),
                        config.getDatabaseName(),
                        config.getDatabaseUser(),
                        config.getDatabasePassword()
                );
                return usedStorage;
            }
            case POSTGRESQL -> {
                usedStorage = new PostgreSQLStorageProvider(
                        config.getDatabaseHost(),
                        config.getDatabasePort(),
                        config.getDatabaseName(),
                        config.getDatabaseUser(),
                        config.getDatabasePassword()
                );
                return usedStorage;
            }
            case H2 -> {
                usedStorage = new H2StorageProvider(
                        config.getDatabaseName()
                );
                return usedStorage;
            }
            case SQLite -> {
                usedStorage = new SQLiteStorageProvider(
                        config.getDatabaseName()
                );
                return usedStorage;
            }
        }
        return null;
    }

    public boolean connect() {
        if (usedStorage != null) {
            return usedStorage.connect();
        }
        return false;
    }

    public enum StorageType {
        MYSQL(3306),
        MSSQL(1434),
        Mariadb(3306),
        SQLite(0),
        H2(0),
        ORACLE(1521),
        POSTGRESQL(5432);

        public final int port;

        StorageType(int port) {
            this.port = port;
        }
    }
}
