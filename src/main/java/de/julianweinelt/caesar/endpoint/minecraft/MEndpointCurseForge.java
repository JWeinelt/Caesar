package de.julianweinelt.caesar.endpoint.minecraft;

public class MEndpointCurseForge extends MCPluginEndpoint{
    protected MEndpointCurseForge(String name, String baseURL, String apiURLPart, boolean needKey, String apiKey, boolean acceptsDownload) {
        super("CurseForge", "https://api.curseforge.com", "/", true, "", true);
    }
}
