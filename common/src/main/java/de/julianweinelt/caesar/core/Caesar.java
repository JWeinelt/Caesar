package de.julianweinelt.caesar.core;

import de.julianweinelt.caesar.core.authentication.UserManager;
import de.julianweinelt.caesar.core.configuration.Configuration;
import de.julianweinelt.caesar.core.configuration.ConfigurationManager;
import de.julianweinelt.caesar.core.util.FileChecker;
import de.julianweinelt.caesar.core.util.PasswordGenerator;
import de.julianweinelt.caesar.core.util.logging.Log;
import de.julianweinelt.caesar.core.util.ui.DisplayComponent;
import de.julianweinelt.caesar.core.util.ui.DisplayComponentSection;
import de.julianweinelt.caesar.core.util.ui.DisplayComponentType;
import lombok.Getter;

import java.io.File;


@Getter
public class Caesar {
    private File configFile;

    private ConfigurationManager configurationManager;
    private UserManager userManager;

    @Getter
    private static Caesar instance;

    public void instantiate(File configFile) {
        this.configFile = configFile;
        instance = this;
    }

    public void start() {
        configurationManager = new ConfigurationManager(configFile);
        userManager = new UserManager();
        if (FileChecker.isFirstStart()) {
            Log.info("Welcome to the Caesar!");
            Log.info("Let's start with some configuration!");
            Configuration configuration = new Configuration();
            Log.info("We just create a default configuration file.");
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
            Log.info("Saving default configuration file...");
            configurationManager.setConfig(configuration);
            configurationManager.save();
            Log.info("Configuration file saved!");
            Log.info("Now let's continue to create an admin user for setup.");
            String password = PasswordGenerator.generatePassword(12);
            userManager.createUser("admin", password);
            Log.info("Created new user:");
            Log.info("Username: admin");
            Log.info("Password: {}", password);
        }
    }
}
