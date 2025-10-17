package de.julianweinelt.caesar.plugin.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated event listener method should ignore cancelled events.
 * When applied, the event system will not invoke the method if the event has been cancelled.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface IgnoreCancelled {}