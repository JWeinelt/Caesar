package de.julianweinelt.caesar.plugin;


/**
 * The SystemPlugin is a core plugin representing the system module of the Caesar plugin framework.
 * It is used to register system-level events and commands.
 * Unloading it may lead to unexpected behavior.<br>
 * <b>DO NOT USE THIS PLUGIN INSTANCE FOR ANY REGISTRATIONS!!!</b>
 * @apiNote This class is intended for internal use only.
 * @author Julian Weinelt
 * @since 1.0.0
 */
public final class SystemPlugin extends CPlugin {

    @Override
    public void onLoad() {

    }

    @Override
    public void onEnable() {
        getLogger().info("Caesar System Module has been enabled.");
    }

    @Override
    public void onDisable() {

    }

    @Override
    public void onDefineEvents() {

    }

    @Override
    public void onCreateCommands() {

    }
}