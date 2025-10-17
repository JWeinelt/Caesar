package de.julianweinelt.caesar.endpoint.minecraft;

import lombok.Getter;

/**
 * Represents an endpoint for Minecraft plugin APIs.<br>
 * Contains information about the endpoint's name, base URL, API URL part,<br>
 * whether an API key is needed, the API key itself, and if it accepts downloads.<br>
 * <br>
 * Default endpoints are:<br> {@link MEndpointModrinth} for modrinth.com,<br>
 * {@link MEndpointCurseForge} for curseforge.com and<br>
 * {@link MEndpointSpigot} for spigotmc.org.
 */
@Getter
public class MCPluginEndpoint {
    private final String name;
    private final String baseURL;
    private final String apiURLPart;
    private final boolean needKey;
    private final String apiKey;
    private final boolean acceptsDownload;

    protected MCPluginEndpoint(String name, String baseURL, String apiURLPart, boolean needKey, String apiKey, boolean acceptsDownload) {
        this.name = name;
        this.baseURL = baseURL;
        this.apiURLPart = apiURLPart;
        this.needKey = needKey;
        this.apiKey = apiKey;
        this.acceptsDownload = acceptsDownload;
    }
}
