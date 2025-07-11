package de.julianweinelt.caesar.ai;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class DiscordMessageContextChecker {
    private static final Logger log = LoggerFactory.getLogger(DiscordMessageContextChecker.class);


    private static final String API_KEY = "AIzaSysC6CuZ0NQoRyK0YB-f89r5AzCVISzVDId0";
    private static final String ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + API_KEY;
    private static final Gson gson = new Gson();

    public static void main(String[] args) throws IOException {
        System.out.println(tellJoke());
    }

    public static JsonObject getMessageType(String message) {

        List<JsonObject> chatHistory = new ArrayList<>();

        JsonObject systemPrompt = new JsonObject();
        systemPrompt.addProperty("role", "user");
        systemPrompt.add("parts", gson.toJsonTree(new Part[]{
                new Part("You will get a message by a user and say if they need help, " +
                        "want to ban someone or kick from the server or timeout someone. Please answer in this format: " +
                        "{\"type\":\"<the type you think>\"}. When timing out someone, add a property called 'time'" +
                        " with the amount in seconds. Of no time is given, just say '-1' there. Append a field 'reason' " +
                        "if there is a reason given. Add 'ticket' if a ticket type was specified" +
                        ". If none of these types a applicable, just answer normally. " +
                        "Valid types are: timeout, kick, ban, help, open_ticket, close_ticket, joke"),
        }));
        chatHistory.add(systemPrompt);

        JsonObject userPrompt = new JsonObject();
        userPrompt.addProperty("role", "user");
        userPrompt.add("parts", gson.toJsonTree(new Part[]{
                new Part(message)
        }));
        chatHistory.add(userPrompt);

        JsonObject body = new JsonObject();
        body.add("contents", gson.toJsonTree(chatHistory));

        try {
            URL url = new URL(ENDPOINT);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setDoOutput(true);

            int responseCode = conn.getResponseCode();

            BufferedReader reader;
            if (responseCode >= 200 && responseCode < 300) {
                reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            } else {
                reader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
            }

            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line.trim());
            }
            reader.close();
            String responseString = response.toString();
            JsonObject responseJson = JsonParser.parseString(responseString).getAsJsonObject();
            String text = responseJson
                    .getAsJsonArray("candidates").get(0).getAsJsonObject()
                    .getAsJsonObject("content").getAsJsonArray("parts").get(0)
                    .getAsJsonObject().get("text").getAsString();
            return JsonParser.parseString(text).getAsJsonObject();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }

        return new JsonObject();
    }

    public static String tellJoke() {

        List<JsonObject> chatHistory = new ArrayList<>();

        JsonObject systemPrompt = new JsonObject();
        systemPrompt.addProperty("role", "user");
        systemPrompt.add("parts", gson.toJsonTree(new Part[]{
                new Part("You are a funny bot"),
        }));
        chatHistory.add(systemPrompt);

        JsonObject userPrompt = new JsonObject();
        userPrompt.addProperty("role", "user");
        userPrompt.add("parts", gson.toJsonTree(new Part[]{
                new Part("@Caesar Tell me a joke!")
        }));
        chatHistory.add(userPrompt);

        JsonObject body = new JsonObject();
        body.add("contents", gson.toJsonTree(chatHistory));

        try {
            URL url = new URL(ENDPOINT);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setDoOutput(true);

            int responseCode = conn.getResponseCode();

            BufferedReader reader;
            if (responseCode >= 200 && responseCode < 300) {
                reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            } else {
                reader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
            }

            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line.trim());
            }
            reader.close();
            String responseString = response.toString();
            JsonObject responseJson = JsonParser.parseString(responseString).getAsJsonObject();
            return responseJson
                    .getAsJsonArray("candidates").get(0).getAsJsonObject()
                    .getAsJsonObject("content").getAsJsonArray("parts").get(0)
                    .getAsJsonObject().get("text").getAsString();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return "";
    }

    static class Part {
        String text;
        Part(String text) { this.text = text; }
    }
}
