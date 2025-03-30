package de.julianweinelt.caesar.core.util.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Slf4jLoggerAdapter implements LoggerAdapter {
    private final Logger logger = LoggerFactory.getLogger("Caesar");

    @Override
    public void info(String msg) {
        logger.info(msg);
    }

    @Override
    public void warn(String msg) {
        logger.warn(msg);
    }

    @Override
    public void error(String msg) {
        logger.error(msg);
    }
}
