package de.julianweinelt.caesar.storage;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Configuration {
    private String jwtSecret;
    private String jwtIssuer;

    private String databaseHost;
    private String databaseName;
    private String databaseUser;
    private String databasePassword;
    private int databasePort;

    private String webServerHost;
    private int webServerPort;
}
