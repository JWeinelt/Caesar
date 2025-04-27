package de.julianweinelt.caesar;

import de.julianweinelt.caesar.auth.CloudNETConnectionChecker;
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
import org.checkerframework.checker.units.qual.A;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
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
        localStorage.loadData();
        languageManager = new LanguageManager();
        List<String> availableLanguages = languageManager.getAvailableLanguagesFromServer();
        availableLanguages.forEach(languageManager::downloadLanguageIfMissing);
        languageManager.loadAllLanguageData();


        try {
            Terminal terminal = TerminalBuilder.builder().system(true).build();

            clearScreen();
            log.info("Hello!");

            systemLanguage = prompt(terminal, "setup.language", "en", getLanguageManager().getAvailableLanguagesFromServer());
            clearScreen();

            String hostName = prompt(terminal, "setup.hostname", InetAddress.getLocalHost().getHostAddress(), getAvailableHostNames());
            clearScreen();

            String port = prompt(terminal, "setup.port", "6565", List.of());
            clearScreen();

            databaseProcedure(terminal);
        } catch (IOException e) {
            log.error("Failed to start terminal: {}", e.getMessage());
            System.exit(1);
        }
    }


    public void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    private void databaseProcedure(Terminal terminal) {
        List<String> databases = new ArrayList<>();
        for (StorageFactory.StorageType type : StorageFactory.StorageType.values()) databases.add(type.name());
        String databaseType = prompt(terminal, "setup.database.type", "MYSQL",
                databases);
        StorageFactory.StorageType storageType = StorageFactory.StorageType.valueOf(databaseType.toUpperCase());
        clearScreen();

        String databaseHost = prompt(terminal, "setup.database.host",
                "localhost", List.of());
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
        storageFactory.getUsedStorage().connect();
        boolean success = storageFactory.getUsedStorage().connect();
        boolean hasTables = storageFactory.getUsedStorage().hasTables();
        if (hasTables) {
            log.warn(languageManager.getTranslation(systemLanguage, "setup.database.info.tables-already-exists"));
            databaseProcedure(terminal);
            return;
        }
        if (success) {
            localStorage.saveData();
            log.info(languageManager.getTranslation(systemLanguage, "setup.database.info.connected"));
            boolean usesCloudNET = checkBoolInput(prompt(terminal, "setup.cloudnet.uses", "false", getBooleans()));
            clearScreen();
            if (usesCloudNET) {
                cloudNETProcedure(terminal);
            } else finishSetup();
        } else databaseProcedure(terminal);
    }

    private void cloudNETProcedure(Terminal terminal) {
        String cloudnetHost = prompt(terminal, "setup.cloudnet.host", "localhost", List.of());
        clearScreen();
        int cloudnetPort = Integer.parseInt(prompt(terminal, "setup.cloudnet.port", "2812", List.of()));
        String cloudnetUsername = prompt(terminal, "setup.cloudnet.username", "SystemAdmin", List.of());
        clearScreen();
        String cloudnetPassword = prompt(terminal, "setup.cloudnet.password", "", List.of());
        clearScreen();
        boolean cloudnetUseSSL = checkBoolInput(prompt(terminal, "setup.cloudnet.ssl", "false", getBooleans()));
        clearScreen();
        int cPort = cloudnetUseSSL ? 443 : cloudnetPort;
        CloudNETConnectionChecker connectionChecker = new CloudNETConnectionChecker(cloudnetHost,
                cPort, cloudnetUsername, cloudnetPassword);
        if (cloudnetUseSSL) connectionChecker.withSSL();
        log.info(languageManager.getTranslation(systemLanguage, "setup.cloudnet.info.try-to-connect"));
        boolean cloudConnect = connectionChecker.checkConnection();
        if (cloudConnect) {
            localStorage.getSaveData().setCloudnetHost(cloudnetHost);
            localStorage.getSaveData().setCloudnetUser(cloudnetUsername);
            localStorage.getSaveData().setCloudnetPassword(cloudnetPassword);
            log.info(languageManager.getTranslation(systemLanguage, "setup.cloudnet.info.connected"));
            finishSetup();
        } else cloudNETProcedure(terminal);
    }

    private void finishSetup() {
        localStorage.saveData();
        log.info("Finishing database setup...");
        storageFactory.getUsedStorage().createTables();
        storageFactory.getUsedStorage().insertDefaultData();
        storageFactory.getUsedStorage().createAdminUser();
        log.info(languageManager.getTranslation(systemLanguage, "setup.info.user-created"));
        log.info(languageManager.getTranslation(systemLanguage, "setup.info.setup-finished"));
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
            prompt += "\nAvailable answers: ";
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
        if (!completions.contains(input) && !completions.isEmpty()) {
            log.error("Invalid input! Please try again.");
            return prompt(terminal, promptMessage, defaultValue, completions);
        }
        return input.trim();
    }

    public boolean checkBoolInput(String input) {
        return List.of("true", "yes", "on", "1").contains(input.toLowerCase());
    }

    public List<String> getAvailableHostNames() {
        List<String> availableHostNames = new java.util.ArrayList<>();
        try {
            InetAddress[] iaddress = InetAddress.getAllByName(InetAddress.getLocalHost().getHostName());
            for (InetAddress address : iaddress) {
                availableHostNames.add(address.getHostAddress());
            }
        } catch (Exception e) {
            log.error("Failed to get local host names: {}", e.getMessage());
        }
        availableHostNames.add("0.0.0.0");
        availableHostNames.add("localhost");
        return availableHostNames;
    }

    public List<String> getBooleans() {
        return List.of("true", "false", "yes", "no", "on", "off", "1", "0");
    }
}