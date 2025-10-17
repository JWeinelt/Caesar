package de.julianweinelt.caesar.plugin;

import com.google.gson.Gson;
import de.julianweinelt.caesar.plugin.event.Event;
import org.jetbrains.annotations.ApiStatus;
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
    private final PluginClassLoaderFactory loaderFactory = new PluginClassLoaderFactory(false);

    private final Map<String, String> pluginFileNames = new HashMap<>();

    private final ClassLoader parentLoader = getClass().getClassLoader();

    public PluginLoader(Registry registry) {
        this.registry = registry;
        registerEvents();
    }

    /**
     * Registers plugin-related events in the registry.
     */
    private void registerEvents() {
        registry.registerEvents(
                "PluginLoadEvent",
                "PluginEnableEvent",
                "PluginDisableEvent",
                "PluginPreloadEvent"
        );
    }

    /**
     * Loads all plugins found by the scanner, resolving dependencies and load order.
     */
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
                    pluginFileNames.put(config.pluginName(), pluginFile.getName());
                    loadPlugin(config.pluginName());
                } catch (Exception e) {
                    log.error("Failed to load plugin '{}'", pluginName, e);
                }
            }
        }
    }

    /**
     * Unloads a plugin by name.
     * @param name The name of the plugin to unload.
     * @apiNote Using this method is experimental and may lead to issues with dependencies.
     */
    @ApiStatus.Experimental
    public void unload(String name) {
        CPlugin plugin = registry.getPlugin(name);
        if (plugin == null) return;
        plugin.onDisable();
        registry.removePlugin(name);
        registry.callEvent(new Event("PluginDisableEvent").nonCancellable().set("plugin", name));
        log.info("Unloaded plugin: {}", name);
    }

    /**
     * Loads a plugin by its name.
     * @param name The name of the plugin to load.
     * @apiNote Using this method is experimental and may lead to issues with dependencies.
     */
    @ApiStatus.Experimental
    public void loadPlugin(String name) {
        if (pluginFileNames.containsKey(name)) name = pluginFileNames.get(name);
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

                File dataFolder = new File("data/" + plugin.getName());
                if (dataFolder.mkdir()) log.info("Created data folder for {}", plugin.getName());

                plugin.onLoad();
                registry.callEvent(new Event("PluginLoadEvent").nonCancellable()
                        .set("name", pluginName)
                        .set("plugin", plugin)
                        .set("dataFolder", dataFolder)
                        .set("classLoader", loader)
                        .set("descriptor", descriptor)
                );
                plugin.onDefineEvents();
                plugin.onCreateCommands();
                plugin.onEnable();
                registry.callEvent(new Event("PluginEnableEvent").nonCancellable()
                        .set("name", pluginName)
                        .set("plugin", plugin)
                        .set("author", plugin.getAuthors())
                        .set("version", plugin.getVersion())
                        .set("dependencies", plugin.getDependencies())
                        .set("dataFolder", dataFolder)
                        .set("classLoader", loader)
                        .set("descriptor", descriptor)
                );

                registry.addPlugin(plugin);
                log.info("Successfully loaded plugin: {}", plugin.getName());
            }

        } catch (Exception e) {
            log.error("Failed to load plugin {}", name, e);
        }
    }

    /**
     * Resolves the load order of plugins based on their dependencies using topological sorting.
     * @param configs A map of plugin names to their configurations.
     * @return A list of plugin names in the order they should be loaded.
     */
    private List<String> resolveLoadOrder(Map<String, PluginConfiguration> configs) {
        List<String> loadOrder = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> visiting = new HashSet<>();

        for (String plugin : configs.keySet()) {
            registry.callEvent(new Event("PluginPreloadEvent").nonCancellable()
                    .set("plugin", plugin).set("pluginConfiguration", configs.get(plugin)));
            visit(plugin, configs, loadOrder, visited, visiting);
        }

        return loadOrder;
    }

    /**
     * Helper method for topological sorting of plugins.
     * @param plugin The current plugin being visited.
     * @param configs A map of plugin names to their configurations.
     * @param loadOrder The list to store the load order.
     * @param visited The set of already visited plugins.
     * @param visiting The set of currently visiting plugins (for cycle detection).
     */
    private void visit(String plugin,
                       Map<String, PluginConfiguration> configs,
                       List<String> loadOrder,
                       Set<String> visited,
                       Set<String> visiting) {
        if (visited.contains(plugin)) return;
        if (visiting.contains(plugin))
            throw new IllegalStateException("Cyclic dependency involving: " + plugin);

        visiting.add(plugin);

        PluginConfiguration config = configs.get(plugin);
        if (config == null) {
            throw new IllegalStateException("Plugin configuration for '" + plugin + "' is missing.");
        }
        List<String> dependencies = config.requires();
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