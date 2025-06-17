package de.julianweinelt.caesar.endpoint.minecraft;

public class MEndpointModrinth extends MCPluginEndpoint{
    protected MEndpointModrinth(String name, String baseURL, String apiURLPart, boolean needKey, String apiKey, boolean acceptsDownload) {
        super(name, baseURL, apiURLPart, needKey, apiKey, acceptsDownload);
    }
}
