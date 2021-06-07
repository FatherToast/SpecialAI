package fathertoast.specialai.config.field;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Represents a single integer config option.
 */
public class IntField implements IConfigField {
    /** The underlying field value. */
    private int value;
    /** The raw config field. The value of this may change as configs are loaded and reloaded. */
    private final ForgeConfigSpec.IntValue specValue;
    
    public IntField( ForgeConfigSpec.IntValue val ) { specValue = val; }
    
    /** Returns this config field's value. */
    public int get() { return value; }
    
    /** Called when the config is loaded or reloaded to update the underlying return value. */
    @Override
    public void resolve() { value = specValue.get(); }
}