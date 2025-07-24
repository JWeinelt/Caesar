package de.julianweinelt.caesar.plugin.event;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

public class Event {
    @Getter
    private final String name;
    private final Map<String, EventProperty> properties = new HashMap<>();
    @Getter @Setter
    private boolean cancelled = false;
    @Getter
    private boolean cancellable = true;

    public Event(String name) {
        this.name = name;
    }

    public Event nonCancellable() {
        cancellable = false;
        return this;
    }

    public Event set(String key, Object value) {
        properties.put(key, new EventProperty(value));
        return this;
    }

    public EventProperty get(String key) {
        if (!properties.containsKey(key))
            throw new IllegalArgumentException("The key " + key + " does not exist in the event " + name);
        return properties.get(key);
    }

    public void cancel() {
        if (!cancellable) throw new IllegalStateException("The event " + name + " cannot be cancelled");
        this.cancelled = true;
    }
}