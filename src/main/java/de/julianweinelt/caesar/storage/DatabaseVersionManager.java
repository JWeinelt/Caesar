package de.julianweinelt.caesar.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.vdurmont.semver4j.Semver;
import de.julianweinelt.caesar.util.LanguageManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;


public class DatabaseVersionManager {
    private static final Logger log = LoggerFactory.getLogger(DatabaseVersionManager.class);

    private final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public void startDownload(String version) {
        Semver v = new Semver(version);
        List<String> all = getAllVersions();
        List<String> toExecute = new ArrayList<>();
        for (String s : all) {
            if (new Semver(s).isLowerThanOrEqualTo(v)) {
                downloadVersion(s);
                toExecute.add(s);
            }
        }

        log.info("Download of database scripts completed.");
        log.info("Continuing with applying changes to database...");
        for (String s : toExecute) {
            log.info("Applying changes for version {}...", s);
            if (StorageFactory.getInstance().getUsedStorage().executeScript(s)) {
                log.info("Changes applied.");
            } else {
                log.warn("Failed to apply changes. Aborting.");
                break;
            }
        }
    }

    private String loadSQLScript(String version) {
        try (BufferedReader br = new BufferedReader(new FileReader(new File(new File("update"), version + ".sql")))) {
            String line;
            StringBuilder sb = new StringBuilder();
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        return "";
    }

    public void downloadVersion(String version) {
        File folder = new File("update");
        try {
            log.info("Downloading database script for {}...", version);

            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(Configuration.getInstance().getCaesarAPIEndpoint() + "public/database" + version))
                    .header("Accept", "application/json")
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                try (FileWriter w = new FileWriter(new File(folder, version + ".sql"))) {
                    w.write(response.body());
                }
            } else if (response.statusCode() != 404) {
                log.error("Failed to download sql script for version {}. Status code: {}", version, response.statusCode());
            }
        } catch (InterruptedException | IOException e) {
            log.error("Failed to download language data: {}", e.getMessage());
        }
    }

    public List<String> getAllVersions() {

        try {
            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(Configuration.getInstance().getCaesarAPIEndpoint() + "public/database/allversions"))
                    .header("Accept", "application/json")
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return GSON.fromJson(response.body(), new TypeToken<List<String>>(){}.getType());
            } else {
                log.error("Failed to download database scripts. Status code: {}", response.statusCode());
            }
        } catch (InterruptedException | IOException e) {
            log.error("Failed to get database scripts: {}", e.getMessage());
        }
        return new ArrayList<>();
    }
}
