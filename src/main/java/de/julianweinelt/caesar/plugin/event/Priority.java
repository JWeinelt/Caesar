package de.julianweinelt.caesar.plugin.event;

/**
 * Defines the priority levels for event listeners.
 * Listeners with higher priority are executed before those with lower priority.
 */
public enum Priority {
    HIGH, 
    NORMAL, 
    LOW;
}
