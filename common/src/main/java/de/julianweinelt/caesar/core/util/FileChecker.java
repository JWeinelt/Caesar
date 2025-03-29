package de.julianweinelt.caesar.core.util;

import java.io.File;

public class FileChecker {
    public static boolean isFirstStart() {
        return new File(".", "config.json").exists();
    }
}
