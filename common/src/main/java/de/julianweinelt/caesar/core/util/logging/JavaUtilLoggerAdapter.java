package de.julianweinelt.caesar.core.util.logging;

import java.util.logging.Logger;

public class JavaUtilLoggerAdapter implements LoggerAdapter {
    private final Logger logger = Logger.getLogger("CaesarPl");

    @Override
    public void info(String msg) {
        logger.info(msg);
    }

    @Override
    public void warn(String msg) {
        logger.warning(msg);
    }

    @Override
    public void error(String msg) {
        logger.severe(msg);
    }
}
