package de.julianweinelt.caesar.action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class ActionManager {
    private static final Logger log = LoggerFactory.getLogger(ActionManager.class);

    private final Map<String, Action> actionMap = new HashMap<>();

    public void registerAction(String actionName, Action action) {
        actionMap.put(actionName, action);
    }

    public void executeAction(String actionName, Object... params) {
        Action action = actionMap.get(actionName);
        if (action != null) {
            action.run(params);
        } else {
            log.error("Action {} not found.", actionName);
        }
    }

    public Object getActionSavedParam(String action, String param) {
        return null; //TODO: Get saved parameters from file
    }
}