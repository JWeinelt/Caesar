package de.julianweinelt.caesar.core;

import de.julianweinelt.caesar.core.configuration.Configuration;
import de.julianweinelt.caesar.core.configuration.ConfigurationManager;
import de.julianweinelt.caesar.core.util.FileChecker;
import de.julianweinelt.caesar.core.util.ui.DisplayComponent;
import de.julianweinelt.caesar.core.util.ui.DisplayComponentSection;
import de.julianweinelt.caesar.core.util.ui.DisplayComponentType;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.util.logging.Logger;

public class Caesar {
    private File configFile;
    private Logger log;

    private ConfigurationManager configurationManager;

    public void instantiate(File configFile, Logger log) {
        this.configFile = configFile;
        this.log = log;
    }

    public void start() {
        configurationManager = new ConfigurationManager(configFile);
        if (FileChecker.isFirstStart()) {
            log.info("Welcome to the Caesar!");
            log.info("Let's start with some configuration!");
            Configuration configuration = new Configuration();
            log.info("We just create a default configuration file.");
            configuration.addComponent(
                    new DisplayComponentSection("config.database", "Database")
                            .addComponent(DisplayComponent.Builder.create(DisplayComponentType.STRING, "database.type", "MySQL", false))
                            .addComponent(DisplayComponent.Builder.create(DisplayComponentType.STRING,
                                    "database.host", new String("localhost"), false))
                            .addComponent(DisplayComponent.Builder.create(DisplayComponentType.INTEGER, "database.port", 3306, false))
                            .addComponent(DisplayComponent.Builder.create(DisplayComponentType.STRING, "database.user", "root", false))
                            .addComponent(DisplayComponent.Builder.create(DisplayComponentType.STRING, "database.password", "root", false))
                            .addComponent(DisplayComponent.Builder.create(DisplayComponentType.BOOLEAN, "database.autocommit", true, false))
                            .addComponent(DisplayComponent.Builder.create(DisplayComponentType.STRING, "database.name", "caesar", false))
            );
            configuration.addComponent(
                    new DisplayComponentSection("config.users", "Users")
                            .addComponent(DisplayComponent.Builder.create(DisplayComponentType.BOOLEAN, "users.enable-register", true, false))
                            .addComponent(DisplayComponent.Builder.create(DisplayComponentType.BOOLEAN, "users.pw-policy", true, false))
            );
            log.info("Saving default configuration file...");
            configurationManager.setConfig(configuration);
            configurationManager.save();
            log.info("Configuration file saved!");
            log.info("Now let's continue to create an admin user for setup.");
        }
    }
}
