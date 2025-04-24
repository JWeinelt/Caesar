package de.julianweinelt.caesar.plugin.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME) // Annotation bleibt zur Laufzeit erhalten
@Target(ElementType.METHOD) // Kann nur auf Methoden angewendet werden
public @interface Subscribe {
    String value(); // Event-Name
    Priority priority() default Priority.NORMAL; // Optional: Priorität
}
