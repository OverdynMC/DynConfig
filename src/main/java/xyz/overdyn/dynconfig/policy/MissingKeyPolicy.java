package xyz.overdyn.dynconfig.policy;

/**
 * Strategy for handling missing keys in config file.
 */
public enum MissingKeyPolicy {
    /**
     * Use field's current value, do not touch file.
     */
    USE_FIELD_DEFAULT,

    /**
     * Write field's current value into file if missing.
     */
    WRITE_DEFAULT,

    /**
     * Log error / throw exception on missing key.
     */
    ERROR
}
