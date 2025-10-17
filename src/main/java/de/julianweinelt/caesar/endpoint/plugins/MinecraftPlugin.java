package de.julianweinelt.caesar.endpoint.plugins;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Wrapper class for a Minecraft plugin containing its internal ID, rating and compatibility for particular versions.
 */
@Getter
@Setter
public class MinecraftPlugin {
    private final UUID uniqueID;
    private float rating;
    private VersionCompatibility compatibility;

    public MinecraftPlugin(UUID uniqueID) {
        this.uniqueID = uniqueID;
    }
}