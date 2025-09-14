package de.julianweinelt.caesar;

import com.vdurmont.semver4j.Semver;
import de.julianweinelt.caesar.ai.AIManager;
import de.julianweinelt.caesar.auth.CaesarLinkServer;
import de.julianweinelt.caesar.auth.CloudNETConnectionChecker;
import de.julianweinelt.caesar.auth.UserManager;
import de.julianweinelt.caesar.backup.BackupManager;
import de.julianweinelt.caesar.commands.CLICommand;
import de.julianweinelt.caesar.commands.CLITabCompleter;
import de.julianweinelt.caesar.commands.system.PluginCommand;
import de.julianweinelt.caesar.discord.DiscordBot;
import de.julianweinelt.caesar.discord.DiscordConfiguration;
import de.julianweinelt.caesar.discord.ticket.TicketManager;
import de.julianweinelt.caesar.endpoint.CaesarServer;
import de.julianweinelt.caesar.endpoint.CaesarServiceProvider;
import de.julianweinelt.caesar.endpoint.MinecraftUUIDFetcher;
import de.julianweinelt.caesar.endpoint.chat.ChatManager;
import de.julianweinelt.caesar.endpoint.chat.ChatServer;
import de.julianweinelt.caesar.endpoint.chat.voice.VoiceServer;
import de.julianweinelt.caesar.endpoint.client.CaesarClientLinkServer;
import de.julianweinelt.caesar.exceptions.logging.ProblemLogger;
import de.julianweinelt.caesar.plugin.PluginLoader;
import de.julianweinelt.caesar.plugin.Registry;
import de.julianweinelt.caesar.plugin.event.Event;
import de.julianweinelt.caesar.plugin.event.Priority;
import de.julianweinelt.caesar.plugin.event.Subscribe;
import de.julianweinelt.caesar.storage.*;
import de.julianweinelt.caesar.util.JWTUtil;
import de.julianweinelt.caesar.util.LanguageManager;
import io.javalin.util.JavalinBindException;
import lombok.Getter;
import lombok.Setter;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
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

@SuppressWarnings({"RedundantSuppression", "unused", "SpellCheckingInspection"})
public class Caesar {
    private static final Logger log = LoggerFactory.getLogger(Caesar.class);

    @Getter
    private static Caesar instance;
    public static String systemVersion = "0.2.0";

    private String systemLanguage = "en";

    @Getter
    private Registry registry = null;
    @Getter
    private PluginLoader pluginLoader = null;
    @Getter
    private LocalStorage localStorage = null;

    @Getter
    private AIManager aiManager = null;

    @Getter
    private CaesarServer caesarServer = null;
    @Getter
    private ChatServer chatServer = null;
    @Getter
    private CaesarLinkServer connectionServer = null;
    @Getter
    private CaesarClientLinkServer clientLinkServer = null;
    @Getter
    private VoiceServer voiceServer = null;

    @Getter
    private CaesarServiceProvider serviceProvider = null;

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
    @Getter
    private TicketManager ticketManager = null;
    @Getter
    private BackupManager backupManager;
    @Getter @Setter
    private DatabaseVersionManager dbVersionManager = null;

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
        apiKeySaver = new APIKeySaver();
        jwt = new JWTUtil();
        problemLogger = new ProblemLogger();
        log.info("Welcome!");
        log.info("Starting Caesar v{}", systemVersion);
        languageManager = new LanguageManager();
        registry = new Registry();
        log.info("Registering basic events...");
        registry.registerEvents(
                "BackupCreateEvent",
                "UserCreateEvent",
                "UserChangeEvent",
                "UserDeleteEvent",
                "ChatCreateEvent",
                "ChatDeleteEvent",
                "ChatAddUserEvent",
                "ChatRemoveUserEvent",
                "StorageReadyEvent",
                "ServerStartupEvent",
                "ServerShutdownEvent",
                "ConfigChangeEvent",
                "DatabaseConnectedEvent",
                "DatabaseDisconnectedEvent",
                "DatabaseTablesCreateEvent",
                "VersionDataGetEvent"
        );
        LocalStorage.getInstance().checkConnectionKeys();
        pluginLoader = new PluginLoader(registry);
        log.info("Starting backup service...");
        backupManager = new BackupManager();
        backupManager.configure(localStorage.getData());
        log.info("Loading Caesar server plugins...");
        pluginLoader.loadAll();
        log.info("Plugin loading complete.");

        if (localStorage.getData().isUseDiscord()) {
            discordBot = new DiscordBot();
            ticketManager = new TicketManager();

            registry.registerListener(this, Registry.getInstance().getSystemPlugin());
        }
        Registry.getInstance().callEvent(new Event("StorageReadyEvent"));

