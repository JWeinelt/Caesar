package de.julianweinelt.caesar.plugin;

import de.julianweinelt.caesar.exceptions.PluginInvalidException;

public class PluginInstantiator {

    public CPlugin instantiate(PluginDescriptor descriptor, ClassLoader loader) throws Exception {
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
