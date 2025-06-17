package de.julianweinelt.caesar;

import de.julianweinelt.caesar.auth.CaesarLinkServer;
import de.julianweinelt.caesar.auth.CloudNETConnectionChecker;
import de.julianweinelt.caesar.auth.UserManager;
import de.julianweinelt.caesar.commands.CLICommand;
import de.julianweinelt.caesar.discord.DiscordBot;
import de.julianweinelt.caesar.endpoint.CaesarServer;
import de.julianweinelt.caesar.endpoint.chat.ChatManager;
import de.julianweinelt.caesar.endpoint.chat.ChatServer;
import de.julianweinelt.caesar.endpoint.client.CaesarClientLinkServer;
import de.julianweinelt.caesar.exceptions.logging.ProblemLogger;
import de.julianweinelt.caesar.plugin.PluginLoader;
import de.julianweinelt.caesar.plugin.Registry;
import de.julianweinelt.caesar.plugin.event.Event;
import de.julianweinelt.caesar.storage.APIKeySaver;
import de.julianweinelt.caesar.storage.LocalStorage;
import de.julianweinelt.caesar.storage.StorageFactory;
import de.julianweinelt.caesar.util.JWTUtil;
import de.julianweinelt.caesar.util.LanguageManager;
import io.javalin.util.JavalinBindException;
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
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public class Caesar {
    private static final Logger log = LoggerFactory.getLogger(Caesar.class);

    @Getter
    private static Caesar instance;
    public static String systemVersion = "1.0";

    private String systemLanguage = "en";

    @Getter
    private Registry registry = null;
    @Getter
    private PluginLoader pluginLoader = null;
    @Getter
    private LocalStorage localStorage = null;

    @Getter
    private CaesarServer caesarServer = null;
    @Getter
    private ChatServer chatServer = null;
    @Getter
    private CaesarLinkServer connectionServer = null;
    @Getter
    private CaesarClientLinkServer clientLinkServer = null;

    @Getter
    private ChatManager chatManager;

    @Getter
    private JWTUtil jwt = null;
    @Getter
    private APIKeySaver apiKeySaver = null;

    @Getter
    private StorageFactory storageFactory = null;

    @Getter
    private LanguageManager languageManager = null;

    @Getter
    private UserManager userManager = null;

    @Getter
    private ProblemLogger problemLogger = null;


    @Getter
    private DiscordBot discordBot = null;

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
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));

        if (localStorage == null) {
            localStorage = new LocalStorage();
            localStorage.loadData();
            localStorage.loadConnections();
        }
        jwt = new JWTUtil();
        problemLogger = new ProblemLogger();
        apiKeySaver = new APIKeySaver();
        log.info("Welcome!");
        log.info("Starting Caesar v{}", systemVersion);
        languageManager = new LanguageManager();
        registry = new Registry();
        log.info("Registering basic events...");
        registry.registerEvent("StorageReadyEvent");
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
        if (chatManager == null) chatManager = new ChatManager();
        if (caesarServer == null) caesarServer = new CaesarServer();
        if (chatServer == null) chatServer = new ChatServer(chatManager);
        if (connectionServer == null) connectionServer = new CaesarLinkServer();
        if (clientLinkServer == null) clientLinkServer = new CaesarClientLinkServer(localStorage.getData().getClientLinkPort());
        if (storageFactory == null) storageFactory = new StorageFactory();
        userManager = new UserManager();
        log.info("Connecting to database...");
        storageFactory.provide(localStorage.getData().getDatabaseType(), localStorage.getData());
        boolean success = storageFactory.connect();
        if (!success) log.error("Failed to connect to database!");
        chatManager.setServer(chatServer);
        try {
            caesarServer.start();
            chatServer.start();
            connectionServer.start();
            clientLinkServer.start();
        } catch (JavalinBindException e) {
            log.error("Failed to start Caesar web server: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Failed to start endpoints: {}", e.getMessage());
        }
        log.info("Registered all available users ({}).", userManager.getUsers().size());
        log.info("Starting endpoints complete.");

        if (localStorage.getData().isUseDiscord()) {
            discordBot = new DiscordBot();
        }

        registry.callEvent(new Event("StorageReadyEvent"));

        log.info("Registering system commands...");
        registerSystemCommands();

        log.info("Caesar has been started.");
        startCLI();
    }

    public void startCLI() {
        try {
            Terminal terminal = TerminalBuilder.builder().system(true).build();

            LineReaderBuilder readerBuilder = LineReaderBuilder.builder()
                    .terminal(terminal);

            LineReader reader = readerBuilder.build();
            reader.setOpt(LineReader.Option.AUTO_LIST);
            reader.setOpt(LineReader.Option.MOUSE);

            String input = reader.readLine();

            String[] args = input.split(" ");

            for (CLICommand cmd : getRegistry().getCommands())
                if (cmd.getName().equalsIgnoreCase(args[0]) || cmd.getAliases().contains(args[0])) {
                    cmd.execute(args);
                    log.info("Executed command: {}", cmd.getName());
                }
        } catch (IOException e) {
            log.error("Failed to start terminal: {}", e.getMessage());
        }
    }

    public void registerSystemCommands() {
        getRegistry().registerCommand(
                new CLICommand("stop")
                        .aliases("exit", "quit", "shutdown")
                        .executor((label, args) -> Caesar.this.shutdown())
        );
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

            String port = prompt(terminal, "setup.port", "49850", List.of());
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
        userManager = new UserManager();
        localStorage.saveData();
        log.info("Finishing database setup...");
        storageFactory.getUsedStorage().createTables();
        storageFactory.getUsedStorage().insertDefaultData();
        userManager.createUser("admin", "admin");
        log.info(languageManager.getTranslation(systemLanguage, "setup.info.user-created"));
        log.info(languageManager.getTranslation(systemLanguage, "setup.info.setup-finished"));
        localStorage.getData().setJwtSecret(generateSecret(20));
        localStorage.getData().setConnectionAPISecret(generateSecret(20));
        jwt = new JWTUtil();
        localStorage.saveData();
        caesarServer = new CaesarServer(true);
        clearScreen();
        start();
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
            prompt += completionMessage;
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

    public String generateSecret(int length) {
        SecureRandom random = new SecureRandom();

        StringBuilder characterPool = new StringBuilder();
        String LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        characterPool.append(LETTERS);
        String DIGITS = "0123456789";
        characterPool.append(DIGITS);
        String SYMBOLS = "!@#$%^&*()-_=+[]{}|;:,.<>?";
        characterPool.append(SYMBOLS);

        StringBuilder result = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(characterPool.length());
            result.append(characterPool.charAt(index));
        }

        return result.toString();
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

    public void shutdown() {
        log.info("Caesar is shutting down...");
        localStorage.saveData();
        localStorage.saveConnections();
        log.info("Stopping endpoints...");
        try {
            chatServer.stop();
            caesarServer.stop();
            connectionServer.stop();
            storageFactory.getUsedStorage().disconnect();
            if (discordBot != null) discordBot.stop();
        } catch (InterruptedException e) {
            log.error("Failed to stop endpoints: {}", e.getMessage());
        }
        log.info("Caesar has been stopped.");
        Runtime.getRuntime().halt(0);
        System.exit(0);
    }

    public List<String> getBooleans() {
        return List.of("true", "false", "yes", "no", "on", "off", "1", "0");
    }
}