        log.info("Starting endpoints...");
        if (chatManager == null && localStorage.getData().isUseChat()) chatManager = new ChatManager();
        if (chatServer == null && localStorage.getData().isUseChat()) chatServer = new ChatServer(chatManager);
        if (caesarServer == null) caesarServer = new CaesarServer();
        if (connectionServer == null) connectionServer = new CaesarLinkServer(LocalStorage.getInstance().getData().isShouldEncryptLinkConnections());
        if (clientLinkServer == null) clientLinkServer = new CaesarClientLinkServer(localStorage.getData().getClientLinkPort());
        if (storageFactory == null) storageFactory = new StorageFactory();
        if (voiceServer == null && localStorage.getData().isAllowVoiceChat()) {
            try {
                voiceServer = new VoiceServer(localStorage.getData().getVoiceServerPort());
            } catch (Exception ex) {
                log.error("Failed to load voice server", ex);
            }
        }
        if (localStorage.getData().isUseAIChat() && !localStorage.getData().getChatAIAPISecret().isEmpty()) {
            aiManager = new AIManager();
        }
        userManager = new UserManager();
        serviceProvider = new CaesarServiceProvider();
        serviceProvider.start();

        log.info("Connecting to database...");
        storageFactory.provide(StorageType.get(localStorage.getData().getDatabaseType()), localStorage.getData());
        boolean success = storageFactory.connect();
        if (!success) log.error("Failed to connect to database!");
        else {
            log.info("Performing checks...");

            if (localStorage.getData().getCaesarVersion() == null
                    || new Semver(localStorage.getData().getCaesarVersion()).isLowerThan(systemVersion)) {
                log.info("Performing update to Caesar v{}...", systemVersion);
                dbVersionManager.startDownload(systemVersion);
                localStorage.getData().setCaesarVersion(systemVersion);
                localStorage.saveData();
            }
        }
        try {
            caesarServer.start();
            if (localStorage.getData().isUseChat()) {
                chatServer.start();
            }
            connectionServer.start();
            clientLinkServer.start();
            if (localStorage.getData().isAllowVoiceChat()) {
                log.warn("WARNING: You are using a BETA feature. DO NOT run this in any productive environment!!");
                voiceServer.start();
            }
        } catch (JavalinBindException e) {
            log.error("Failed to start Caesar web server: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Failed to start endpoints: {}", e.getMessage());
        }
        log.info("Registered all available users ({}).", userManager.getUsers().size());
        log.info("Starting endpoints complete.");

        log.info("Registering system commands...");
        registerSystemCommands();

        log.info("Caesar has been started.");
        startCLI();

