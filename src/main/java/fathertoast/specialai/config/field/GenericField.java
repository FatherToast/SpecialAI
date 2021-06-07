package fathertoast.specialai.config.field;

/**
 * Represents a single config option. Resolves its value when first accessed after creation or config reload.
 */
public abstract class GenericField<T> implements IConfigField {
    /** The underlying field value. */
    protected T value;
    
    /** Returns this config field's value. */
    public T get() { return value; }
}