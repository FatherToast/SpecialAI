package fathertoast.specialai.config;

import fathertoast.specialai.config.file.ToastConfigSpec;

import java.io.File;

public class VillagesConfig extends Config.AbstractConfig {
    
    public final VillagesGeneral GENERAL;
    
    /** Builds the config spec that should be used for this config. */
    VillagesConfig( File dir, String fileName ) {
        super( dir, fileName,
                "This config contains various options to control village fixes, tweaks, aggression, and reputation." +
                        "For reference, starting player reputation is 0, minimum is -30 and maximum is 10." +
                        "Players are considered an enemy to a village if their reputation is -15 or lower."
        );
        
        GENERAL = new VillagesGeneral( SPEC );
    }
    
    public static class VillagesGeneral extends Config.AbstractCategory {
        
        VillagesGeneral( ToastConfigSpec parent ) {
            super( parent, "general",
                    "Options for customizing villages in general." );
            
            SPEC.comment( "UNDER CONSTRUCTION..." ); //TODO
        }
    }
}