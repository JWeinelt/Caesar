package de.julianweinelt.caesar.commands.system;

import de.julianweinelt.caesar.commands.CLITabCompleter;

import java.util.ArrayList;
import java.util.List;

public class CommandCompleter extends CLITabCompleter {
    @Override
    public List<String> onTabCompletion(String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length <= 1) {
            complete(completions, args[0], "info");
        }
        return completions;
    }
}