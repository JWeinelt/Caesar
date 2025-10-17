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

    /**
     * Add an alias for this command
     * @param alias the alias to add
     * @return the command instance for chaining
     */
    public CLICommand alias(String alias) {
        aliases.add(alias);
        return this;
    }

    /**
     * Add multiple aliases for this command
     * @param aliases the aliases to add
     * @return the command instance for chaining
     */
    public CLICommand aliases(List<String> aliases) {
        this.aliases.addAll(aliases);
        return this;
    }

    /**
     * Add multiple aliases for this command
     * @param aliases the aliases to add
     * @return the command instance for chaining
     */
    public CLICommand aliases(String... aliases) {
        this.aliases.addAll(Arrays.asList(aliases));
        return this;
    }

    /**
     * Set the executor for this command
     * @param executor the executor to set
     * @return the command instance for chaining
     */
    public CLICommand executor(CLICommandExecutor executor) {
        this.executor = executor;
        return this;
    }

    /**
     * Set the tab completer for this command
     * @param tabCompleter the tab completer to set
     * @return the command instance for chaining
     */
    public CLICommand tabCompleter(CLITabCompleter tabCompleter) {
        this.tabCompleter = tabCompleter;
        return this;
    }

    /**
     * Execute this command
     * @param arguments the arguments passed to the command
     */
    public void execute(String[] arguments) {
        List<String> args = new ArrayList<>(Arrays.asList(arguments));
        args.remove(0);
        executor.execute(arguments[0], args.toArray(new String[0]));
    }
}
