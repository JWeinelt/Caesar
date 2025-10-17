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

    /**
     * Marks the event as non-cancellable.
     * @return The current Event instance for method chaining.
     */
    public Event nonCancellable() {
        cancellable = false;
        return this;
    }

    /**
     * Sets a property for the event.
     * @param key The property key.
     * @param value The property value.
     * @return The current Event instance for method chaining.
     */
    public Event set(String key, Object value) {
        properties.put(key, new EventProperty(value));
        return this;
    }

    /**
     * Gets a property of the event.
     * @param key The property key.
     * @return The EventProperty associated with the key.
     * @throws IllegalArgumentException if the key does not exist.
     */
    public EventProperty get(String key) throws IllegalArgumentException{
        if (!properties.containsKey(key))
            throw new IllegalArgumentException("The key " + key + " does not exist in the event " + name);
        return properties.get(key);
    }

    /**
     * Cancels the event if it is cancellable.
     * @throws IllegalStateException if the event is not cancellable.
     */
    public void cancel() throws IllegalStateException{
        if (!cancellable) throw new IllegalStateException("The event " + name + " cannot be cancelled");
        this.cancelled = true;
    }
}