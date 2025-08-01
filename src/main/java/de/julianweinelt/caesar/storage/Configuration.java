package de.julianweinelt.caesar.storage;

import de.julianweinelt.caesar.ai.AIModel;
import de.julianweinelt.caesar.annotation.BetaFeature;
import de.julianweinelt.caesar.auth.PasswordConditions;
import de.julianweinelt.caesar.endpoint.CorporateDesign;
import de.julianweinelt.caesar.endpoint.minecraft.MCPluginEndpoint;
import de.julianweinelt.caesar.endpoint.minecraft.MEndpointCurseForge;
import de.julianweinelt.caesar.endpoint.minecraft.MEndpointModrinth;
import de.julianweinelt.caesar.endpoint.minecraft.MEndpointSpigot;
import de.julianweinelt.caesar.exceptions.InvalidConfigKeyException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Getter
@Setter
public class Configuration {
    public static Configuration getInstance() {
        return LocalStorage.getInstance().getData();
    }

    @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE)
    private final String _NOTE = "The values in this file should NOT be changed manually. Instead use the Caesar Client to manage them.";
    @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE)
    private final String _INFO_JWT = "These parameters are important for token issuing. Keep it secret at every time. If you think someone got it, change the secret IMMEDIATELY! It won't break your stored data but reconnect all clients.";
    private String jwtSecret;
    private String jwtIssuer;

    @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE)
    private final String _INFO_BETA = "Do not enable this in production environments.";
    private boolean enableBetaFeatures = false; // TODO: Add get/set
    @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE)
    private final String _INFO_LEGACY = "This enables legacy support, like for Waterfall or BungeeCord (not recommended in production use!)";
    private boolean enableLegacyMode = false;


    @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE)
    private final String _INFO_DB = "These options are important for saving data.";
    private String databaseType;
    private String databaseHost = "localhost";
    private String databaseName = "caesar";
    private String databaseUser = "caesar";
    private String databasePassword = "secret";
    private int databasePort = 3306;

    private String webServerHost = "127.0.0.1";
    private int webServerPort = 48000;
    private int chatServerPort = 48001;
    private int connectionServerPort = 48002;
    private int clientLinkPort = 48003;
    private int voiceServerPort = 48004;

    @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE)
    private final String _INFO_CN = "These fields define options for the usage of CloudNET.";
    private boolean cloudnetEnabled = false;
    private String cloudnetHost = "localhost";
    private String cloudnetUser = "admin";
    private String cloudnetPassword = "secret";

    @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE)
    private final String _INFO2 = "This value is defined in minutes.";
    private int tokenExpirationTime = 360*4; // Default: 24 hours

    @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE)
    private String _INFO_SECRET = "Changing this value will invalidate ALL connections in your system.";
    private String connectionAPISecret = "";

    @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE)
    private String _INFO_AI = "These settings are related to AI in chat. It won't be active if useChat is set to false.";
    private boolean useAIChat = false; // TODO: Add get/set
    private String chatAIAPISecret = ""; // TODO: Add get/set
    private AIModel chatAIModel = AIModel.GEMINI_2_5_FLASH; // TODO: Add get/set

    private PasswordConditions passwordConditions = new PasswordConditions();
    private boolean useDiscord = false;
    private CorporateDesign corporateDesign = CorporateDesign.DEFAULT;
    private boolean useCorporateDesign = false;

    private boolean useChat = false;
    @BetaFeature
    private boolean allowVoiceChat = false;
    private boolean allowPublicChats = false;
    private boolean useMailClient = false;
    private boolean useSupport = true;
    private boolean enableFileBrowser = false;

    @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE)
    private final String _INFO1 = "Here you can set custom API endpoints. Only change if you know what you are doing!";
    private String caesarAPIEndpoint = "https://api.caesarnet.cloud/";
    //private String caesarAPIEndpoint = "http://localhost:48009/";
    private boolean apiEndpointKeyRequired = false;

    @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE)
    private final String _INFO3 = "Defined Minecraft plugin endpoints are here.";
    private final List<MCPluginEndpoint> endpoints = new ArrayList<>
            (List.of(new MEndpointCurseForge(), new MEndpointSpigot(), new MEndpointModrinth()));
    private final long cacheExpiration = Duration.ofHours(12).toMillis();

    @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE)
    private final String _INFO_UPDATES = "Update logic";
    private boolean autoUpdateServerOnStartup = true;
    private boolean autoUpdateClients = true;
    private UpdateChannel updateChannel = UpdateChannel.STABLE;

    @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE)
    private final String _INFO_BACKUPS = "These values specify backup logics.";
    private boolean doAutoBackups = true;
    private int interval = 1;
    private ChronoUnit intervalType = ChronoUnit.DAYS;
    private BackupType backupType = BackupType.FULL;
    private AfterBackupAction afterBackupAction = AfterBackupAction.NOTHING;
    private BackupCompressType compressType = BackupCompressType.TAR;


    @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE)
    private final String _DO_NOT_CHANGE = "CHANGING THESE VALUES WILL BREAK YOUR SYSTEM!";
    @Setter(AccessLevel.NONE)
    private String languageVersion = "1.0.0";
    @Setter(AccessLevel.NONE)
    private String configVersion = "1.0.5";
    private String caesarVersion = "0.2.0";

    public void set(String key, Object value) {
        String[] readOnlyKeys = {
                "languageVersion",
                "configVersion",
                "caesarVersion",
        };
        boolean readOnly = Arrays.stream(readOnlyKeys).toList().contains(key) || key.startsWith("_");
        switch (key) {
            case "jwtSecret" -> jwtSecret = (String) value;
            case "jwtIssuer" -> jwtIssuer = (String) value;
            case "databaseType" -> databaseType = (String) value;
            case "databaseHost" -> databaseHost = (String) value;
            case "databaseName" -> databaseName = (String) value;
            case "databaseUser" -> databaseUser = (String) value;
            case "databasePassword" -> databasePassword = (String) value;
            case "databasePort" -> databasePort = (int) value;
            case "webServerHost" -> webServerHost = (String) value;
            case "webServerPort" -> webServerPort = (int) value;
            case "chatServerPort" -> chatServerPort = (int) value;
            case "connectionServerPort" -> connectionServerPort = (int) value;
            case "cloudnetEnabled" -> cloudnetEnabled = (boolean) value;
            case "cloudnetHost" -> cloudnetHost = (String) value;
            case "cloudnetUser" -> cloudnetUser = (String) value;
            case "cloudnetPassword" -> cloudnetPassword = (String) value;
            case "tokenExpirationTime" -> tokenExpirationTime = (int) value;
            case "connectionAPISecret" -> connectionAPISecret = (String) value;
            case "passwordConditions" -> passwordConditions = (PasswordConditions) value;
            case "useDiscord" -> useDiscord = (boolean) value;
            case "corporateDesign" -> corporateDesign = (CorporateDesign) value;
            case "useCorporateDesign" -> useCorporateDesign = (boolean) value;
            case "useChat" -> useChat = (boolean) value;
            case "allowVoiceChat" -> allowVoiceChat = (boolean) value;
            case "allowPublicChats" -> allowPublicChats = (boolean) value;
            case "useMailClient" -> useMailClient = (boolean) value;
            case "useSupport" -> useSupport = (boolean) value;
            case "enableFileBrowser" -> enableFileBrowser = (boolean) value;
            case "backupType" -> backupType = BackupType.valueOf((String) value);
            case "caesarAPIEndpoint" -> caesarAPIEndpoint = (String) value;
            case "caesarVersion" -> caesarVersion = (String) value;
            case "apiEndpointKeyRequired" -> apiEndpointKeyRequired = (boolean) value;
            case "afterBackupAction" -> afterBackupAction = AfterBackupAction.valueOf((String) value);
            case "updateChannel" -> updateChannel = UpdateChannel.valueOf((String) value);
            case "intervalType" -> intervalType = ChronoUnit.valueOf((String) value);
            case "compressType" -> compressType = BackupCompressType.valueOf((String) value);
            case "doAutoBackups" -> doAutoBackups = (boolean) value;
            case "autoUpdateClients" -> autoUpdateClients = (boolean) value;
            case "autoUpdateServerOnStartup" -> autoUpdateServerOnStartup = (boolean) value;
            default -> throw new InvalidConfigKeyException(key, readOnly);
        }
    }

    public Object get(String key) {
        String[] readOnlyKeys = {
                "languageVersion",
                "clientVersion",
        };
        boolean readOnly = Arrays.stream(readOnlyKeys).toList().contains(key);
        return switch (key) {
            case "jwtSecret" -> jwtSecret;
            case "jwtIssuer" -> jwtIssuer;
            case "databaseType" -> databaseType;
            case "databaseHost" -> databaseHost;
            case "databaseName" -> databaseName;
            case "databaseUser" -> databaseUser;
            case "databasePassword" -> databasePassword;
            case "databasePort" -> databasePort;
            case "webServerHost" -> webServerHost;
            case "webServerPort" -> webServerPort;
            case "chatServerPort" -> chatServerPort;
            case "connectionServerPort" -> connectionServerPort;
            case "cloudnetEnabled" -> cloudnetEnabled;
            case "cloudnetHost" -> cloudnetHost;
            case "cloudnetUser" -> cloudnetUser;
            case "cloudnetPassword" -> cloudnetPassword;
            case "tokenExpirationTime" -> tokenExpirationTime;
            case "connectionAPISecret" -> connectionAPISecret;
            case "passwordConditions" -> passwordConditions;
            case "useDiscord" -> useDiscord;
            case "corporateDesign" -> corporateDesign;
            case "useCorporateDesign" -> useCorporateDesign;
            case "useChat" -> useChat;
            case "allowVoiceChat" -> allowVoiceChat;
            case "allowPublicChats" -> allowPublicChats;
            case "useMailClient" -> useMailClient;
            case "useSupport" -> useSupport;
            case "enableFileBrowser" -> enableFileBrowser;
            case "backupType" -> backupType;
            case "caesarAPIEndpoint" -> caesarAPIEndpoint;
            case "caesarVersion" -> caesarVersion;
            case "apiEndpointKeyRequired" -> apiEndpointKeyRequired;
            case "afterBackupAction" -> afterBackupAction;
            case "updateChannel" -> updateChannel;
            case "intervalType" -> intervalType;
            case "compressType" -> compressType;
            case "doAutoBackups" -> doAutoBackups;
            case "autoUpdateClients" -> autoUpdateClients;
            case "autoUpdateServerOnStartup" -> autoUpdateServerOnStartup;
            default -> throw new InvalidConfigKeyException(key, readOnly);
        };
    }


    public enum ConfigValueType {
        STRING, INT, BOOLEAN, PASSWORD_CONDITIONS, CORPORATE_DESIGN, ONLINE_STATUS, UNKNOWN
    }
    public enum UpdateChannel {
        STABLE, BETA, ALPHA, NIGHTLY
    }

    public enum BackupType {
        FULL, INCREMENTAL, LOCAL_ONLY
    }
    public enum AfterBackupAction {
        NOTHING, UPLOAD_FTP
    }
    public enum BackupCompressType {
        ZIP, TAR
    }
}