        MinecraftUUIDFetcher.prepareCacheDirectory();
        MinecraftUUIDFetcher.loadCache();
    }
    public void startCLI() {
        try {
            Terminal terminal = TerminalBuilder.builder().system(true).build();

            Completer completer = (reader, line, candidates) -> {
                List<String> words = line.words();
                String currentWord = line.word();

                if (words.isEmpty()) {
                    for (CLICommand cmd : getRegistry().getCommands()) {
                        candidates.add(new Candidate(cmd.getName()));
                        cmd.getAliases().forEach(alias -> candidates.add(new Candidate(alias)));
                    }
                    return;
                }

                String commandName = words.get(0);
                CLICommand matching = getRegistry().getCommands().stream()
                        .filter(cmd -> cmd.getName().equalsIgnoreCase(commandName) ||
                                cmd.getAliases().stream().anyMatch(a -> a.equalsIgnoreCase(commandName)))
                        .findFirst()
                        .orElse(null);

                if (matching == null) {
                    for (CLICommand cmd : getRegistry().getCommands()) {
                        if (cmd.getName().toLowerCase().startsWith(currentWord.toLowerCase())) {
                            candidates.add(new Candidate(cmd.getName()));
                        }
                        for (String alias : cmd.getAliases()) {
                            if (alias.toLowerCase().startsWith(currentWord.toLowerCase())) {
                                candidates.add(new Candidate(alias));
                            }
                        }
                    }
                    return;
                }

                String[] args = words.subList(1, words.size()).toArray(new String[0]);
                CLITabCompleter tabCompleter = matching.getTabCompleter();
                if (tabCompleter != null) {
                    for (String suggestion : tabCompleter.onTabCompletion(args)) {
                        if (suggestion.toLowerCase().startsWith(currentWord.toLowerCase())) {
                            candidates.add(new Candidate(suggestion));
                        }
                    }
                }
            };


            LineReader reader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .completer(completer)
                    .build();

            reader.setOpt(LineReader.Option.AUTO_LIST);
            reader.setOpt(LineReader.Option.MOUSE);

            while (true) {
                String input = reader.readLine("> ");
                if (input == null || input.trim().equalsIgnoreCase("exit")) break;

                String[] args = input.split(" ");

                boolean found = false;
                for (CLICommand cmd : getRegistry().getCommands()) {
                    if (cmd.getName().equalsIgnoreCase(args[0]) ||
                            cmd.getAliases().stream().anyMatch(a -> a.equalsIgnoreCase(args[0]))) {
                        cmd.execute(args);
                        log.debug("Executed command: {}", cmd.getName());
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    System.out.println("Unknown command: " + args[0]);
                }
            }

        } catch (IOException e) {
            log.error("Failed to start terminal CLI: {}", e.getMessage());
        }
    }

    public void registerSystemCommands() {
        getRegistry().registerCommand(
                new CLICommand("stop")
                        .aliases("exit", "quit", "shutdown")
                        .executor((label, args) -> Caesar.this.shutdown())
        );
        getRegistry().registerCommand(new CLICommand("plugins").executor(new PluginCommand()));
    }

    public void startFirstStartup() {
        localStorage = new LocalStorage();
        localStorage.loadData();
        languageManager = new LanguageManager();
        storageFactory = new StorageFactory();
        List<String> availableLanguages = languageManager.getAvailableLanguagesFromServer();
        availableLanguages.forEach(languageManager::downloadLanguageIfMissing);
        languageManager.loadAllLanguageData();

        ticketManager = new TicketManager();


        try {
            Terminal terminal = TerminalBuilder.builder().system(true).build();

            clearScreen();
            log.info("Hello!");

            systemLanguage = prompt(terminal, "setup.language", "en", getLanguageManager().getAvailableLanguagesFromServer());
            clearScreen();

            String hostName = prompt(terminal, "setup.hostname", InetAddress.getLocalHost().getHostAddress(), getAvailableHostNames());
            localStorage.getData().setWebServerHost(hostName);
            clearScreen();

            String port = prompt(terminal, "setup.port", localStorage.getData().getWebServerPort() + "", List.of());
            localStorage.getData().setWebServerPort(Integer.parseInt(port));
            clearScreen();
            boolean useChatServer = checkBoolInput(prompt(terminal, "setup.chat.use", "yes", getBooleans()));
            clearScreen();
            if (useChatServer) {
                localStorage.getData().setUseChat(true);
                String portChatServer = prompt(terminal, "setup.chat.port", localStorage.getData().getChatServerPort() + "", List.of());
                localStorage.getData().setChatServerPort(Integer.parseInt(portChatServer));
                clearScreen();
            }
            int connectionServerPort = Integer.parseInt(prompt(terminal, "setup.mc-conn.port",
                    localStorage.getData().getConnectionServerPort() + "", List.of()));
            localStorage.getData().setConnectionServerPort(connectionServerPort);
            clearScreen();
            int caesarClientLinkPort = Integer.parseInt(prompt(terminal, "setup.caesar-client-link.port",
                    localStorage.getData().getClientLinkPort() + "", List.of()));
            localStorage.getData().setClientLinkPort(caesarClientLinkPort);
            clearScreen();

            userManager = new UserManager();

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
        for (StorageType type : StorageType.values()) databases.add(type.getName());
        String databaseType = prompt(terminal, "setup.database.type", "MYSQL",
                databases);
        StorageType storageType = StorageType.get(databaseType);
        clearScreen();

        String databaseHost = prompt(terminal, "setup.database.host",
                "localhost", List.of());
        clearScreen();

        String databasePort = "0";
        String defaultDatabasePort = "" + storageType.getDefaultPort();
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
        localStorage.getData().setDatabaseType(databaseType);
        localStorage.getData().setDatabaseHost(databaseHost);
        localStorage.getData().setDatabasePort(Integer.parseInt(databasePort));
        localStorage.getData().setDatabaseName(databaseName);
        localStorage.getData().setDatabaseUser(databaseUser);
        localStorage.getData().setDatabasePassword(databasePassword);
        storageFactory = new StorageFactory();
        storageFactory.provide(storageType, localStorage.getData());
        boolean success = storageFactory.getUsedStorage().connect();
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
        Storage s = storageFactory.getUsedStorage();
        DatabaseVersionManager.getInstance().downloadVersion(systemVersion);
        if (!s.systemDataExist()) s.insertDefaultData();

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

    public byte[] generateIV() {
        byte[] iv = new byte[16];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        return iv;
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
        chatManager.terminate();
        try {
            chatServer.stop();
            caesarServer.stop();
            connectionServer.stop();
            clientLinkServer.stop();
            storageFactory.getUsedStorage().disconnect();
            if (discordBot != null) discordBot.stop();
            if (voiceServer != null) voiceServer.stop();
        } catch (InterruptedException e) {
            log.error("Failed to stop endpoints: {}", e.getMessage());
        }
        log.info("Caesar has been stopped.");
    }

    public List<String> getBooleans() {
        return List.of("true", "false", "yes", "no", "on", "off", "1", "0");
    }

    @Subscribe(value = "StorageReadyEvent", priority = Priority.HIGH)
    public void onStorageReady(Event event) {
        DiscordConfiguration dc = LocalStorage.getInstance().load("discord", DiscordConfiguration.class);
        if (dc == null) return;
        if (dc.isUseTicketSystem()) {
            log.info("Initializing ticket manager...");
            ticketManager = new TicketManager();
        } else {
            log.info("Ticket system is not activated.");
        }
    }
}