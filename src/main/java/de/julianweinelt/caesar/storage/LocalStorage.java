package de.julianweinelt.caesar.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import de.julianweinelt.caesar.Caesar;
import de.julianweinelt.caesar.integration.ServerConnection;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.lang.reflect.Type;
import java.time.temporal.ChronoUnit;
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
            if (data.getBackupType().equals(Configuration.BackupType.INCREMENTAL)) {
                log.warn("INCREMENTAL backups are not supported!");
            }
            if (data.getIntervalType().equals(ChronoUnit.HOURS) || data.getIntervalType().equals(ChronoUnit.MINUTES)
            || data.getIntervalType().equals(ChronoUnit.SECONDS) || data.getIntervalType().equals(ChronoUnit.MILLIS)
            || data.getIntervalType().equals(ChronoUnit.NANOS) || data.getIntervalType().equals(ChronoUnit.MICROS)) {
                log.warn("The interval unit {} is too short! Using fallback: DAYS.", data.getIntervalType().name());
                log.warn("Backup intervals must be per day or longer.");
                data.setIntervalType(ChronoUnit.DAYS);
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }


    public void saveData() {
        saveData(false);
    }

    public void saveData(boolean silent) {
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write(GSON.toJson(data));
        } catch (IOException e) {
            log.error("Failed to save object: " + e.getMessage());
        }
        if (!silent) log.info("Local storage saved.");
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
            connections = GSON.fromJson(jsonStringBuilder.toString(), new TypeToken<List<ServerConnection>>(){}.getType());
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

    public <T> T load(String fileName, Type type) {
        try (BufferedReader br = new BufferedReader(new FileReader(new File("data", fileName + ".json")))) {
            StringBuilder jsonStringBuilder = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                jsonStringBuilder.append(line);
            }
            return GSON.fromJson(jsonStringBuilder.toString(), type);
        } catch (IOException e) {
            log.error(e.getMessage());
            return null;
        }
    }

    public void save(Object object, String fileName) {

        try (FileWriter writer = new FileWriter(new File("data", fileName + ".json"))) {
            writer.write(GSON.toJson(object));
        } catch (IOException e) {
            log.error("Failed to save object: " + e.getMessage());
        }
    }
}