package de.julianweinelt.caesar.endpoint.minecraft;

public class MEndpointHangar extends MCPluginEndpoint{
    protected MEndpointHangar(String name, String baseURL, String apiURLPart, boolean needKey, String apiKey, boolean acceptsDownload) {
        super(name, baseURL, apiURLPart, needKey, apiKey, acceptsDownload);
    }
}
