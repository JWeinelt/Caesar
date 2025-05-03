package de.julianweinelt.caesar.commands;

public interface CLICommandExecutor {
    void execute(String label, String[] args);
}