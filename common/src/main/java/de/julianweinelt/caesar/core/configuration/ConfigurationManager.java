package de.julianweinelt.caesar.core.configuration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.julianweinelt.caesar.core.Caesar;
import lombok.Getter;
import lombok.Setter;

import java.io.*;

public class ConfigurationManager {
    private final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final File configFile;

    @Getter
    @Setter
    private Configuration config;

    public ConfigurationManager(File configFile) {
        this.configFile = configFile;
    }

    public static ConfigurationManager getInstance() {
        return Caesar.getInstance().getConfigurationManager();
    }

    public void load() {
        try (FileReader reader = new FileReader(configFile)) {
            config = GSON.fromJson(reader, Configuration.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void save() {
        try (FileWriter writer = new FileWriter(configFile)) {
            GSON.toJson(config, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}