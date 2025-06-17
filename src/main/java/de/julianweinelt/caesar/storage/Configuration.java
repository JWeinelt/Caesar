package de.julianweinelt.caesar.storage;

import de.julianweinelt.caesar.auth.PasswordConditions;
import de.julianweinelt.caesar.endpoint.CorporateDesign;
import de.julianweinelt.caesar.endpoint.minecraft.MCPluginEndpoint;
import de.julianweinelt.caesar.endpoint.minecraft.MEndpointCurseForge;
import de.julianweinelt.caesar.endpoint.minecraft.MEndpointModrinth;
import de.julianweinelt.caesar.endpoint.minecraft.MEndpointSpigot;
import de.julianweinelt.caesar.exceptions.InvalidConfigKeyException;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.OnlineStatus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Getter
@Setter
public class Configuration {
    public static Configuration getInstance() {
        return LocalStorage.getInstance().getData();
    }

    private final String _NOTE = "The values in this should NOT be changed manually. Instead use the Caesar Client to change them.";
    private final String _INFO_JWT = "These parameters are important for token issuing.";
    private String jwtSecret;
    private String jwtIssuer;

    private final String _INFO_DB = "These options are important for saving data.";
    private StorageFactory.StorageType databaseType;
    private String databaseHost;
    private String databaseName;
    private String databaseUser;
    private String databasePassword;
    private int databasePort;

    private String webServerHost;
    private int webServerPort = 48000;
    private int chatServerPort = 48001;
    private int connectionServerPort = 48002;
    private int clientLinkPort = 48003;

    private String discordBotToken = "SECRET";
    private OnlineStatus defaultOnlineStatus;

    private final String _INFO_CN = "These fields define options for the usage of CloudNET.";
    private boolean cloudnetEnabled;
    private String cloudnetHost = "localhost";
    private String cloudnetUser = "admin";
    private String cloudnetPassword = "secret";

    private final String _INFO2 = "This value is defined in minutes.";
    private int tokenExpirationTime = 360*4; // Default: 24 hours

    private String connectionAPISecret;

    private PasswordConditions passwordConditions = new PasswordConditions();
    private boolean useDiscord = false;
    private CorporateDesign corporateDesign = CorporateDesign.DEFAULT;
    private boolean useCorporateDesign = false;

    private boolean useChat = false;
    private boolean allowVoiceChat = false;
    private boolean allowPublicChats = false;

    private final String _INFO1 = "Here you can set custom API endpoints. Only change if you know what you are doing!";
    private String caesarAPIEndpoint = "https://api.caesarnet.cloud/";

    private final String _INFO3 = "Defined Minecraft plugin endpoints are here.";
    private final List<MCPluginEndpoint> endpoints = new ArrayList<MCPluginEndpoint>
            (List.of(new MEndpointCurseForge(), new MEndpointSpigot(), new MEndpointModrinth()));


    private final String _DO_NOT_CHANGE = "CHANGING THESE VALUES MAY BREAK YOUR SYSTEM!";
    private String languageVersion = "1.0.0";
    private String clientVersion = "1.0.0";
    
    public void set(String key, Object value) {
        String[] readOnlyKeys = {
                "languageVersion",
                "clientVersion",
        };
        boolean readOnly = Arrays.stream(readOnlyKeys).toList().contains(key);
        switch (key) {
            case "jwtSecret" -> jwtSecret = (String) value;
            case "jwtIssuer" -> jwtIssuer = (String) value;
            case "databaseType" -> databaseType = StorageFactory.StorageType.valueOf((String) value);
            case "databaseHost" -> databaseHost = (String) value;
            case "databaseName" -> databaseName = (String) value;
            case "databaseUser" -> databaseUser = (String) value;
            case "databasePassword" -> databasePassword = (String) value;
            case "databasePort" -> databasePort = (int) value;
            case "webServerHost" -> webServerHost = (String) value;
            case "webServerPort" -> webServerPort = (int) value;
            case "chatServerPort" -> chatServerPort = (int) value;
            case "connectionServerPort" -> connectionServerPort = (int) value;
            case "discordBotToken" -> discordBotToken = (String) value;
            case "defaultOnlineStatus" -> defaultOnlineStatus = OnlineStatus.valueOf((String) value);
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
            default -> {
                throw new InvalidConfigKeyException(key, readOnly);
            }
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
            case "discordBotToken" -> discordBotToken;
            case "defaultOnlineStatus" -> defaultOnlineStatus;
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
            default -> throw new InvalidConfigKeyException(key, readOnly);
        };
    }


    public enum ConfigValueType {
        STRING, INT, BOOLEAN, PASSWORD_CONDITIONS, CORPORATE_DESIGN, ONLINE_STATUS, UNKNOWN
    }
}