package de.julianweinelt.caesar.plugin.event;

import lombok.Getter;

import java.lang.reflect.Method;

public class EventListener {
    private final Object instance;
    private final Method method;
    @Getter
    private final Priority priority;

    public EventListener(Object instance, Method method, Priority priority) {
        this.instance = instance;
        this.method = method;
        this.priority = priority;
    }

    public void invoke(Event event) throws Exception {
        method.setAccessible(true);
        method.invoke(instance, event);
    }
}