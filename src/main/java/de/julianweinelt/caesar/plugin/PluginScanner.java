package de.julianweinelt.caesar.plugin;

import com.google.gson.Gson;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

public class PluginScanner {

    private final File pluginFolder = new File("plugins");

    public List<PluginDescriptor> scan() {
        List<PluginDescriptor> descriptors = new ArrayList<>();
        if (!pluginFolder.exists()) pluginFolder.mkdirs();

        File[] files = pluginFolder.listFiles((dir, name) -> name.endsWith(".jar"));
        if (files == null) return descriptors;

        for (File jar : files) {
            try (JarFile jarFile = new JarFile(jar)) {
                ZipEntry entry = jarFile.getEntry("plugin.json");
                if (entry == null) continue;

                try (InputStream in = jarFile.getInputStream(entry)) {
                    String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                    PluginConfiguration config = new Gson().fromJson(json, PluginConfiguration.class);
                    URL url = jar.toURI().toURL();
                    descriptors.add(new PluginDescriptor(config.pluginName(), jar, url, config));
                }
            } catch (Exception e) {
                LoggerFactory.getLogger(PluginScanner.class).warn("Failed to read plugin: {}", jar.getName(), e);
            }
        }

        return descriptors;
    }
}
