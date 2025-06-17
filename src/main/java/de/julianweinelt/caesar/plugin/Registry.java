package de.julianweinelt.caesar.plugin;

import de.julianweinelt.caesar.commands.CLICommand;
import de.julianweinelt.caesar.endpoint.minecraft.MCPluginEndpoint;
import de.julianweinelt.caesar.exceptions.MinecraftEndpointNotFoundException;
import de.julianweinelt.caesar.plugin.event.Event;
import de.julianweinelt.caesar.plugin.event.EventListener;
import de.julianweinelt.caesar.plugin.event.Priority;
import de.julianweinelt.caesar.plugin.event.Subscribe;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Registry {
    private static final Logger log = LoggerFactory.getLogger(Registry.class);

    @Getter
    private final ConcurrentLinkedQueue<CPlugin> plugins = new ConcurrentLinkedQueue<>();

    private final Map<String, List<EventListener>> listeners = new HashMap<>();

    private final List<MCPluginEndpoint> pluginEndpoints = new ArrayList<>();

    @Getter
    private final List<CLICommand> commands = new ArrayList<>();

    public void registerEvent(String eventName) {
        listeners.putIfAbsent(eventName, new ArrayList<>());
    }

    public void registerListener(Object listener) {
        for (Method method : listener.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(Subscribe.class)) {
                Subscribe annotation = method.getAnnotation(Subscribe.class);
                String eventName = annotation.value();
                Priority priority = annotation.priority();

                listeners.putIfAbsent(eventName, new ArrayList<>());
                listeners.get(eventName).add(new EventListener(listener, method, priority));
                listeners.get(eventName).sort(Comparator.comparing(EventListener::getPriority)); // Sortiere nach Priorität
            }
        }
    }

    public void callEvent(Event event) {
        List<EventListener> eventListeners = listeners.get(event.getName());
        if (eventListeners != null) {
            for (EventListener listener : eventListeners) {
                try {
                    listener.invoke(event);
                    if (event.isCancelled()) break; //TODO
                } catch (Exception e) {
                    log.error(e.getMessage());
                }
            }
        }
    }

    public void addPlugin(CPlugin name) {
        plugins.add(name);
    }

    public CPlugin getPlugin(String name) {
        for (CPlugin m : plugins) if (m.getName().equals(name)) return m;
        return null;
    }
    public void removePlugin(String name) {
        plugins.removeIf(m -> m.getName().equals(name)); //TODO: Remove listeners
    }

    public void registerCommand(CLICommand command) {
        commands.add(command);
    }

    public void addEndpoint(MCPluginEndpoint endpoint) {
        pluginEndpoints.add(endpoint);
    }

    public MCPluginEndpoint getEndpoint(String name) {
        for (MCPluginEndpoint endpoint : pluginEndpoints) {
            if (endpoint.getName().equals(name)) return endpoint;
        }
        throw new MinecraftEndpointNotFoundException(name);
    }
}
