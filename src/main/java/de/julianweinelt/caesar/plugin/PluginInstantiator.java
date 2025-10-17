package de.julianweinelt.caesar.plugin;

import de.julianweinelt.caesar.exceptions.PluginInvalidException;

import java.lang.reflect.InvocationTargetException;

public class PluginInstantiator {

    /**
     * Instantiates a plugin based on its descriptor and class loader.
     * @param descriptor A {@link PluginDescriptor} object containing technical plugin metadata.
     * @param loader The class loader to use for loading the plugin's main class.
     * @return An instance of the plugin as a {@link CPlugin}.
     * @throws PluginInvalidException If the main class is not a child class of {@link CPlugin}
     * @throws ClassNotFoundException If the main class specified in the descriptor cannot be found.
     * @throws NoSuchMethodException If the main class does not have a default constructor.
     * @throws InvocationTargetException If the constructor of the main class throws an exception.
     * @throws InstantiationException If the main class cannot be instantiated.
     * @throws IllegalAccessException If the constructor of the main class is not accessible.
     */
    public CPlugin instantiate(PluginDescriptor descriptor, ClassLoader loader) throws
            PluginInvalidException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException,
            InstantiationException, IllegalAccessException {
        String className = descriptor.config().mainClass();
        Class<?> clazz = Class.forName(className, true, loader);
        if (!CPlugin.class.isAssignableFrom(clazz)) {
            throw new PluginInvalidException("Main class must extend CPlugin or implement the CPlugin interface.");
        }

        CPlugin plugin = (CPlugin) clazz.getDeclaredConstructor().newInstance();

        plugin.setName(descriptor.config().pluginName());
        plugin.setDescription(descriptor.config().description());
        plugin.setVersion(descriptor.config().version());
        plugin.setMinAPIVersion(descriptor.config().minAPIVersion());
        plugin.setUsesEncryption(descriptor.config().usesEncryption());
        plugin.setStoresSensitiveData(descriptor.config().storesSensitiveData());

        return plugin;
    }
}
