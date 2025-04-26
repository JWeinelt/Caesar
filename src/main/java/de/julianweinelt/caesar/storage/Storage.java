package de.julianweinelt.caesar.storage;

public abstract class Storage {
    private final StorageFactory.StorageType type;

    protected Storage(StorageFactory.StorageType type) {
        this.type = type;
    }

    public abstract void connect();
    public abstract void disconnect();
    public abstract void checkConnection();
}