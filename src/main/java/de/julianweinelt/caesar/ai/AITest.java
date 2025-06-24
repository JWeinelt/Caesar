package de.julianweinelt.caesar.ai;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AITest {
    private static final String API_KEY = "AIzaSysC6CuZ0NQoRyK0YB-f89r5AzCVISzVDId0";
    private static final String ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + API_KEY;
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build();
    private static final Gson gson = new Gson();

    public static void main(String[] args) throws IOException {
        String prompt = "Weißt du noch, wie die Anwendung heißt?";

        List<JsonObject> chatHistory = new ArrayList<>();

// Systemprompt – definiert Junos Persönlichkeit
        JsonObject systemPrompt = new JsonObject();
        systemPrompt.addProperty("role", "user");
        systemPrompt.add("parts", gson.toJsonTree(new Part[]{
                new Part("Du bist Juno, eine hilfreiche KI für das System Caesar. Du berätst Admins bei der Serververwaltung. Sprich sachlich, analytisch und motivierend."),
                new Part("Die aktuelle Auslastung vom Server BedWars-1 beträgt 6GB. Spielerzahl: 692; Plugins: BedWars, ViaVersion, Floodgate, FastASyncWorldEdit; Version 1.21.5 Purpur"),
                new Part("Die aktuelle Auslastung vom Server Lobby-1 beträgt 4GB. Spielerzahl: 315"),
        }));
        chatHistory.add(systemPrompt);

// Erste Nutzerfrage
        JsonObject userPrompt = new JsonObject();
        userPrompt.addProperty("role", "user");
        userPrompt.add("parts", gson.toJsonTree(new Part[]{
                new Part("Gib mir einen Statusbericht zum Netzwerk")
        }));
        chatHistory.add(userPrompt);

// Anfrage aufbauen
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

    // Hilfsklasse für einen „Part“ (Teil eines Prompts)
    static class Part {
        String text;
        Part(String text) { this.text = text; }
    }
}
