package fathertoast.specialai.config.field;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Represents a single enum config option.
 */
public class EnumField<T extends Enum<T>> implements IConfigField {
    /** The underlying field value. */
    private T value;
    /** The raw config field. The value of this may change as configs are loaded and reloaded. */
    private final ForgeConfigSpec.EnumValue<T> specValue;
    
    public EnumField( ForgeConfigSpec.EnumValue<T> val ) { specValue = val; }
    
    /** Returns this config field's value. */
    public T get() { return value; }
    
    /** Called when the config is loaded or reloaded to update the underlying return value. */
    @Override
    public void resolve() { value = specValue.get(); }
}