package de.julianweinelt.caesar.endpoint;

import com.google.common.reflect.TypeToken;
import com.google.gson.*;
import de.julianweinelt.caesar.storage.LocalStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class MinecraftUUIDFetcher {
    private static final Logger log = LoggerFactory.getLogger(MinecraftUUIDFetcher.class);
    private static final String USER_AGENT = "Caesar/0.0.2";


    private static final HttpClient client = HttpClient.newHttpClient();

    private static final File cacheFile = new File("cache", "usernames.cache");

    private static final ConcurrentHashMap<UUID, JsonObject> cachedNames = new ConcurrentHashMap<>();

    /**
     * Prepares the cache directory and file. If the directory does not exist, it will be created.<br>
     * If the cache file does not exist, it will be created with an empty cache.<br>
     * Usually, the path of the cache is at {@code ./cache/usernames.cache}.
     */
    public static void prepareCacheDirectory() {
        if (cacheFile.getParentFile().mkdirs()) log.info("Created cache directory");
        if (!cacheFile.exists()) saveCache();
    }

    /**
     * Loads the cache from the cache file. If the file does not exist, the method returns immediately, making the cached
     * names potentially empty.
     */
    public static void loadCache() {
        if (!cacheFile.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(cacheFile))) {
            String line;
            StringBuilder content = new StringBuilder();
            while ((line = br.readLine()) != null) content.append(line);
            Type type = new TypeToken<ConcurrentHashMap<UUID, JsonObject>>() {}.getType();
            ConcurrentHashMap<UUID, JsonObject> loaded = new Gson().fromJson(content.toString(), type);
            if (loaded == null) loaded = new ConcurrentHashMap<>();

            synchronized (cachedNames) {
                cachedNames.clear();
                cachedNames.putAll(loaded);
                checkTTL();
            }
        } catch (IOException e) {
            log.error("Could not read cache file", e);
        }
    }


    /**
     * Saves the current cache to the cache file. Entries that have expired will be removed before saving.
     */
    public static void saveCache() {
        synchronized (cachedNames) {
            cachedNames.entrySet().removeIf(entry -> System.currentTimeMillis() > entry.getValue().get("valid").getAsLong());
            try (FileWriter w = new FileWriter(cacheFile)) {
                w.write(new GsonBuilder().setPrettyPrinting().create().toJson(cachedNames));
            } catch (IOException e) {
                log.error("Could not write cache file", e);
            }
        }
    }

    /**
     * Removes expired entries from the cache based on their TTL (time-to-live).
     */
    private static void checkTTL() {
        synchronized (cachedNames) {
            cachedNames.entrySet().removeIf(entry -> System.currentTimeMillis() > entry.getValue().get("valid").getAsLong());
        }
    }

    /**
     * Fetches the Minecraft username associated with the given UUID.<br>
     * If the username is cached and valid, it will be returned from the cache.<br>
     * Otherwise, it will query the Mojang API to retrieve the username.<br>
     * If the Mojang API rate limit is exceeded, it will fall back to PlayerDB API.<br>
     * @param uuid The {@link UUID} of the Minecraft player.
     * @return An {@link Optional<String>} containing the username if found, or empty if not found or an error occurred.
     */
    public static Optional<String> getByID(UUID uuid) {
        if (cachedNames.containsKey(uuid)) return Optional.of(cachedNames.get(uuid).get("username").getAsString());

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid.toString().replace("-", "")))
                    .header("Accept", "application/json")
                    .header("User-Agent", USER_AGENT)
                    .timeout(Duration.ofSeconds(3))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonObject o = JsonParser.parseString(response.body()).getAsJsonObject();
                String name = o.get("name").getAsString();
                JsonObject obj = new JsonObject();
                obj.addProperty("username", name);
                obj.addProperty("valid", System.currentTimeMillis() + LocalStorage.getInstance().getData().getCacheExpiration());
                cachedNames.put(uuid, obj);
                return Optional.of(name);
            } else if (response.statusCode() == 429) { // If there are too many requests
                return getByIDPlayerDB(uuid); // Used as fallback
            } else {
                log.warn("Mojang response: {}", response.body());
                log.error("Failed to get player username from Mojang API. Status code: {}", response.statusCode());
            }
        } catch (InterruptedException | IOException e) {
            log.error("Failed to load player username from Mojang API: {}", e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Asynchronous version of {@link #getByID(UUID)}.
     * @param uuid The {@link UUID} of the Minecraft player.
     * @return A {@link CompletableFuture} that will complete with an {@link Optional<String>} containing the username
     * if found, or empty if not found or an error occurred.
     */
    public static CompletableFuture<Optional<String>> getByIDASync(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> getByID(uuid));
    }

    /**
     * Fetches the Minecraft username associated with the given UUID using PlayerDB API as a fallback.<br>
     * @param uuid The {@link UUID} of the Minecraft player.
     * @return An {@link Optional<String>} containing the username if found, or empty if not found or an error occurred.
     */
    private static Optional<String> getByIDPlayerDB(UUID uuid) { //TODO: Add asynchronous version
        // PlayerDB does not have rate limits and is being used as a fallback if the rate limit of Mojang was exceeded.
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://playerdb.co/api/player/minecraft/" + uuid.toString()))
                    .header("Accept", "application/json")
                    .header("user-agent", USER_AGENT)
                    .timeout(Duration.ofSeconds(3))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonObject o = JsonParser.parseString(response.body()).getAsJsonObject();
                if (!o.get("code").getAsString().equalsIgnoreCase("player.found")) return Optional.empty();


                String name = o.get("data").getAsJsonObject().get("player").getAsJsonObject().get("username").getAsString();

                JsonObject obj = new JsonObject();
                obj.addProperty("username", name);
                obj.addProperty("valid", System.currentTimeMillis() + LocalStorage.getInstance().getData().getCacheExpiration());
                cachedNames.put(uuid, obj);
                return Optional.of(name);
            } else {
                log.warn("PlayerDB response: {}", response.body());
                log.error("Failed to get player username from PlayerDB API. Status code: {}", response.statusCode());
            }
        } catch (InterruptedException | IOException e) {
            log.error("Failed to load player username from PlayerDB API: {}", e.getMessage());
        }
        return Optional.empty();
    }
}
