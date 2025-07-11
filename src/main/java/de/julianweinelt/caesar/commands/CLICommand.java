package de.julianweinelt.caesar.commands;


import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

@Getter
public class CLICommand {
    private final String name;
    private final List<String> aliases = new ArrayList<>();

    private CLICommandExecutor executor;
    private CLITabCompleter tabCompleter;

    public CLICommand(String name) {
        this.name = name;
    }

    public CLICommand alias(String alias) {
        aliases.add(alias);
        return this;
    }
    public CLICommand aliases(List<String> aliases) {
        this.aliases.addAll(aliases);
        return this;
    }

    public CLICommand aliases(String... aliases) {
        this.aliases.addAll(Arrays.asList(aliases));
        return this;
    }

    public CLICommand executor(CLICommandExecutor executor) {
        this.executor = executor;
        return this;
    }

    public CLICommand tabCompleter(CLITabCompleter tabCompleter) {
        this.tabCompleter = tabCompleter;
        return this;
    }

    public void execute(String[] arguments) {
        List<String> args = new ArrayList<>(Arrays.asList(arguments));
        args.remove(0);
        executor.execute(arguments[0], args.toArray(new String[0]));
    }
}
