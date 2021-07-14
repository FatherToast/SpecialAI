package fathertoast.specialai.config.field;

/**
 * Represents a config field with an object value.
 */
public abstract class GenericField<T> extends AbstractConfigField {
    /** The default field value. */
    protected final T valueDefault;
    
    /** The underlying field value. */
    protected T value;
    
    /** Creates a new field. */
    public GenericField( String key, T defaultValue, String... description ) {
        super( key, description );
        valueDefault = defaultValue;
    }
    
    /** @return Returns the config field's value. */
    public T get() { return value; }
    
    /** @return The raw toml value that should be assigned to this field in the config file. */
    @Override
    public Object getRaw() { return value; }
}