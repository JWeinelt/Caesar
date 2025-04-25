package de.julianweinelt.caesar.plugin.event;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

public class Event {
    @Getter
    private final String name;
    private final Map<String, EventProperty> properties = new HashMap<>();
    @Getter
    private boolean cancelled = false;

    public Event(String name) {
        this.name = name;
    }

    public Event set(String key, Object value) {
        properties.put(key, new EventProperty(value));
        return this;
    }

    public EventProperty get(String key) {
        return properties.get(key);
    }

    public void cancel() {
        this.cancelled = true;
    }

}
