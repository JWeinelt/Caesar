package de.julianweinelt.caesar.storage;

public abstract class Storage {
    public abstract void connect();
    public abstract void disconnect();
    public abstract void checkConnection();
}