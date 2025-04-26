package de.julianweinelt.caesar.storage.providers;

import de.julianweinelt.caesar.storage.Storage;
import de.julianweinelt.caesar.storage.StorageFactory;

public class MySQLStorageProvider extends Storage {
    protected MySQLStorageProvider() {
        super(StorageFactory.StorageType.MYSQL);
    }

    @Override
    public void connect() {

    }

    @Override
    public void disconnect() {

    }

    @Override
    public void checkConnection() {

    }
}
