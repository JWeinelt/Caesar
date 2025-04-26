package de.julianweinelt.caesar.storage;

public class StorageFactory {
    public enum StorageType {
        MYSQL,
        MSSQL,
        Mariadb,
        SQLite,
        H2,
        ORACLE,
        POSTGRESQL,
    }
}
