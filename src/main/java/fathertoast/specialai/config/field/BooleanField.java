package fathertoast.specialai.config.field;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Represents a single boolean config option.
 */
public class BooleanField implements IConfigField {
    /** The underlying field value. */
    private boolean value;
    /** The raw config field. The value of this may change as configs are loaded and reloaded. */
    private final ForgeConfigSpec.BooleanValue specValue;
    
    public BooleanField( ForgeConfigSpec.BooleanValue val ) { specValue = val; }
    
    /** Returns this config field's value. */
    public boolean get() { return value; }
    
    /** Called when the config is loaded or reloaded to update the underlying return value. */
    @Override
    public void resolve() { value = specValue.get(); }
}