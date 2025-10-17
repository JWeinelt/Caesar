package de.julianweinelt.caesar.plugin.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as an event subscriber for a specific event.
 * The annotation takes a {@link String} as a parameter to specify for which event the method is subscribing.
 * Additionally, it allows setting a priority for the event listener using the {@link Priority} enum
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Subscribe {
    String value();
    Priority priority() default Priority.NORMAL;
}
