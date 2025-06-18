package de.julianweinelt.caesar.endpoint.plugins;


public record VersionCompatibility(String minecraftVersion, MinecraftServerSoftware serverSoftware,
                                   CompatibilityState state) {}