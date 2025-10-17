package de.julianweinelt.caesar.plugin;

import de.julianweinelt.caesar.endpoint.CaesarServer;
import jdk.dynalink.linker.LinkerServices;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public abstract class CPlugin {
    private String name;
    private String description;
    private String[] authors;
    private String version;

    private Path jarURL;
    private String minAPIVersion;
    private boolean storesSensitiveData = false;
    private boolean usesEncryption = false;
    private final List<String> dependencies = new ArrayList<>();
    private final List<String> optionalDependencies = new ArrayList<>();

    /**
     * Returns the data folder of the module. Typically the path is ~/data/[ModuleName].
     * @return {@link File} object of the data folder
     */
    public File getDataFolder() {
        return new File("data/" + name);
    }


    /**
     * Called when module is loaded. API calls should not be done here, as dependencies may not be loaded at this time.
     */
    public abstract void onLoad();

    /**
     * Called when the module is enabled. All dependencies are loaded.
     */
    public abstract void onEnable();

    /**
     * Called when module is being disabled
     */
    public abstract void onDisable();

    /**
     * Called to define events in {@link Registry}.
     */
    public abstract void onDefineEvents();

    /**
     * Called to define commands in {@link Registry}.
     */
    public abstract void onCreateCommands();

    protected Logger getLogger() {
        return LoggerFactory.getLogger(this.getClass());
    }

    /**
     * Called when the plugin is being enabled, only if the plugin is running as an extension for CaesarConnector.
     */
    public void onBukkitEnable() {}

    /**
     * Called when the plugin is being disabled, only if the plugin is running as an extension for CaesarConnector.
     */
    public void onBukkitDisable() {}

    /**
     * Called when the Bukkit server has finished loading, only if the plugin is running as an extension for CaesarConnector.
     * <br><b>IMPORTANT:</b> Do not call any API methods in this method, as they may not be available yet!
     */
    public void onBukkitLoad() {}
}