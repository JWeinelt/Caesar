package de.julianweinelt.caesar.plugin;

import java.net.URL;
import java.net.URLClassLoader;

public class PluginClassLoaderFactory {

    private final boolean isolated;

    public PluginClassLoaderFactory(boolean isolated) {
        this.isolated = isolated;
    }

    public URLClassLoader createLoader(URL pluginUrl, ClassLoader parent) {
        return isolated
                ? new URLClassLoader(new URL[]{pluginUrl}, null)
                : new URLClassLoader(new URL[]{pluginUrl}, parent);
    }
}
