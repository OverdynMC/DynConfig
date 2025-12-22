package xyz.overdyn.dynconfig.annotation;

import java.lang.annotation.*;

/**
 * Overrides the default field name as config key.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ConfigKey {
    String value();
}
