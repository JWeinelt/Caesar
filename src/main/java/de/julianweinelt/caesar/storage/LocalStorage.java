package de.julianweinelt.caesar.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import de.julianweinelt.caesar.Caesar;
import de.julianweinelt.caesar.integration.ServerConnection;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class LocalStorage {
    private final Gson GSON = new GsonBuilder().setPrettyPrinting().create();


    private final File configFile = new File("config.json");
    private final File connectionFile = new File("data/connections.json");

    @Getter
    private Configuration data = new Configuration();
    @Getter
    private List<ServerConnection> connections = new ArrayList<>();


    public static LocalStorage getInstance() {
        return Caesar.getInstance().getLocalStorage();
    }


    public void loadData() {
        log.info("Loading local storage...");
        if (!configFile.exists()) saveData();
        try (BufferedReader br = new BufferedReader(new FileReader(configFile))) {
            StringBuilder jsonStringBuilder = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                jsonStringBuilder.append(line);
            }
            data = GSON.fromJson(jsonStringBuilder.toString(), new TypeToken<Configuration>(){}.getType());
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }


    public void saveData() {
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write(GSON.toJson(data));
        } catch (IOException e) {
            log.error("Failed to save object: " + e.getMessage());
        }
        log.info("Local storage saved.");
    }

    public void loadConnections() {
        log.info("Loading API connections");
        if (!connectionFile.exists()) saveConnections();
        try (BufferedReader br = new BufferedReader(new FileReader(connectionFile))) {
            StringBuilder jsonStringBuilder = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                jsonStringBuilder.append(line);
            }
            connections = GSON.fromJson(jsonStringBuilder.toString(), new TypeToken<Configuration>(){}.getType());
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    public void saveConnections() {
        try (FileWriter writer = new FileWriter(connectionFile)) {
            writer.write(GSON.toJson(connections));
        } catch (IOException e) {
            log.error("Failed to save object: " + e.getMessage());
        }
        log.info("Connection data saved.");
    }
}
