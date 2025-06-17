package de.julianweinelt.caesar.exceptions.logging;

import org.eclipse.jetty.util.log.Logger;
import org.slf4j.LoggerFactory;

public class LogbackLogger implements Logger {

    private final org.slf4j.Logger log = LoggerFactory.getLogger(getName());

    @Override
    public String getName() {
        return "JettyLogger";
    }

    @Override
    public void warn(String s, Object... objects) {
        log.warn(s, objects);
        ProblemLogger.getInstance().log(ProblemLogger.ProblemType.WARN, s);
    }

    @Override
    public void warn(Throwable throwable) {
        log.warn("Error!", throwable);
    }

    @Override
    public void warn(String s, Throwable throwable) {
        log.warn(s, throwable);
        ProblemLogger.getInstance().log(ProblemLogger.ProblemType.WARN, s);
    }

    @Override
    public void info(String s, Object... objects) {
        log.info(s, objects);
    }

    @Override
    public void info(Throwable throwable) {
        log.info("Error!", throwable);
    }

    @Override
    public void info(String s, Throwable throwable) {
        log.info(s, throwable);
    }

    @Override
    public boolean isDebugEnabled() {
        return log.isDebugEnabled();
    }

    @Override
    public void setDebugEnabled(boolean b) {
        log.warn("Tried to enable debugging!");
    }

    @Override
    public void debug(String s, Object... objects) {
        log.debug(s, objects);
        ProblemLogger.getInstance().log(ProblemLogger.ProblemType.DEBUG, s);
    }

    @Override
    public void debug(String s, long l) {
        log.debug(s);
        ProblemLogger.getInstance().log(ProblemLogger.ProblemType.DEBUG, s);

    }

    @Override
    public void debug(Throwable throwable) {
        log.debug("Error!", throwable);
    }

    @Override
    public void debug(String s, Throwable throwable) {
        log.debug(s, throwable);
        ProblemLogger.getInstance().log(ProblemLogger.ProblemType.DEBUG, s);
    }

    @Override
    public Logger getLogger(String s) {
        return this;
    }

    @Override
    public void ignore(Throwable throwable) {
        log.debug("Was told to ignore an exception (bad practice)!!");
    }

    public void error(String s) {
        log.error(s);
        ProblemLogger.getInstance().log(ProblemLogger.ProblemType.ERROR, s);
    }
}