package de.julianweinelt.caesar.plugin;

import java.io.File;
import java.net.URL;

public record PluginDescriptor(String name, File jarFile, URL jarUrl, PluginConfiguration config) {}
