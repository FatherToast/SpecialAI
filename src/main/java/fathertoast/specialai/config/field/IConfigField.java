package fathertoast.specialai.config.field;

/**
 * Represents a single field in a config file. Used to update the value whenever the config is changed.
 */
public interface IConfigField {
    /** Called when the config is loaded or reloaded to update the underlying return value. */
    void resolve();
}