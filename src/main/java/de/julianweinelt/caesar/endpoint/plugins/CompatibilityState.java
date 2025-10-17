package de.julianweinelt.caesar.endpoint.plugins;

/**
 * Represents the compatibility state of a plugin with the server.<br>
 * COMPATIBLE: The plugin is fully compatible with the server.<br>
 * FEATURES_UNAVAILABLE: Some features of the plugin are unavailable due to compatibility issues.<br>
 * NOT_STARTING: The plugin cannot start due to compatibility issues.<br>
 * CRASHING_SERVER: The plugin is causing the server to crash due to severe compatibility issues
 */
public enum CompatibilityState {
    COMPATIBLE,
    FEATURES_UNAVAILABLE,
    NOT_STARTING,
    CRASHING_SERVER;
}