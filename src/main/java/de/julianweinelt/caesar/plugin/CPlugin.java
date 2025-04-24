package de.julianweinelt.caesar.plugin;

import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.nio.file.Path;

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
}