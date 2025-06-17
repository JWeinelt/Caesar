package de.julianweinelt.caesar.exceptions.logging;

import de.julianweinelt.caesar.Caesar;

import java.util.ArrayList;
import java.util.List;

public class ProblemLogger {

    public static ProblemLogger getInstance() {
        return Caesar.getInstance().getProblemLogger();
    }

    public void log(ProblemType type, String message) {
        logs.add(new LogEntry(type, message));
    }

    private final List<LogEntry> logs = new ArrayList<>();

    public record LogEntry(ProblemType type, String message) {}

    public enum ProblemType {
        WARN,
        ERROR,
        FATAL,
        DEBUG;
    }
}