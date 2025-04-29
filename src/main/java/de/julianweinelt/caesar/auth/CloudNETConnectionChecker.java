package de.julianweinelt.caesar.auth;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Getter
public class CloudNETConnectionChecker {
    private final String cloudNetHost;
    private final int cloudNetPort;
    private final String cloudUser;
    private final String cloudPassword;
    private boolean useSSL = false;
    private String cloudToken;
    private static final Logger log = LoggerFactory.getLogger(CloudNETConnectionChecker.class);

    public CloudNETConnectionChecker(String cloudNetHost, int cloudNetPort, String cloudUser, String cloudPassword) {
        this.cloudNetHost = cloudNetHost;
        this.cloudNetPort = cloudNetPort;
        this.cloudUser = cloudUser;
        this.cloudPassword = cloudPassword;
    }

    public CloudNETConnectionChecker withSSL() {
        this.useSSL = true;
        return this;
    }


    public boolean checkConnection() {
        String portString = cloudNetPort == 80 || cloudNetPort == 443 ? "" : ":" + cloudNetPort;
        try {
            String urlString = useSSL ? "https://" : "http://";
            URL url = new URL(urlString + cloudNetHost + portString + "/api/v3/auth");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);

            String credentials = cloudUser + ":" + cloudPassword;
            String encodedCredentials = Base64.getEncoder()
                    .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

            conn.setRequestProperty("Authorization", "Basic " + encodedCredentials);

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

            JsonObject o = JsonParser.parseString(responseString).getAsJsonObject();
            return !o.get("accessToken").getAsJsonObject().get("token").getAsString().isEmpty();
        } catch (Exception e) {
            log.error("Failed to connect to CloudNet: {}", e.getMessage());
            return false;
        }
    }
}
