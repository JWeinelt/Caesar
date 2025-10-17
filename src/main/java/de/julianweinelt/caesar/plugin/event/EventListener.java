package de.julianweinelt.caesar.plugin.event;

import lombok.Getter;
import org.jetbrains.annotations.ApiStatus;

import java.lang.reflect.Method;

public class EventListener {
    private final Object instance;
    @Getter
    private final Method method;
    @Getter
    private final Priority priority;

    public EventListener(Object instance, Method method, Priority priority) {
        this.instance = instance;
        this.method = method;
        this.priority = priority;
    }

    /**
     * Invokes the event listener method with the given event.
     * @param event The event to pass to the listener method.
     * @throws Exception if the method invocation fails.
     */
    @ApiStatus.Internal
    public void invoke(Event event) throws Exception {
        method.setAccessible(true);
        method.invoke(instance, event);
    }
}