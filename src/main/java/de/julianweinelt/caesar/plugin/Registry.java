package de.julianweinelt.caesar.plugin;

import de.julianweinelt.caesar.Caesar;
import de.julianweinelt.caesar.commands.CLICommand;
import de.julianweinelt.caesar.endpoint.minecraft.MCPluginEndpoint;
import de.julianweinelt.caesar.exceptions.MinecraftEndpointNotFoundException;
import de.julianweinelt.caesar.plugin.event.*;
import de.julianweinelt.caesar.plugin.event.EventListener;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
/**
 * The {@code Registry} class manages the core functionality of the Caesar plugin framework,
 * including:
 * <ul>
 *     <li>Plugin registration and lifecycle</li>
 *     <li>Custom event handling and listener management</li>
 *     <li>Command registration</li>
 *     <li>Minecraft plugin endpoints</li>
 * </ul>
 *
 * <p>
 * It acts as a centralized manager and dispatcher, handling all dynamic components
 * within the plugin environment.
 * </p>
 */
public class Registry {
    private static final Logger log = LoggerFactory.getLogger(Registry.class);

    @Getter
    private final SystemPlugin systemPlugin;

    @Getter
    private final ConcurrentLinkedQueue<CPlugin> plugins = new ConcurrentLinkedQueue<>();

    private final Map<String, List<EventListener>> listeners = new HashMap<>();
    private final Map<CPlugin, List<String>> eventsRegisteredByPlugin = new HashMap<>();

    private final List<MCPluginEndpoint> pluginEndpoints = new ArrayList<>();

    @Getter
    private final List<CLICommand> commands = new ArrayList<>();

    public Registry() {
        systemPlugin = new SystemPlugin();
    }
    /**
     * Returns the global singleton {@code Registry} instance.
     *
     * @return the singleton instance from Caesar core
     */
    public static Registry getInstance() {
        return Caesar.getInstance().getRegistry();
    }

    /**
     * Registers a single event name for a given plugin.
     *
     * @param plugin    the plugin registering the event
     * @param eventName the name of the event
     */
    public void registerEvent(CPlugin plugin, String eventName) {
        listeners.putIfAbsent(eventName, new ArrayList<>());

        if (eventsRegisteredByPlugin.containsKey(plugin)) eventsRegisteredByPlugin.get(plugin).add(eventName);
        else eventsRegisteredByPlugin.put(plugin, new ArrayList<>(Collections.singletonList(eventName)));
    }

    /**
     * Registers multiple event names for a given plugin.
     *
     * @param plugin     the plugin registering the events
     * @param eventNames the names of the events
     */
    public void registerEvents(CPlugin plugin, String... eventNames) {
        for (String eventName : eventNames) {
            listeners.putIfAbsent(eventName, new ArrayList<>());
        }

        if (eventsRegisteredByPlugin.containsKey(plugin)) eventsRegisteredByPlugin.get(plugin).addAll(Arrays.asList(eventNames));
        else eventsRegisteredByPlugin.put(plugin, new ArrayList<>(Arrays.asList(eventNames)));
    }

    /**
     * Registers multiple system event names (not bound to any plugin).
     * Should not be used to register events
     *
     * @param eventNames the event names to register
     */
    public void registerEvents(String... eventNames) {
        for (String eventName : eventNames) {
            listeners.putIfAbsent(eventName, new ArrayList<>());
        }
    }

    /**
     * Registers all listener methods annotated with {@link Subscribe} in the given listener object.
     *
     * @param listener the listener instance containing subscribed methods
     * @param plugin   the plugin registering the listener
     */
    public void registerListener(Object listener, CPlugin plugin) {
        for (Method method : listener.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(Subscribe.class)) {
                Subscribe annotation = method.getAnnotation(Subscribe.class);
                String eventName = annotation.value();
                Priority priority = annotation.priority();

                listeners.putIfAbsent(eventName, new ArrayList<>());
                listeners.get(eventName).add(new EventListener(listener, method, priority));
                listeners.get(eventName).sort(Comparator.comparing(EventListener::getPriority));
            }
        }
    }

    /**
     * Calls a specific event and dispatches it to all registered listeners
     * for the event's name, in priority order.
     *
     * @param event the event to dispatch
     */
    public void callEvent(Event event) {
        log.debug("Calling event {}", event.getName());
        List<EventListener> eventListeners = listeners.get(event.getName());
        if (eventListeners != null) {
            for (EventListener listener : eventListeners) {
                try {
                    if (listener.getMethod().isAnnotationPresent(IgnoreCancelled.class) || !event.isCancelled()) {
                        listener.invoke(event);
                    }
                } catch (Exception e) {
                    log.error(e.getMessage());
                }
            }
        }
    }

    /**
     * Adds a plugin to the internal registry.
     *
     * @param name the plugin to register
     */
    public void addPlugin(CPlugin name) {
        plugins.add(name);
    }

    /**
     * Retrieves a plugin by its name.
     *
     * @param name the name of the plugin
     * @return the {@link CPlugin} instance, or {@code null} if not found
     */
    public CPlugin getPlugin(String name) {
        for (CPlugin m : plugins) if (m.getName().equals(name)) return m;
        return null;
    }

    /**
     * Removes a plugin and all its associated event registrations.
     *
     * @param name the name of the plugin to remove
     */
    public void removePlugin(String name) {
        CPlugin plugin = getPlugin(name);
        List<String> events = eventsRegisteredByPlugin.getOrDefault(plugin, new ArrayList<>());
        events.forEach(event -> listeners.getOrDefault(event, new ArrayList<>()).clear());
        events.forEach(listeners::remove);
        plugins.removeIf(m -> m.getName().equals(name));
    }

    /**
     * Registers a new CLI command.
     *
     * @param command the command to register
     */
    public void registerCommand(CLICommand command) {
        commands.add(command);
    }

    /**
     * Adds a new Minecraft plugin endpoint.
     *
     * @param endpoint the endpoint to register
     */
    public void addEndpoint(MCPluginEndpoint endpoint) {
        pluginEndpoints.add(endpoint);
    }

    /**
     * Retrieves a Minecraft plugin endpoint by its name.
     *
     * @param name the name of the endpoint
     * @return the {@link MCPluginEndpoint} instance
     * @throws MinecraftEndpointNotFoundException if no endpoint with the given name exists
     */
    public MCPluginEndpoint getEndpoint(String name) {
        for (MCPluginEndpoint endpoint : pluginEndpoints) {
            if (endpoint.getName().equals(name)) return endpoint;
        }
        throw new MinecraftEndpointNotFoundException(name);
    }
}
