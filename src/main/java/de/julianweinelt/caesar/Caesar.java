package de.julianweinelt.caesar;

import de.julianweinelt.caesar.endpoint.CaesarServer;
import de.julianweinelt.caesar.endpoint.ChatServer;
import de.julianweinelt.caesar.endpoint.ConnectionServer;
import de.julianweinelt.caesar.plugin.PluginLoader;
import de.julianweinelt.caesar.plugin.Registry;
import de.julianweinelt.caesar.storage.LocalStorage;
import de.julianweinelt.caesar.util.LanguageManager;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.List;

@Slf4j
public class Caesar {
    @Getter
    private static Caesar instance;
    public static String systemVersion = "1.0";

    private String systemLanguage = "en";

    @Getter
    private Registry registry;
    @Getter
    private PluginLoader pluginLoader;
    @Getter
    private LocalStorage localStorage;

    @Getter
    private CaesarServer caesarServer;
    @Getter
    private ChatServer chatServer;
    @Getter
    private ConnectionServer connectionServer;

    @Getter
    private LanguageManager languageManager;

    public static void main(String[] args) {
        instance = new Caesar();
        File configFile = new File("config.json");
        if (!configFile.exists()) {
            instance.startFirstStartup();
        } else {
            instance.start();
        }
    }

    public void start() {
        log.info("Welcome!");
        log.info("Starting Caesar v{}", systemVersion);
        languageManager = new LanguageManager();
        registry = new Registry();
        log.info("Registering basic events...");
        registry.registerEvent("ServerStartupEvent");
        registry.registerEvent("ServerShutdownEvent");
        registry.registerEvent("PluginLoadEvent");
        registry.registerEvent("PluginEnableEvent");
        registry.registerEvent("PluginDisableEvent");
        pluginLoader = new PluginLoader(registry);
        log.info("Preparing plugin loading...");
        pluginLoader.prepareLoading();
        log.info("Loading plugins...");
        pluginLoader.loadPlugins();
        log.info("Enabling plugins...");
        pluginLoader.enablePlugins();
        log.info("Plugin loading complete.");
    }

    public void startFirstStartup() {
        localStorage.loadData();
        languageManager = new LanguageManager();
        languageManager.downloadLanguageIfMissing("en");


        try {
            Terminal terminal = TerminalBuilder.builder().system(true).build();

            clearScreen();
            log.info("Hello!");

            systemLanguage = prompt(terminal, "setup.language", "en", List.of("en", "de", "fr"));
            clearScreen();

            String hostName = prompt(terminal, "setup.hostname", InetAddress.getLocalHost().getHostAddress(), List.of());

            clearScreen();

            String port = prompt(terminal, "setup.port", "6565", List.of("6565", "8080", "443"));
        } catch (IOException e) {
            log.error("Failed to start terminal: {}", e.getMessage());
            System.exit(1);
        }
    }



    public void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    private String prompt(Terminal terminal, String promptMessage, String defaultValue, List<String> completions) {
        String prompt = languageManager.getTranslation(systemLanguage, promptMessage);
        if (defaultValue != null && !defaultValue.isEmpty()) {
            prompt += " [" + defaultValue + "]";
        }
        prompt += ": ";

        LineReaderBuilder readerBuilder = LineReaderBuilder.builder()
                .terminal(terminal);

        if (completions != null && !completions.isEmpty()) {
            readerBuilder.completer(new StringsCompleter(completions));
        }

        LineReader reader = readerBuilder.build();
        reader.setOpt(LineReader.Option.AUTO_LIST);
        reader.setOpt(LineReader.Option.MOUSE);

        String input = reader.readLine(prompt);
        if (input == null || input.trim().isEmpty()) {
            return defaultValue;
        }
        return input.trim();
    }

}