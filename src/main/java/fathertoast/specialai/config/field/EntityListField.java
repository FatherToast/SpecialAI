package fathertoast.specialai.config.field;

import fathertoast.specialai.config.util.EntityList;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

/**
 * Represents an entity list config option.
 */
public class EntityListField extends GenericField<EntityList> {
    /** The raw config field. The value of this may change as configs are loaded and reloaded. */
    private final ForgeConfigSpec.ConfigValue<List<? extends String>> specValue;
    
    public EntityListField( ForgeConfigSpec.ConfigValue<List<? extends String>> val ) { specValue = val; }
    
    @Override
    public void resolve() {
        value = new EntityList( specValue.get() );
    }
}