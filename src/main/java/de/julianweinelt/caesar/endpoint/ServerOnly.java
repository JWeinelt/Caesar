package de.julianweinelt.caesar.endpoint;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an action sent by or to a websocket endpoint as server-only.
 * Such actions will only be processed on the server side and ignored on the client side.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ServerOnly {
}