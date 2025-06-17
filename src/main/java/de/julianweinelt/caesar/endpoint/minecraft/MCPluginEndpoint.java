package de.julianweinelt.caesar.endpoint.minecraft;

import lombok.Getter;

@Getter
public abstract class MCPluginEndpoint {
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
