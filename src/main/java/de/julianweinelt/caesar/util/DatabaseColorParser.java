package de.julianweinelt.caesar.util;

import com.google.gson.JsonObject;

import java.awt.*;

public class DatabaseColorParser {

    /**
     * Parses a color from a database string format {@code red;green;blue;alpha}
     * @param color The color string from the database
     * @return The {@link Color} object for the given database string
     */
    public static Color parseColor(String color) {
        int red = Integer.parseInt(color.split(";")[0]);
        int green = Integer.parseInt(color.split(";")[1]);
        int blue = Integer.parseInt(color.split(";")[2]);
        int alpha = Integer.parseInt(color.split(";")[3]);

        return new Color(red, green, blue, alpha);
    }

    /**
     * Parses a color to a database string format {@code red;green;blue;alpha}
     * @param color The {@link Color} object to parse
     * @return The database string for the given color
     */
    public static String parseColor(Color color) {
        return color.getRed() + ";" + color.getGreen() + ";" + color.getBlue() + ";" + color.getAlpha();
    }

    /**
     * Converts a database color string to a {@link JsonObject} with red, green, blue and alpha properties
     * @param dbColor The database color string
     * @return The {@link JsonObject} representing the color
     */
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