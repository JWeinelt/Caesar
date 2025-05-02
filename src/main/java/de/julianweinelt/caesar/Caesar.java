package de.julianweinelt.caesar;

import de.julianweinelt.caesar.auth.CloudNETConnectionChecker;
import de.julianweinelt.caesar.auth.UserManager;
import de.julianweinelt.caesar.discord.DiscordBot;
import de.julianweinelt.caesar.endpoint.CaesarServer;
import de.julianweinelt.caesar.endpoint.ChatServer;
import de.julianweinelt.caesar.endpoint.ConnectionServer;
import de.julianweinelt.caesar.exceptions.ProblemLogger;
import de.julianweinelt.caesar.plugin.PluginLoader;
import de.julianweinelt.caesar.plugin.Registry;
import de.julianweinelt.caesar.storage.APIKeySaver;
import de.julianweinelt.caesar.storage.LocalStorage;
import de.julianweinelt.caesar.storage.StorageFactory;
import de.julianweinelt.caesar.util.JWTUtil;
import de.julianweinelt.caesar.util.LanguageManager;
import lombok.Getter;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class Caesar {
    private static final Logger log = LoggerFactory.getLogger(Caesar.class);

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
    private JWTUtil jwt;
    @Getter
    private APIKeySaver apiKeySaver;

    @Getter
    private StorageFactory storageFactory;

    @Getter
    private LanguageManager languageManager;

    @Getter
    private UserManager userManager;

    @Getter
    private ProblemLogger problemLogger;


    @Getter
    private DiscordBot discordBot;

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
        if (localStorage == null) {
            localStorage = new LocalStorage();
            localStorage.loadData();
            localStorage.loadConnections();
        }
        problemLogger = new ProblemLogger();
        apiKeySaver = new APIKeySaver();
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
        log.info("Starting endpoints...");
        caesarServer = new CaesarServer();
        chatServer = new ChatServer();
        connectionServer = new ConnectionServer();
        caesarServer.start();
        chatServer.start();
        connectionServer.start();
        log.info("Loading users from database...");
        userManager = new UserManager();
        userManager.overrideUsers(storageFactory.getUsedStorage().getAllUsers());
        log.info("Registered all available users ({}).", userManager.getUsers().size());
        log.info("Starting endpoints complete.");

        if (localStorage.getData().isUseDiscord()) {
            discordBot = new DiscordBot();
            discordBot.start();
        }

        log.info("Caesar has been started.");
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
            localStorage.getData().setWebServerHost(hostName);
            localStorage.getData().setWebServerPort(Integer.parseInt(port));

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
        localStorage.getData().setDatabaseType(storageType);
        localStorage.getData().setDatabaseHost(databaseHost);
        localStorage.getData().setDatabasePort(Integer.parseInt(databasePort));
        localStorage.getData().setDatabaseName(databaseName);
        localStorage.getData().setDatabaseUser(databaseUser);
        localStorage.getData().setDatabasePassword(databasePassword);
        storageFactory = new StorageFactory();
        storageFactory.provide(storageType, localStorage.getData());
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
            localStorage.getData().setCloudnetHost(cloudnetHost);
            localStorage.getData().setCloudnetUser(cloudnetUsername);
            localStorage.getData().setCloudnetPassword(cloudnetPassword);
            log.info(languageManager.getTranslation(systemLanguage, "setup.cloudnet.info.connected"));
            finishSetup();
        } else cloudNETProcedure(terminal);
    }

    private void finishSetup() {
        localStorage.saveData();
        log.info("Finishing database setup...");
        storageFactory.getUsedStorage().createTables();
        storageFactory.getUsedStorage().insertDefaultData();
        userManager.createUser("admin", "admin");
        log.info(languageManager.getTranslation(systemLanguage, "setup.info.user-created"));
        log.info(languageManager.getTranslation(systemLanguage, "setup.info.setup-finished"));
        jwt = new JWTUtil();
        localStorage.getData().setJwtSecret(jwt.generateSecret(20));
        localStorage.getData().setConnectionAPISecret(jwt.generateSecret(20));
        clearScreen();
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
            InetAddress[] addresses = InetAddress.getAllByName(InetAddress.getLocalHost().getHostName());
            for (InetAddress address : addresses) {
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