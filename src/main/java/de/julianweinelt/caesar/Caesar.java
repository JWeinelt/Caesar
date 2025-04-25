package de.julianweinelt.caesar;

import de.julianweinelt.caesar.plugin.Registry;
import lombok.Getter;

public class Caesar {
    @Getter
    private static Caesar instance;
    public static String systemVersion = "1.0";

    @Getter
    private Registry registry;

    public static void main(String[] args) {
        instance = new Caesar();
        instance.start();
    }

    public void start() {
        registry = new Registry();
    }
}