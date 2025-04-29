package de.julianweinelt.caesar.plugin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.julianweinelt.caesar.Caesar;
import de.julianweinelt.caesar.exceptions.PluginInvalidException;
import de.julianweinelt.caesar.plugin.event.Event;
import lombok.extern.slf4j.Slf4j;
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
    private final HashMap<String, URL> moduleURLs = new HashMap<>();
    private final List<String> alreadyLoaded = new ArrayList<>();
    private URLClassLoader sharedLoader;

    public PluginLoader(Registry registry) {
        this.registry = registry;
    }

    public void prepareLoading() {
        File folder = new File("plugins");
        File[] modules = folder.listFiles();
        if (modules == null) return;
        for (File f : modules) {
            if (f.getName().endsWith(".jar")) {
                try {
                    Path jarPath = Path.of("plugins/" + f.getName());

                    try (JarFile jarFile = new JarFile(jarPath.toFile())) {
                        ZipEntry jsonEntry = jarFile.getEntry("plugin.json");
                        if (jsonEntry == null) {
                            throw new PluginInvalidException("The loaded file " + f.getName() + " does not contain a module.json file.");
                        }

                        try(InputStream inputStream = jarFile.getInputStream(jsonEntry)) {
                            String jsonString = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                            JsonObject json = JsonParser.parseString(jsonString).getAsJsonObject();

                            URL jarURL = jarPath.toUri().toURL();
                            moduleURLs.put(json.get("pluginName").getAsString(), jarURL);
                        }

                    }
                } catch (Exception e) {
                    log.error("Error while loading plugin.");
                    log.error(e.getMessage());
                    for (StackTraceElement s : e.getStackTrace()) {
                        log.error(s.toString());
                    }
                }
            }
        }


        sharedLoader = new URLClassLoader(moduleURLs.values().toArray(URL[]::new), getClass().getClassLoader());
    }

    public void loadPlugins() {
        File folder = new File("plugins");
        File[] modules = folder.listFiles();
        if (modules == null) return;
        for (File f : modules) {
            if (f.getName().endsWith(".jar")) {
                loadPlugin(f.getName().replace(".jar", ""));
            }
        }
    }

    public void loadPlugin(String name) {
        log.info("Loading {}", name);
        name = name.replace(".jar", "");
        try {
            Path jarPath = Path.of("plugins/"+name+".jar");

            try (JarFile jarFile = new JarFile(jarPath.toFile())) {
                ZipEntry jsonEntry = jarFile.getEntry("plugin.json");
                if (jsonEntry == null) {
                    throw new PluginInvalidException("The loaded file " + name + ".jar does not contain a plugin.json file.");
                }


                try(InputStream inputStream = jarFile.getInputStream(jsonEntry)) {
                    String jsonString = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                    JsonObject json = JsonParser.parseString(jsonString).getAsJsonObject();
                    List<String> authors = new ArrayList<>();
                    for (JsonElement e : json.get("authors").getAsJsonArray()) authors.add(e.getAsString());
                    StringBuilder autorString = new StringBuilder();
                    for (String s : authors) autorString.append(s).append(",");
                    log.info("Detected plugin with name {} created by {}.", json.get("pluginName").getAsString(), autorString);
                    log.info("Version: {}", json.get("version").getAsString());

                    if (Caesar.getInstance().getRegistry().getPlugin(name) != null) {
                        return; // Module with the name is already loaded
                    }

                    String mainClassName = json.get("mainClass").getAsString();
                    URLClassLoader classLoader = sharedLoader;

                    log.info("Loading {}", mainClassName);

                    Class<?> mainClass = Class.forName(mainClassName, true, classLoader);

                    if (!Module.class.isAssignableFrom(mainClass)) {
                        throw new PluginInvalidException("Main class must implement CPlugin interface");
                    }

                    CPlugin moduleInstance = (CPlugin) mainClass.getDeclaredConstructor().newInstance();
                    log.info("Module Classloader: {}", moduleInstance.getClass().getClassLoader());

                    try {
                        moduleInstance.setName(json.get("moduleName").getAsString());
                        moduleInstance.setDescription(json.get("description").getAsString());
                        moduleInstance.setVersion(json.get("version").getAsString());
                    } catch (NullPointerException ignored) {
                        log.error("It looks like the author of the Plugin {} forgot to add important information" +
                                " to their plugin.json. Please contact them for support.", name);
                        log.error("Plugin {} can't be loaded due to a fatal error while loading.", name);
                        return;
                    }

                    try {
                        JsonElement minAPI = json.get("minAPIVersion");
                        if (minAPI == null) log.warn("Plugin {} does not request a minimum API version. " +
                                "This is recommended, as the API may change. Please report any problems" +
                                " related to this module to the corresponding author(s).", moduleInstance.getName());
                        else {
                            moduleInstance.setMinAPIVersion(minAPI.getAsString());
                            ComparableVersion moduleVersion = new ComparableVersion(minAPI.getAsString());
                            ComparableVersion systemVersion = new ComparableVersion(Caesar.systemVersion);
                            if (systemVersion.compareTo(moduleVersion) > 0) log.warn("Plugin {} is using an older version of" +
                                    " GoP: {}, but the server is using {}. Expect weird things while using.",
                                    name, minAPI.getAsString(), Caesar.systemVersion);
                        }
                        moduleInstance.setStoresSensitiveData(json.get("storesSensitiveData").getAsBoolean());
                        moduleInstance.setUsesEncryption(json.get("usesEncryption").getAsBoolean());

                    } catch (NullPointerException ignored) {
                        log.error("The Plugin.json of {} provides some broken information. Please let the Author(s) " +
                                "correct them.", moduleInstance.getName());
                    }
                    moduleInstance.onLoad();
                    Caesar.getInstance().getRegistry().addPlugin(moduleInstance);
                    StringBuilder s = new StringBuilder();
                    for (CPlugin p : Caesar.getInstance().getRegistry().getPlugins()) {
                        s.append(p.getName()).append(", ");
                    }
                    s = new StringBuilder(s.substring(0, s.length() - 2));
                    log.info(s.toString());

                    File dataFolder = new File("data/" + moduleInstance.getName());
                    if (dataFolder.mkdir()) log.info("Created new data folder for {}.", moduleInstance.getName());
                    Caesar.getInstance().getRegistry().callEvent(
                            new Event("ServerPluginLoadEvent")
                                    .set("plugin", moduleInstance.getName())
                                    .set("description", moduleInstance.getDescription())
                                    .set("authors", moduleInstance.getAuthors())
                                    .set("version", moduleInstance.getVersion())
                                    .set("dataFolder", moduleInstance.getDataFolder())
                    );

                    moduleInstance.onDefineEvents();
                    moduleInstance.onCreateCommands();
                    moduleInstance.onEnable();
                }

            }
        } catch (Exception e) {
            log.error("Error while loading plugin {}.", name);
            log.error(e.getMessage());
            printStacktrace(e);
        }
    }

    public void enablePlugins() {
        for (CPlugin m : Caesar.getInstance().getRegistry().getPlugins()) {
            m.onCreateCommands();
            m.onDefineEvents();
            m.onEnable();
        }
    }

    public void unloadPlugin(String name) {
        log.info("Disabling {}...", name);
        Caesar.getInstance().getRegistry().getPlugin(name).onDisable();
        Caesar.getInstance().getRegistry().removePlugin(name);
    }

    private void printStacktrace(Exception e) {
        for (StackTraceElement a : e.getStackTrace()) {
            log.error(a.toString());
        }
    }
}