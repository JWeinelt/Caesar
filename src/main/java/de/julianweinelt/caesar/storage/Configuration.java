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

    private String discordBotToken;
    private OnlineStatus defaultOnlineStatus;

    private String cloudnetHost;
    private String cloudnetUser;
    private String cloudnetPassword;

    private String languageVersion;
    private String clientVersion;

    private int tokenExpirationTime = 360*4; // Default: 24 hours

    private String connectionAPISecret;

    private PasswordConditions passwordConditions;
    private boolean useDiscord;
    private CorporateDesign corporateDesign;
    private boolean useCorporateDesign;
}