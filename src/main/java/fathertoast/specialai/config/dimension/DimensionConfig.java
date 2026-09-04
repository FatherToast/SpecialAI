package fathertoast.specialai.config.dimension;

import fathertoast.crust.api.config.common.AbstractConfigCategory;
import fathertoast.crust.api.config.common.AbstractConfigFile;
import fathertoast.crust.api.config.common.ConfigManager;
import fathertoast.crust.api.config.common.ConfigUtil;
import fathertoast.specialai.config.Config;
import net.minecraft.world.level.Level;

/** The base of a dimension-based config. */
public abstract class DimensionConfig extends AbstractConfigFile {
    
    /** The parent group containing this feature config. */
    public final DimensionConfigGroup DIMENSION_CONFIGS;
    
    DimensionConfig( ConfigManager manager, String dir, DimensionConfigGroup dimConfigs, String name ) {
        super( manager, dir + ConfigUtil.noSpaces( name ), false,
                "This config contains various " + name + "-related options." );
        DIMENSION_CONFIGS = dimConfigs;
        
        if( Level.OVERWORLD.equals( dimConfigs.DIMENSION ) ) {
            SPEC.decreaseIndent();
            SPEC.newLine();
            SPEC.comment( "This config also functions as the default config for dimensions that doesn't have their own configs"
                    + " (all dimensions not included in the \"" + Config.GENERAL.MAIN.extraDimensions.getKey()
                    + "\" list within the mod's general config file, \"" + Config.GENERAL.SPEC.NAME + "\")." );
            SPEC.increaseIndent();
        }
    }
    
    public static class DimensionCategory extends AbstractConfigCategory<DimensionConfig> {
        
        DimensionCategory( DimensionConfig parent, String name, String... categoryDescription ) {
            super( parent, name, categoryDescription );
        }
        
        /** @return True if this config is for the overworld dimension. */
        protected boolean isOverworldDimension() { return PARENT.isOverworldDimension(); }
        
        /** @return True if this config is for the Nether dimension. */
        protected boolean isNetherDimension() { return PARENT.isNetherDimension(); }
        
        /** @return True if this config is for the End dimension. */
        protected boolean isEndDimension() { return PARENT.isEndDimension(); }
    }
    
    /** @return True if this config is for the overworld dimension. */
    protected boolean isOverworldDimension() { return Level.OVERWORLD.equals( DIMENSION_CONFIGS.DIMENSION ); }
    
    /** @return True if this config is for the Nether dimension. */
    protected boolean isNetherDimension() { return Level.NETHER.equals( DIMENSION_CONFIGS.DIMENSION ); }
    
    /** @return True if this config is for the End dimension. */
    protected boolean isEndDimension() { return Level.END.equals( DIMENSION_CONFIGS.DIMENSION ); }
}