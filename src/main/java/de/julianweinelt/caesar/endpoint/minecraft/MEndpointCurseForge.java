package de.julianweinelt.caesar.endpoint.minecraft;

public class MEndpointCurseForge extends MCPluginEndpoint{
    public MEndpointCurseForge() {
        super("CurseForge", "https://api.curseforge.com", "/", true, "", true);
    }
}
