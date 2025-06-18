package de.julianweinelt.caesar.backup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class MySQLDumper {
    private static final Logger log = LoggerFactory.getLogger(MySQLDumper.class);

    public static boolean dumpDatabase(String user, String password, String dbName, File output) {
        if (output.mkdir()) log.debug("Output folder created");
        try {
            String command = String.format(
                    "mysqldump -u%s -password=%s --add-drop-table --routines --events --triggers %s",
                    user, password, dbName
            );

            ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
            pb.redirectOutput(output);
            pb.redirectErrorStream(true);

            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                log.debug("Created MySQL dump.");
                return true;
            } else {
                log.debug("Failed to create MySQL dump.");
                return false;
            }
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }
}