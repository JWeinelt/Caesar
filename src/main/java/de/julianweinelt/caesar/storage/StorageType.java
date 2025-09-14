package de.julianweinelt.caesar.storage;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

public record StorageType(String name, int defaultPort, Function<Configuration, Storage> providerFactory) {
    private static final Map<String, StorageType> REGISTERED_TYPES = new LinkedHashMap<>();

    public StorageType(String name, int defaultPort, Function<Configuration, Storage> providerFactory) {
        this.name = name;
        this.defaultPort = defaultPort;
        this.providerFactory = providerFactory;
        REGISTERED_TYPES.put(name.toUpperCase(), this);
    }

    public static StorageType get(String name) {
        return REGISTERED_TYPES.get(name.toUpperCase());
    }

    public static Collection<StorageType> values() {
        return REGISTERED_TYPES.values();
    }

    public Storage createProvider(Configuration config) {
        return providerFactory.apply(config);
    }
}
