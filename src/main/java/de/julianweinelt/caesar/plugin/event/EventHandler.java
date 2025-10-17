package de.julianweinelt.caesar.plugin.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation was intended to mark methods as event handlers.
 * @deprecated This annotation is deprecated and will be removed in future versions.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Deprecated(forRemoval = true)
@interface EventHandler {}