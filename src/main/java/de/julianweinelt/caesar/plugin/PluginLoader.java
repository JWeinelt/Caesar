package de.julianweinelt.caesar.plugin;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

public class PluginLoader {
    private static final Logger log = LoggerFactory.getLogger(PluginLoader.class);

    private final Registry registry;
    private final PluginScanner scanner = new PluginScanner();
    private final PluginInstantiator instantiator = new PluginInstantiator();
    private final PluginClassLoaderFactory loaderFactory = new PluginClassLoaderFactory(false); // or true

    private final ClassLoader parentLoader = getClass().getClassLoader();

    public PluginLoader(Registry registry) {
        this.registry = registry;
    }

    public void loadAll() {
        List<PluginDescriptor> descriptors = scanner.scan();
        Map<String, PluginConfiguration> pluginConfigs = new HashMap<>();
        Map<String, File> pluginFiles = new HashMap<>();
        for (PluginDescriptor descriptor : descriptors) pluginConfigs.put(descriptor.name(), descriptor.config());
        for (PluginDescriptor descriptor : descriptors) pluginFiles.put(descriptor.name(), descriptor.jarFile());

        List<String> loadOrder = resolveLoadOrder(pluginConfigs);

        for (String pluginName : loadOrder) {
            File pluginFile = pluginFiles.get(pluginName);
            PluginConfiguration config = pluginConfigs.get(pluginName);
            if (pluginFile != null && config != null) {
                try {
                    loadPlugin(config.pluginName());
                } catch (Exception e) {
                    log.error("Failed to load plugin '{}'", pluginName, e);
                }
            }
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

    private List<String> resolveLoadOrder(Map<String, PluginConfiguration> configs) {
        List<String> loadOrder = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> visiting = new HashSet<>();

        for (String plugin : configs.keySet()) {
            visit(plugin, configs, loadOrder, visited, visiting);
        }

        return loadOrder;
    }

    private void visit(String plugin,
                       Map<String, PluginConfiguration> configs,
                       List<String> loadOrder,
                       Set<String> visited,
                       Set<String> visiting) {
        if (visited.contains(plugin)) return;
        if (visiting.contains(plugin))
            throw new IllegalStateException("Cyclic dependency involving: " + plugin);

        visiting.add(plugin);

        List<String> dependencies = configs.get(plugin).requires();
        if (dependencies != null) {
            for (String dep : dependencies) {
                if (!configs.containsKey(dep)) {
                    throw new IllegalStateException("Missing dependency: " + dep + " required by " + plugin);
                }
                visit(dep, configs, loadOrder, visited, visiting);
            }
        }

        visiting.remove(plugin);
        visited.add(plugin);
        loadOrder.add(plugin);
    }

}