package de.julianweinelt.caesar.endpoint;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an action sent by or to a websocket endpoint as client-only.
 * Such actions will only be processed on the client side and ignored on the server side.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ClientOnly {
}