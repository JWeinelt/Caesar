package de.julianweinelt.caesar.exceptions.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

import java.util.ArrayList;
import java.util.List;

public class NotificationAppender extends AppenderBase<ILoggingEvent> {
    @Override
    protected void append(ILoggingEvent e) {
        if (e.getLevel() == Level.ERROR) {
            List<String> stackTrace = new ArrayList<>();
            for (StackTraceElement s : e.getCallerData()) stackTrace.add(s.toString());
            ExceptionalManager.logError(e.getMessage(), stackTrace, e.getThreadName(), e.getInstant(), e.getLoggerName());
        }
    }
}
