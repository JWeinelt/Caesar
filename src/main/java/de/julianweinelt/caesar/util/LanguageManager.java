package de.julianweinelt.caesar.util;


import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import de.julianweinelt.caesar.Caesar;
import de.julianweinelt.caesar.storage.Configuration;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LanguageManager {
    private static final Logger log = LoggerFactory.getLogger(LanguageManager.class);

    private final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final HashMap<String, HashMap<String, String>> languages = new HashMap<>();

    private final File directory = new File("./data/language");

    public LanguageManager() {
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                log.error("Could not create language folder.");
            }
        }
    }

    public static LanguageManager getInstance() {
        return Caesar.getInstance().getLanguageManager();
    }

    /**
     * Register a new language
     * @param lang Language code (e.g. "en", "de")
     * @apiNote Please always use lowercase ISO 639-1 language codes
     */
    public void registerLanguage(String lang) {
        languages.putIfAbsent(lang, new HashMap<>());
    }

    /**
     * Add a translation for a language
     * @param lang Language code (e.g. "en", "de")
     * @param key Translation key (e.g. "greeting.hello")
     * @param value Translation value (e.g. "Hello")
     */
    public void addTranslation(String lang, String key, String value) {
        languages.getOrDefault(lang, new HashMap<>()).put(key, value);
    }

    /**
     * Get a translation for a language and key
     * @param lang Language code (e.g. "en", "de")
     * @param key Translation key (e.g. "greeting.hello")
     * @return Translation value (e.g. "Hello")
     */
    public String getTranslation(String lang, String key) {
        return languages.getOrDefault(lang, new HashMap<>()).getOrDefault(key,
                "No translation found for key '" + key + "' in language '" + lang + "'");
    }

    /**
     * Save language data to a file on the disk
     * @apiNote Usually written to {@code /data/language/<lang>.json}
     * @param lang Language code (e.g. "en", "de")
     */
    public void saveLanguage(String lang) {
        try (FileWriter writer = new FileWriter(new File(directory, lang + ".json"))) {
            writer.write(GSON.toJson(languages.getOrDefault(lang, new HashMap<>())));
        } catch (IOException e) {
            log.error("Failed to save language data: {}", e.getMessage());
        }
    }

    /**
     * Calls {@link #saveLanguage(String)} for all registered languages
     */
    public void saveAllLanguages() {
        for (String lang : languages.keySet()) {
            saveLanguage(lang);
        }
    }

    /**
     * Load language data from a file on the disk
     * @param lang Language code (e.g. "en", "de")
     * @apiNote Usually loaded from {@code /data/language/<lang>.json}
     */
    public void loadLanguageData(String lang) {
        try (BufferedReader br = new BufferedReader(new FileReader(new File(directory, lang + ".json")))) {
            StringBuilder jsonStringBuilder = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                jsonStringBuilder.append(line);
            }
            HashMap<String, String> loaded = GSON.fromJson(jsonStringBuilder.toString(), new TypeToken<HashMap<String, String>>(){}.getType());
            languages.put(lang, loaded);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    /**
     * Calls {@link #loadLanguageData(String)} for all available languages
     */
    public void loadAllLanguageData() {
        for (String lang : getAvailableLanguages()) {
            loadLanguageData(lang);
        }
    }

    /**
     * Get a list of all available languages on the disk
     * @return List of language codes (e.g. "en", "de")
     */
    public List<String> getAvailableLanguages() {
        File[] files = directory.listFiles();
        if (files == null) return new ArrayList<>();
        List<String> langs = new ArrayList<>();
        for (File file : files) {
            if (file.getName().endsWith(".json")) {
                langs.add(file.getName().replace(".json", ""));
            }
        }
        return langs;
    }


    /**
     * Download language data from the server if the file is missing on the disk
     * @param lang Language code (e.g. "en", "de")
     */
    public void downloadLanguageIfMissing(String lang) {
        if (!new File(directory, lang + ".json").exists()) downloadLanguage(lang);
        else log.info("File {} already exists.", lang);
    }

    /**
     * Download language data from the server
     * @param lang Language code (e.g. "en", "de")
     */
    public void downloadLanguage(String lang) {
        try {
            log.info("Downloading {}...", lang);

            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(Configuration.getInstance().getCaesarAPIEndpoint() + "public/language/server/" + lang))
                    .header("Accept", "application/json")
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                languages.put(lang, flattenJson(response.body()));
                saveLanguage(lang);
            } else {
                log.error("Failed to download language data. Status code: {}", response.statusCode());
            }
        } catch (InterruptedException | IOException e) {
            log.error("Failed to download language data: {}", e.getMessage());
        }
    }

    /**
     * Get a list of all available languages from the server
     * @return List of language codes (e.g. "en", "de")
     */
    public List<String> getAvailableLanguagesFromServer() {
        try {
            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(Configuration.getInstance().getCaesarAPIEndpoint() + "public/language/server/available"))
                    .header("Accept", "application/json")
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return GSON.fromJson(response.body(), new TypeToken<List<String>>(){}.getType());
            } else {
                log.error("Failed to download language data. Status code: {}", response.statusCode());
            }
        } catch (InterruptedException | IOException e) {
            log.error("Failed to get available languages: {}", e.getMessage());
        }
        return new ArrayList<>();
    }

    /**
     * Flatten a nested JSON object into a flat map
     * @param json JSON string
     * @return Flat map of key-value pairs
     */
    public HashMap<String, String> flattenJson(String json) {
        JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
        HashMap<String, String> flatMap = new HashMap<>();
        flatten("", jsonObject, flatMap);
        return flatMap;
    }

    /**
     * Recursive helper method to flatten a JSON object
     * @param prefix Prefix for nested keys
     * @param jsonObject JSON object to flatten
     * @param flatMap Flat map to store key-value pairs
     */
    private void flatten(String prefix, JsonObject jsonObject, Map<String, String> flatMap) {
        for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            if (entry.getValue().isJsonObject()) {
                flatten(key, entry.getValue().getAsJsonObject(), flatMap);
            } else if (entry.getValue().isJsonPrimitive()) {
                flatMap.put(key, entry.getValue().getAsString());
            }
        }
    }
}