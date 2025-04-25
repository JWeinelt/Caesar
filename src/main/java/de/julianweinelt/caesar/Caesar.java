package de.julianweinelt.caesar;

import de.julianweinelt.caesar.endpoint.CaesarServer;
import de.julianweinelt.caesar.endpoint.ChatServer;
import de.julianweinelt.caesar.endpoint.ConnectionServer;
import de.julianweinelt.caesar.plugin.PluginLoader;
import de.julianweinelt.caesar.plugin.Registry;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Caesar {
    @Getter
    private static Caesar instance;
    public static String systemVersion = "1.0";

    @Getter
    private Registry registry;
    @Getter
    private PluginLoader pluginLoader;

    @Getter
    private CaesarServer caesarServer;
    @Getter
    private ChatServer chatServer;
    @Getter
    private ConnectionServer connectionServer;

    public static void main(String[] args) {
        instance = new Caesar();
        instance.start();
    }

    public void start() {
        log.info("Welcome!");
        log.info("Starting Caesar v{}", systemVersion);
        registry = new Registry();
        log.info("Registering basic events...");
        registry.registerEvent("ServerStartupEvent");
        registry.registerEvent("ServerShutdownEvent");
        registry.registerEvent("PluginLoadEvent");
        registry.registerEvent("PluginEnableEvent");
        registry.registerEvent("PluginDisableEvent");
        log.info("Loading plugins...");
        pluginLoader = new PluginLoader(registry);
        log.info("Preparing plugin loading...");
        pluginLoader.prepareLoading();
        log.info("Loading plugins...");
        pluginLoader.loadPlugins();
        log.info("Enabling plugins...");
        pluginLoader.enablePlugins();
        log.info("Plugin loading complete.");
    }
}