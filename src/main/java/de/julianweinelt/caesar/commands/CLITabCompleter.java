package de.julianweinelt.caesar.commands;

import java.util.List;

public interface CLITabCompleter {
    List<String> complete(String[] args);
}
