package xyz.overdyn.dynconfig.annotation;

import xyz.overdyn.dynconfig.format.ConfigFormat;
import xyz.overdyn.dynconfig.policy.MissingKeyPolicy;

import java.lang.annotation.*;

/**
 * Marks a class as a configuration resource.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ConfigResource {

    /**
     * Relative path to the config file, e.g. "config.yml".
     */
    String path();

    /**
     * Underlying storage format.
     */
    ConfigFormat format() default ConfigFormat.YAML;

    /**
     * What to do when a key is missing in file.
     */
    MissingKeyPolicy missingKeyPolicy() default MissingKeyPolicy.WRITE_DEFAULT;
}
