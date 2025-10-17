package de.julianweinelt.caesar.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import de.julianweinelt.caesar.Caesar;
import de.julianweinelt.caesar.endpoint.CaesarServer;
import de.julianweinelt.caesar.integration.ServerConnection;
import de.julianweinelt.caesar.plugin.Registry;
import de.julianweinelt.caesar.plugin.event.Event;
import io.javalin.http.Context;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
public class LocalStorage {
    private final Gson GSON = new GsonBuilder().setPrettyPrinting().create();


    private final File configFile = new File("config.json");
    private final File connectionFile = new File("data/connections.json");
    private final File profilePath = new File("data/profiles/");

    @Getter
    private Configuration data = new Configuration();
    @Getter
    private List<ServerConnection> connections = new ArrayList<>();


    public static LocalStorage getInstance() {
        return Caesar.getInstance().getLocalStorage();
    }

    public ServerConnection getConnection(String name) {
        for (ServerConnection connection : connections) if (connection.getName().equals(name)) return connection;
        return null;
    }
    public ServerConnection getConnection(UUID uuid) {
        for (ServerConnection connection : connections) if (connection.getUuid().equals(uuid)) return connection;
        return null;
    }


    /**
     * Loads the local storage from the config file.<br><br>
     * DO NOT CALL THIS METHOD MANUALLY!
     */
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


    /**
     * Saves the local storage to the config file.<br>
     * Calls {@link #saveData(boolean)} internally, where {@code silent} is set to {@code false}.
     */
    public void saveData() {
        saveData(false);
    }

    /**
     * Saves the local storage to the config file.
     * @param silent Define whether to log the save action or not.
     */
    public void saveData(boolean silent) {
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write(GSON.toJson(data));
        } catch (IOException e) {
            log.error("Failed to save object: " + e.getMessage());
        }
        if (!silent) log.info("Local storage saved.");
    }


    /**
     * Gets the profile image of a user.
     * It internally updates all response properties and bodies to make the {@link Context} return the image data.
     * @param ctx The {@link Context} of the HTTP request.
     * @param user The {@link UUID} of the user.
     * @throws Exception If an I/O error occurs reading the file or determining its MIME type.
     */
    public void getProfileImage(Context ctx, UUID user) throws Exception {
        Path path = new File(profilePath, user.toString()).toPath();
        if (!Files.exists(path)) {
            ctx.status(404).result(CaesarServer.createErrorResponse(CaesarServer.ErrorType.FILE_NOT_FOUND));
            return;
        }

        String mimeType = Files.probeContentType(path);
        if (mimeType == null) mimeType = "application/octet-stream";

        try (InputStream is = Files.newInputStream(path)) {
            ctx.contentType(mimeType);
            ctx.result(is);
        }
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
            log.info("Loaded {} connection{}", connections.size(), (connections.size() == 1) ? "" : "s");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    public void checkConnectionKeys() {
        connections.forEach(conn->{
            APIKeySaver.getInstance().loadKey(conn.getName());

            log.info("Found connection with key: {} ({})",  conn.getUuid(), conn.getName());
        });
    }

    public void saveConnections() {
        try (FileWriter writer = new FileWriter(connectionFile)) {
            writer.write(GSON.toJson(connections));
        } catch (IOException e) {
            log.error("Failed to save object: " + e.getMessage());
        }
        log.info("Connection data saved.");
    }

    /**
     * Loads a JSON file from the data folder and deserializes it into an object of the specified type.
     * @param fileName The name of the file located in the {@code data} folder (without .json extension)
     * @param type The {@link Type} of the object to deserialize into. Can be anything which is serializable.<br>
     *             See <a href="https://docs.caesarnet.cloud/docs/Developer%20Documentation/Creating%20Plugins/Creating-configuration-files">Developer Docs</a>
     *             for more information about that.
     * @return The deserialized object, or {@code null} if an error occurred.
     * @param <T> The type of the object to deserialize into.
     */
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

    /**
     * Saves an object as a JSON file in the data folder.
     * @param object The object to serialize and save.
     * @param fileName The name of the file to save the object to (without .json extension).
     */
    public void save(Object object, String fileName) {

        try (FileWriter writer = new FileWriter(new File("data", fileName + ".json"))) {
            writer.write(GSON.toJson(object));
        } catch (IOException e) {
            log.error("Failed to save object: " + e.getMessage());
        }
    }
}