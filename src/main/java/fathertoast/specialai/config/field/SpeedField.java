package fathertoast.specialai.config.field;

import javax.annotation.Nullable;

/**
 * Represents a config field with a double value. The entered config value is converted from m/s when loaded.
 */
@SuppressWarnings( "unused" )
public class SpeedField extends DoubleField {
    /** Conversion factor to convert from more meaningful units (blocks per second or m/s) to blocks per tick. */
    private static final double PER_SECOND_TO_PER_TICK = 0.05;
    
    /** The underlying field value, squared. */
    private double valueConverted;
    
    /** Creates a new field that accepts any value. */
    public SpeedField( String key, double defaultValue, String... description ) {
        super( key, defaultValue, description );
    }
    
    /** Creates a new field that accepts a common range of values. */
    public SpeedField( String key, double defaultValue, Range range, String... description ) {
        super( key, defaultValue, range, description );
    }
    
    /** Creates a new field that accepts a specialized range of values. */
    public SpeedField( String key, double defaultValue, double min, double max, String... description ) {
        super( key, defaultValue, min, max, description );
    }
    
    /**
     * Loads this field's value from the given raw toml value. If anything goes wrong, correct it at the lowest level possible.
     * <p>
     * For example, a missing value should be set to the default, while an out-of-range value should be adjusted to the
     * nearest in-range value
     */
    @Override
    public void load( @Nullable Object raw ) {
        super.load( raw );
        valueConverted = super.get() * PER_SECOND_TO_PER_TICK;
    }
    
    /** @return Returns the config field's value. */
    @Override
    public double get() { return valueConverted; }
    
    /** @return Returns the unconverted form (per second) of the config field's value. */
    public double getUnconverted() { return super.get(); }
}