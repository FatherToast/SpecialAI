package fathertoast.specialai.config.dimension;

import fathertoast.crust.api.config.common.ConfigManager;
import fathertoast.specialai.config.ConfigGroup;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

// TODO - Copy-paste from DeadlyWorld; maybe make a useful dimension group thingy in Crust?

/**
 * Groups together every config file used for a single dimension.
 */
public class DimensionConfigGroup extends ConfigGroup {
    
    /** The registry key of this config's dimension. */
    public final ResourceKey<Level> DIMENSION;
    
    public final EnvironmentConfig ENVIRONMENT;
    
    
    public DimensionConfigGroup( ConfigManager manager, ResourceKey<Level> dimension ) {
        DIMENSION = dimension;
        
        // Organized in folder: configs/SpecialAI/<modid>/<dimension>/
        final String dir = dimension.location().getNamespace() + "/" + dimension.location().getPath() + "/";
        
        ENVIRONMENT = group( new EnvironmentConfig( manager, dir, this, "environment" ) );
    }
    
    /** @return The short name for this dimension (e.g. "'the_nether' dimension"). */
    public String dimensionName() { return "'" + DIMENSION.location().getPath() + "' dimension"; }
    
    /** @return The long name for this dimension (e.g. "'the_nether' dimension from 'minecraft'"). */
    public String longDimensionName() {
        return dimensionName() + " from '" + DIMENSION.location().getNamespace() + "'";
    }
}
