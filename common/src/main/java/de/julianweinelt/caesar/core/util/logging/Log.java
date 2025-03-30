package de.julianweinelt.caesar.core.util.logging;


public class Log {
    private static final LoggerAdapter LOGGER;

    static {
        if (isMinecraftEnvironment()) {
            LOGGER = new JavaUtilLoggerAdapter();
        } else {
            LOGGER = new Slf4jLoggerAdapter();
        }
    }

    public static void info(String msg) {
        LOGGER.info(msg);
    }

    public static void warn(String msg) {
        LOGGER.warn(msg);
    }

    public static void warning(String msg) {
        LOGGER.warn(msg);
    }

    public static void error(String msg) {
        LOGGER.error(msg);
    }

    public static void info(String msg, String... properties) {
        String formattedMessage = formatMessage(msg, properties);
        LOGGER.error(formattedMessage);
    }

    public static void warn(String msg, String... properties) {
        String formattedMessage = formatMessage(msg, properties);
        LOGGER.error(formattedMessage);
    }

    public static void warning(String msg, String... properties) {
        String formattedMessage = formatMessage(msg, properties);
        LOGGER.error(formattedMessage);
    }

    public static void error(String msg, String... properties) {
        String formattedMessage = formatMessage(msg, properties);
        LOGGER.error(formattedMessage);
    }

    private static boolean isMinecraftEnvironment() {
        try {
            Class.forName("org.bukkit.Bukkit");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static String formatMessage(String msg, String... properties) {
        for (String property : properties) {
            msg = msg.replaceFirst("\\{}", property);
        }
        return msg;
    }
}
