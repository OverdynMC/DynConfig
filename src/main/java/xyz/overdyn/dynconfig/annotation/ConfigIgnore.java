package xyz.overdyn.dynconfig.annotation;

import java.lang.annotation.*;

/**
 * Marks field to be ignored by configuration system.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ConfigIgnore {
}
