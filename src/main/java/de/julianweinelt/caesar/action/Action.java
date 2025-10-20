package de.julianweinelt.caesar.action;

/**
 * @deprecated in favor of WorkFlowAPI
 */
@Deprecated(since = "0.2.1")
public interface Action {
    void run(Object... params);
}