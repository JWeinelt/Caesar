package de.julianweinelt.caesar.endpoint.plugins;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is indented for use in the enum {@link MinecraftServerSoftware} to mark server software types
 * as experimental. That means, the support for these server types is not fully tested and may be unstable.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface MCSExperimental {
}
