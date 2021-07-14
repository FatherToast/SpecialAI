package fathertoast.specialai.config;

import fathertoast.specialai.ModCore;
import fathertoast.specialai.config.field.AbstractConfigField;
import fathertoast.specialai.config.file.ToastConfigSpec;
import fathertoast.specialai.config.file.TomlHelper;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;

/**
 * Used as the sole hub for all config access from outside the config package.
 * <p>
 * Contains references to all config files used in this mod, which in turn provide direct 'getter' access to each
 * configurable value.
 */
public class Config {
    /** The root folder for config files in this mod. */
    public static final File CONFIG_DIR = new File( FMLPaths.CONFIGDIR.get().toFile(), "FatherToast/" + ModCore.MOD_ID + "/" );
    
    public static final GeneralConfig GENERAL = new GeneralConfig( CONFIG_DIR, "general" );
    public static final IdleConfig IDLE = new IdleConfig( CONFIG_DIR, "idle_ai" );
    public static final EliteAIConfig ELITE_AI = new EliteAIConfig( CONFIG_DIR, "elite_ai" );
    //public static final VillagesConfig VILLAGES = new VillagesConfig( CONFIG_DIR, "villages" );
    
    /** Performs initial loading of all configs in this mod. */
    public static void initialize() {
        AbstractConfigField.loadingCategory = null;
        
        GENERAL.SPEC.initialize();
        IDLE.SPEC.initialize();
        ELITE_AI.SPEC.initialize();
        //VILLAGES.SPEC.initialize();
    }
    
    /**
     * Represents one config file that contains a reference for each configurable value within and a specification
     * that defines the file's format.
     */
    public static abstract class AbstractConfig {
        /** The spec used by this config that defines the file's format. */
        public final ToastConfigSpec SPEC;
        
        AbstractConfig( File dir, String fileName, String... fileDescription ) {
            AbstractConfigField.loadingCategory = "";
            SPEC = new ToastConfigSpec( dir, fileName );
            SPEC.header( TomlHelper.newComment( fileDescription ) );
        }
    }
    
    /**
     * Represents one config file that contains a reference for each configurable value within and a specification
     * that defines the file's format.
     */
    public static abstract class AbstractCategory {
        /** The spec used by this config that defines the file's format. */
        protected final ToastConfigSpec SPEC;
        
        AbstractCategory( ToastConfigSpec parent, String name, String... categoryDescription ) {
            AbstractConfigField.loadingCategory = name + ".";
            SPEC = parent;
            SPEC.category( name, TomlHelper.newComment( categoryDescription ) );
        }
    }
}