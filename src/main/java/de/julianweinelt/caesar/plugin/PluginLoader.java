package de.julianweinelt.caesar.plugin;

import com.google.gson.*;
import de.julianweinelt.caesar.Caesar;
import de.julianweinelt.caesar.exceptions.PluginInvalidException;
import de.julianweinelt.caesar.plugin.event.Event;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

public class PluginLoader {
    private static final Logger log = LoggerFactory.getLogger(PluginLoader.class);

    private final Registry registry;
    private final PluginScanner scanner = new PluginScanner();
    private final PluginInstantiator instantiator = new PluginInstantiator();
    private final PluginClassLoaderFactory loaderFactory = new PluginClassLoaderFactory(false); // oder true

    private final ClassLoader parentLoader = getClass().getClassLoader();

    public PluginLoader(Registry registry) {
        this.registry = registry;
    }

    public void loadAll() {
        File pluginDir = new File("plugins");

        if (!pluginDir.exists() || !pluginDir.isDirectory()) {
            log.warn("Plugin directory does not exist: {}", pluginDir.getAbsolutePath());
            return;
        }

        File[] jarFiles = pluginDir.listFiles((dir, name) -> name.endsWith(".jar"));
        if (jarFiles == null || jarFiles.length == 0) {
            log.info("No plugins found in directory {}", pluginDir.getAbsolutePath());
            return;
        }

        for (File file : jarFiles) {
            String pluginName = file.getName().replace(".jar", "");
            loadPlugin(pluginName);
        }
    }


    public void unload(String name) {
        CPlugin plugin = registry.getPlugin(name);
        if (plugin == null) return;
        plugin.onDisable();
        registry.removePlugin(name);
        log.info("Unloaded plugin: {}", name);
    }

    public void loadPlugin(String name) {
        File pluginFile = new File("plugins", name.endsWith(".jar") ? name : name + ".jar");

        if (!pluginFile.exists()) {
            log.warn("Plugin file not found: {}", pluginFile.getName());
            return;
        }

        try (JarFile jarFile = new JarFile(pluginFile)) {
            ZipEntry entry = jarFile.getEntry("plugin.json");
            if (entry == null) {
                log.error("Plugin {} does not contain a plugin.json", name);
                return;
            }

            try (InputStream in = jarFile.getInputStream(entry)) {
                String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                PluginConfiguration config = new Gson().fromJson(json, PluginConfiguration.class);
                String pluginName = config.pluginName();

                if (registry.getPlugin(pluginName) != null) {
                    log.warn("Plugin {} is already loaded.", pluginName);
                    return;
                }

                URLClassLoader loader = loaderFactory.createLoader(pluginFile.toURI().toURL(), parentLoader);
                PluginDescriptor descriptor = new PluginDescriptor(pluginName, pluginFile, pluginFile.toURI().toURL(), config);
                CPlugin plugin = instantiator.instantiate(descriptor, loader);

                plugin.onLoad();
                plugin.onDefineEvents();
                plugin.onCreateCommands();
                plugin.onEnable();

                File dataFolder = new File("data/" + plugin.getName());
                if (dataFolder.mkdir()) log.info("Created data folder for {}", plugin.getName());

                registry.addPlugin(plugin);
                log.info("Successfully loaded plugin: {}", plugin.getName());
            }

        } catch (Exception e) {
            log.error("Failed to load plugin {}", name, e);
        }
    }

}