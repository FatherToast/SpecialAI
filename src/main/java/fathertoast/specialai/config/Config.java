package fathertoast.specialai.config;

import fathertoast.crust.api.config.common.ConfigManager;

/**
 * The initial loading for this is done during the common setup event.
 */
public class Config {
    
    private static final ConfigManager MANAGER = ConfigManager.create( "SpecialAI" );
    
    public static final GeneralConfig GENERAL = new GeneralConfig( MANAGER, "general" );
    public static final IdleConfig IDLE = new IdleConfig( MANAGER, "idle_ai" );
    public static final EliteAIConfig ELITE_AI = new EliteAIConfig( MANAGER, "elite_ai" );
    //public static final VillagesConfig VILLAGES = new VillagesConfig( MANAGER, "villages" );
    
    /** Performs initial loading of all configs in this mod. */
    public static void initialize() {
        GENERAL.SPEC.initialize();
        IDLE.SPEC.initialize();
        ELITE_AI.SPEC.initialize();
        //VILLAGES.SPEC.initialize();
    }
}