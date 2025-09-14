package de.julianweinelt.caesar.endpoint.minecraft;

public class MEndpointSpigot extends MCPluginEndpoint{
    public MEndpointSpigot() {
        super("SpigotMC", "https://api.spiget.org", "/", false, "", true);
    }
}