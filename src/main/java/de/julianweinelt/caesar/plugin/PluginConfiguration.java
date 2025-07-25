package de.julianweinelt.caesar.plugin;

import java.util.List;

// Wrapper class for plugin.json files
public record PluginConfiguration(
        String pluginName,
        List<String> authors,
        String version,
        String mainClass,
        String description,
        String minAPIVersion,
        String minAPIVersionMC,
        boolean usesEncryption,
        boolean usesDatabase,
        boolean storesSensitiveData,
        boolean compatibleWithMinecraft,
        List<String> requires

) {}