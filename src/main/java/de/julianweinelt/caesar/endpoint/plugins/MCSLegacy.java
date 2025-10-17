package de.julianweinelt.caesar.endpoint.plugins;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is indented for use in the enum {@link MinecraftServerSoftware} to mark server software types
 * as legacy. That means, the support for these server types won't be actively maintained as the server softwares
 * are EOL (end-of-life) or deprecated by their developers.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface MCSLegacy {
}
