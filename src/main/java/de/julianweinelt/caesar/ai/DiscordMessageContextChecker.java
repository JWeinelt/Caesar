package de.julianweinelt.caesar.ai;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DiscordMessageContextChecker {
    private static final Logger log = LoggerFactory.getLogger(DiscordMessageContextChecker.class);


    private static final String API_KEY = "AIzaSysC6CuZ0NQoRyK0YB-f89r5AzCVISzVDId0";
    private static final String ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + API_KEY;
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build();
    private static final Gson gson = new Gson();

    public static void main(String[] args) throws IOException {
        List<JsonObject> chatHistory = new ArrayList<>();

        JsonObject systemPrompt = new JsonObject();
        systemPrompt.addProperty("role", "user");
        systemPrompt.add("parts", gson.toJsonTree(new Part[]{
                new Part("You will get a message by a user and say if they need help, " +
                        "want to ban someone or kick from the server or timeout someone. Please answer in this format: " +
                        "{\"type\":\"<the type you think>\"}. When timing out someone, add a property called 'time'" +
                        " with the amount in seconds. Of no time is given, just say '-1' there. Append a field 'reason' " +
                        "if there is a reason given. Valid types are: timeout, kick, ban, help, open_ticket, close_ticket, joke"),
        }));
        chatHistory.add(systemPrompt);

        JsonObject userPrompt = new JsonObject();
        userPrompt.addProperty("role", "user");
        userPrompt.add("parts", gson.toJsonTree(new Part[]{
                new Part("@Caesar sperr mal bitte @justcody vom server")
        }));
        chatHistory.add(userPrompt);

        JsonObject body = new JsonObject();
        body.add("contents", gson.toJsonTree(chatHistory));

        Request request = new Request.Builder()
                .url(ENDPOINT)
                .post(RequestBody.create(gson.toJson(body), MediaType.get("application/json")))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code: " + response);

            String responseBody = response.body().string();
            //System.out.println("Antwort:\n" + responseBody);
            JsonObject responseJson = JsonParser.parseString(responseBody).getAsJsonObject();
            String text = responseJson
                    .getAsJsonArray("candidates").get(0).getAsJsonObject()
                    .getAsJsonObject("content").getAsJsonArray("parts").get(0)
                    .getAsJsonObject().get("text").getAsString();

            System.out.println("Antworttext:\n" + text);
        }
    }

    public static JsonObject getMessageType(String message) {

        List<JsonObject> chatHistory = new ArrayList<>();

        JsonObject systemPrompt = new JsonObject();
        systemPrompt.addProperty("role", "user");
        systemPrompt.add("parts", gson.toJsonTree(new Part[]{
                new Part("You will get a message by a user and say if they need help, " +
                        "want to ban someone or kick from the server or timeout someone. Please answer in this format: " +
                        "{\"type\":\"<the type you think>\"}."),
        }));
        chatHistory.add(systemPrompt);

        JsonObject userPrompt = new JsonObject();
        userPrompt.addProperty("role", "user");
        userPrompt.add("parts", gson.toJsonTree(new Part[]{
                new Part("@Caesar how do I setup this bot?")
        }));
        chatHistory.add(userPrompt);

        JsonObject body = new JsonObject();
        body.add("contents", gson.toJsonTree(chatHistory));

        Request request = new Request.Builder()
                .url(ENDPOINT)
                .post(RequestBody.create(gson.toJson(body), MediaType.get("application/json")))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code: " + response);

            String responseBody = response.body().string();
            //System.out.println("Antwort:\n" + responseBody);
            JsonObject responseJson = JsonParser.parseString(responseBody).getAsJsonObject();
            String text = responseJson
                    .getAsJsonArray("candidates").get(0).getAsJsonObject()
                    .getAsJsonObject("content").getAsJsonArray("parts").get(0)
                    .getAsJsonObject().get("text").getAsString();

            return JsonParser.parseString(text).getAsJsonObject();
        } catch (IOException | NullPointerException e) {
            log.error(e.getMessage());
        }
        return new JsonObject();
    }

    static class Part {
        String text;
        Part(String text) { this.text = text; }
    }
}
