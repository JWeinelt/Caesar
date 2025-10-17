package de.julianweinelt.caesar.storage;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * @author JulianWeinelt
 * @version 1.0
 *
 * Represents a type of storage used to determine which drivers should be used.
 * Developers can register new storage types by creating new instances of this class.
 * Each storage type has a name, a default port, and a factory function to create the corresponding storage provider.
 *
 * @param name The name of the storage type. Please always use uppercase letters.
 * @param defaultPort The default port for the storage type. This may be {@code 3306} for MySQL, {@code 5432} for PostgreSQL, etc.
 * @param providerFactory A factory function that takes a {@link Configuration} object and returns a corresponding {@link Storage} provider.
 */
public record StorageType(String name, int defaultPort, Function<Configuration, Storage> providerFactory) {
    private static final Map<String, StorageType> REGISTERED_TYPES = new LinkedHashMap<>();

    public StorageType(String name, int defaultPort, Function<Configuration, Storage> providerFactory) {
        this.name = name;
        this.defaultPort = defaultPort;
        this.providerFactory = providerFactory;
        REGISTERED_TYPES.put(name.toUpperCase(), this);
    }

    /**
     * Retrieves a registered StorageType by its name.
     * @param name The name of the storage type to retrieve.
     * @return The corresponding StorageType, or null if not found.
     */
    //TODO: Change to Optional<StorageType>
    @Nullable
    public static StorageType get(String name) {
        return REGISTERED_TYPES.getOrDefault(name.toUpperCase(), null);
    }

    /**
     * Retrieves all registered StorageTypes.
     * @return A collection of all registered StorageTypes.
     */
    public static Collection<StorageType> values() {
        return REGISTERED_TYPES.values();
    }

    /**
     * Creates a storage provider using the provided configuration.
     * @param config The {@link Configuration} to use for creating the storage provider.
     * @return The created Storage provider as an object extending {@link Storage}.
     */
    public Storage createProvider(Configuration config) {
        return providerFactory.apply(config);
    }
}
