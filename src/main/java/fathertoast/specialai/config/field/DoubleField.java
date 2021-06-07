package fathertoast.specialai.config.field;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Represents a single double config option.
 */
public class DoubleField implements IConfigField {
    /** The underlying field value. */
    private double value;
    /** The raw config field. The value of this may change as configs are loaded and reloaded. */
    private final ForgeConfigSpec.DoubleValue specValue;
    
    public DoubleField( ForgeConfigSpec.DoubleValue val ) { specValue = val; }
    
    /** Returns this config field's value. */
    public double get() { return value; }
    
    /** Called when the config is loaded or reloaded to update the underlying return value. */
    @Override
    public void resolve() { value = specValue.get(); }
}