package de.julianweinelt.caesar.util;

import com.google.gson.JsonObject;

import java.awt.*;

public class DatabaseColorParser {

    public static Color parseColor(String color) {
        int red = Integer.parseInt(color.split(";")[0]);
        int green = Integer.parseInt(color.split(";")[1]);
        int blue = Integer.parseInt(color.split(";")[2]);
        int alpha = Integer.parseInt(color.split(";")[3]);

        return new Color(red, green, blue, alpha);
    }

    public static String parseColor(Color color) {
        return color.getRed() + ";" + color.getGreen() + ";" + color.getBlue() + ";" + color.getAlpha();
    }

    public static JsonObject getColor(String dbColor) {
        JsonObject o = new JsonObject();
        Color c = parseColor(dbColor);
        o.addProperty("red", c.getRed());
        o.addProperty("green", c.getGreen());
        o.addProperty("blue", c.getBlue());
        o.addProperty("alpha", c.getAlpha());
        return o;
    }
}