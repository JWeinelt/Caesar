package de.julianweinelt.caesar.endpoint.minecraft;

public class MEndpointModrinth extends MCPluginEndpoint{
    protected MEndpointModrinth() {
        super("Modrinth", "https://staging-api.modrinth.com", "/", false, "", false);
    }
}
