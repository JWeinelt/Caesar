package de.julianweinelt.caesar.plugin;

import java.net.URL;
import java.net.URLClassLoader;

public class PluginClassLoaderFactory {

    private final boolean isolated;

    public PluginClassLoaderFactory(boolean isolated) {
        this.isolated = isolated;
    }

    /**
     * Creates a URLClassLoader for the given plugin URL.
     * @param pluginUrl {@link URL} of the plugin JAR
     * @param parent Parent {@link ClassLoader}
     * @return {@link URLClassLoader} instance
     */
    public URLClassLoader createLoader(URL pluginUrl, ClassLoader parent) {
        return isolated
                ? new URLClassLoader(new URL[]{pluginUrl}, null)
                : new URLClassLoader(new URL[]{pluginUrl}, parent);
    }
}
