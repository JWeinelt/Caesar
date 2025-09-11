package de.julianweinelt.caesar.endpoint;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.julianweinelt.caesar.Caesar;
import de.julianweinelt.caesar.storage.Configuration;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Date;
import java.util.UUID;


public class CaesarServiceProvider {
    private static final Logger log = LoggerFactory.getLogger(CaesarServiceProvider.class);

    private final String endpointURI = Configuration.getInstance().getCaesarAPIEndpoint();

    @Getter
    private String latestClientVersion = "";
    @Getter
    private String latestServerVersion = "";
    @Getter
    private String latestConnectorVersion = "";

    public static CaesarServiceProvider getInstance() {
        return Caesar.getInstance().getServiceProvider();
    }

    public void start() {
        log.info("Starting CaesarServiceProvider...");
        log.info("Connecting to main endpoint...");
        log.info("Checking current versions...");
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().GET().uri(URI.create(endpointURI + "versions")).build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(body -> {
                    JsonObject re =  new JsonParser().parse(body).getAsJsonObject();
                    latestClientVersion = re.get("client").getAsString();
                    latestServerVersion = re.get("server").getAsString();
                    latestConnectorVersion = re.get("connector").getAsString();

                    log.info("""
                            ========== VERSIONS ===========
                            Client (latest): %s
                            Server (latest): %s
                            Server (used): %s
                            ===============================
                            """.formatted(latestClientVersion, latestServerVersion, Caesar.systemVersion));

                })
                .exceptionally(ex -> {
                    log.error("Response (async): {}", ex.getMessage(), ex);
                    return null;
                });

    }



    @Getter
    @Setter
    public static class SupportSession {
        private final UUID helperID;
        private final Date started;
        private boolean enabled;

        public SupportSession(UUID helperID, Date started, boolean enabled) {
            this.helperID = helperID;
            this.started = started;
            this.enabled = enabled;
        }
    }
}
