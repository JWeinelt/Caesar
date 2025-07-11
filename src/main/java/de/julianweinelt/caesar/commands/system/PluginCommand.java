package de.julianweinelt.caesar.commands.system;

import de.julianweinelt.caesar.commands.CLICommandExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PluginCommand implements CLICommandExecutor {
    private static final Logger log = LoggerFactory.getLogger(PluginCommand.class);

    @Override
    public void execute(String label, String[] args) {
        if (args.length == 0) {
            log.info("""
            No arguments provided.
            
            Aliases:
            plugin plugins
            
            Command usage:
            plugin load <filename> - Loads a plugin from file (experimental!)
            plugin unload <plugin> - Unloads a plugin (experimental!)
            plugin install <url|id> [-nl] - Downloads a plugin from URL/market. Use -nl flag to make it not load automatically.
            plugin reload <name> [--remote] - Reload all configurations of a plugin. Use --remote to send this command to all CaesarLinks.
            plugin info <name> - Displays important information about a plugin.
            """);
            return;
        }
        if (args.length == 1) {
            if (args[0].equals("load")) {
                log.info("Please provide the name of the plugin file to load.");
                return;
            }
        }
    }
}
