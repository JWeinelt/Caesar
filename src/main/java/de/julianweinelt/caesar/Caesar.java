package de.julianweinelt.caesar;

import de.julianweinelt.caesar.endpoint.CaesarServer;
import de.julianweinelt.caesar.endpoint.ChatServer;
import de.julianweinelt.caesar.endpoint.ConnectionServer;
import de.julianweinelt.caesar.plugin.PluginLoader;
import de.julianweinelt.caesar.plugin.Registry;
import de.julianweinelt.caesar.storage.LocalStorage;
import de.julianweinelt.caesar.storage.StorageFactory;
import de.julianweinelt.caesar.util.LanguageManager;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Collections;
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
    private StorageFactory storageFactory;

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
        localStorage = new LocalStorage();
        localStorage.loadData();
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
        localStorage = new LocalStorage();
        //localStorage.loadData();
        languageManager = new LanguageManager();
        List<String> availableLanguages = languageManager.getAvailableLanguagesFromServer();
        availableLanguages.forEach(languageManager::downloadLanguageIfMissing);
        languageManager.loadAllLanguageData();


        try {
            Terminal terminal = TerminalBuilder.builder().system(true).build();

            clearScreen();
            log.info("Hello!");

            systemLanguage = prompt(terminal, "setup.language", "en", List.of("en", "de", "fr"));
            clearScreen();

            String hostName = prompt(terminal, "setup.hostname", InetAddress.getLocalHost().getHostAddress(), List.of());
            clearScreen();

            String port = prompt(terminal, "setup.port", "6565", List.of("6565", "8080", "443"));
            clearScreen();

            String databaseType = prompt(terminal, "setup.database.type", "sqlite",
                    List.of(Arrays.toString(StorageFactory.StorageType.values())));
            //TODO: Input validation
            StorageFactory.StorageType storageType = StorageFactory.StorageType.valueOf(databaseType.toUpperCase());
            clearScreen();

            String databaseHost = prompt(terminal, "setup.database.host",
                    "localhost", Collections.singletonList(InetAddress.getLocalHost().getHostAddress()));
            clearScreen();

            String databasePort = "0";
            String defaultDatabasePort = "" + storageType.port;
            if (!defaultDatabasePort.equals("0")) {
                databasePort = prompt(terminal, "setup.database.port", defaultDatabasePort, List.of());
                clearScreen();
            }

            String databaseName = prompt(terminal, "setup.database.schema", "caesar", List.of());
            clearScreen();
            String databaseUser = prompt(terminal, "setup.database.user", "root", List.of());
            clearScreen();
            String databasePassword = prompt(terminal, "setup.database.password", "", List.of());
            clearScreen();
            log.info(languageManager.getTranslation(systemLanguage, "setup.database.info.try-to-connect"));
            localStorage.getSaveData().setDatabaseType(storageType);
            localStorage.getSaveData().setDatabaseHost(databaseHost);
            localStorage.getSaveData().setDatabasePort(Integer.parseInt(databasePort));
            localStorage.getSaveData().setDatabaseName(databaseName);
            localStorage.getSaveData().setDatabaseUser(databaseUser);
            localStorage.getSaveData().setDatabasePassword(databasePassword);
            storageFactory = new StorageFactory();
            storageFactory.provide(storageType, localStorage.getSaveData());
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
        StringBuilder completionMessage = new StringBuilder();
        int idx = 0;
        for (String completion : completions) {
            completionMessage.append(completion);
            if (!(idx == completions.size()-1))
                completionMessage.append("; ");
            idx++;
        }

        String prompt = languageManager.getTranslation(systemLanguage, promptMessage);
        if (defaultValue != null && !defaultValue.isEmpty()) {
            prompt += " [" + defaultValue + "]";
        }

        if (!completionMessage.toString().isEmpty()) {
            prompt += "{" + completionMessage + "}";
        }
        prompt += ": ";

        LineReaderBuilder readerBuilder = LineReaderBuilder.builder()
                .terminal(terminal);

        if (!completions.isEmpty()) {
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