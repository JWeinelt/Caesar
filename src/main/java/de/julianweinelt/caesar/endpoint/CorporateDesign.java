package de.julianweinelt.caesar.endpoint;

import de.julianweinelt.caesar.util.DatabaseColorParser;

import java.awt.*;

public record CorporateDesign(String background, String front, String buttons, boolean allowBackgrounds, String logoURL) {
    public static final CorporateDesign DEFAULT = new CorporateDesign(
            DatabaseColorParser.parseColor(Color.BLUE),
            DatabaseColorParser.parseColor(Color.BLUE),
            DatabaseColorParser.parseColor(Color.BLUE),
            true,
            "http://caesarnet.cloud/logo.png");

}