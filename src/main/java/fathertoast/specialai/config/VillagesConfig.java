package fathertoast.specialai.config;

import fathertoast.crust.api.config.common.AbstractConfigCategory;
import fathertoast.crust.api.config.common.AbstractConfigFile;
import fathertoast.crust.api.config.common.ConfigManager;

public class VillagesConfig extends AbstractConfigFile {
    
    public final VillagesGeneral GENERAL;
    
    /** Builds the config spec that should be used for this config. */
    VillagesConfig( ConfigManager cfgManager, String cfgName ) {
        super( cfgManager, cfgName,
                "This config contains various options to control village fixes, tweaks, aggression, and reputation." +
                        "For reference, starting player reputation is 0, minimum is -30 and maximum is 10." +
                        "Players are considered an enemy to a village if their reputation is -15 or lower."
        );
        
        GENERAL = new VillagesGeneral( this );
    }
    
    public static class VillagesGeneral extends AbstractConfigCategory<VillagesConfig> {
        
        VillagesGeneral( VillagesConfig parent ) {
            super( parent, "general",
                    "Options for customizing villages in general." );
            
            SPEC.comment( "UNDER CONSTRUCTION..." ); //TODO
        }
    }
}