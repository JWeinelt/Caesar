package de.julianweinelt.caesar.util;


import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import de.julianweinelt.caesar.Caesar;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class LanguageManager {
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

    public void registerLanguage(String lang) {
        languages.putIfAbsent(lang, new HashMap<>());
    }

    public void addTranslation(String lang, String key, String value) {
        languages.getOrDefault(lang, new HashMap<>()).put(key, value);
    }

    public String getTranslation(String lang, String key) {
        return languages.getOrDefault(lang, new HashMap<>()).getOrDefault(key,
                "No translation found for key '" + key + "' in language '" + lang + "'");
    }

    public void saveLanguage(String lang) {
        try (FileWriter writer = new FileWriter(new File(directory, lang + ".json"))) {
            writer.write(GSON.toJson(languages.getOrDefault(lang, new HashMap<>())));
        } catch (IOException e) {
            log.error("Failed to save language data: {}", e.getMessage());
        }
    }

    public void saveAllLanguages() {
        for (String lang : languages.keySet()) {
            saveLanguage(lang);
        }
    }

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

    public void loadAllLanguageData() {
        for (String lang : getAvailableLanguages()) {
            loadLanguageData(lang);
        }
    }

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


    public void downloadLanguageIfMissing(String lang) {
        if (!new File(directory, lang + ".json").exists()) downloadLanguage(lang);
    }

    public void downloadLanguage(String lang) {
        try {
            log.info("Downloading {}...", lang);

            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.caesarnet.cloud/public/language/" + lang))
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

    public List<String> getAvailableLanguagesFromServer() {
        try {
            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.caesarnet.cloud/public/language/available"))
                    .header("Accept", "application/json")
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return GSON.fromJson(response.body(), new TypeToken<List<String>>(){}.getType());
            }
        } catch (InterruptedException | IOException e) {
            log.error("Failed to get available languages: {}", e.getMessage());
        }
        return new ArrayList<>();
    }

    public HashMap<String, String> flattenJson(String json) {
        JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
        HashMap<String, String> flatMap = new HashMap<>();
        flatten("", jsonObject, flatMap);
        return flatMap;
    }

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