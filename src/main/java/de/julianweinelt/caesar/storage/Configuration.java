package de.julianweinelt.caesar.storage;

import de.julianweinelt.caesar.auth.PasswordConditions;
import de.julianweinelt.caesar.endpoint.CorporateDesign;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.OnlineStatus;

@Getter
@Setter
public class Configuration {
    private String jwtSecret;
    private String jwtIssuer;

    private StorageFactory.StorageType databaseType;
    private String databaseHost;
    private String databaseName;
    private String databaseUser;
    private String databasePassword;
    private int databasePort;

    private String webServerHost;
    private int webServerPort;
    private int chatServerPort = 49801;
    private int connectionServerPort = 49802;

    private String discordBotToken = "SECRET";
    private OnlineStatus defaultOnlineStatus;

    private boolean cloudnetEnabled;
    private String cloudnetHost = "localhost";
    private String cloudnetUser = "admin";
    private String cloudnetPassword = "secret";

    private String languageVersion = "1.0.0";
    private String clientVersion = "1.0.0";

    private int tokenExpirationTime = 360*4; // Default: 24 hours

    private String connectionAPISecret;

    private PasswordConditions passwordConditions = new PasswordConditions();
    private boolean useDiscord = false;
    private CorporateDesign corporateDesign = CorporateDesign.DEFAULT;
    private boolean useCorporateDesign = false;

    private boolean useChat = false;
    private boolean allowVoiceChat = false;
    private boolean allowPublicChats = false;
    
    public void set(String key, Object value) {
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
            case "languageVersion" -> languageVersion = (String) value;
            case "clientVersion" -> clientVersion = (String) value;
            case "tokenExpirationTime" -> tokenExpirationTime = (int) value;
            case "connectionAPISecret" -> connectionAPISecret = (String) value;
            case "passwordConditions" -> passwordConditions = (PasswordConditions) value;
            case "useDiscord" -> useDiscord = (boolean) value;
            case "corporateDesign" -> corporateDesign = (CorporateDesign) value;
            case "useCorporateDesign" -> useCorporateDesign = (boolean) value;
            case "useChat" -> useChat = (boolean) value;
            case "allowVoiceChat" -> allowVoiceChat = (boolean) value;
            case "allowPublicChats" -> allowPublicChats = (boolean) value;
        }
    }

    public enum ConfigValueType {
        STRING, INT, BOOLEAN, PASSWORD_CONDITIONS, CORPORATE_DESIGN, ONLINE_STATUS, UNKNOWN
    }
}