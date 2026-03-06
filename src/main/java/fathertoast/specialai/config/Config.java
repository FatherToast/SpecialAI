package fathertoast.specialai.config;

import fathertoast.crust.api.config.common.ConfigManager;
import fathertoast.specialai.SpecialAI;
import net.minecraftforge.fml.DeferredWorkQueue;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingStage;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.Optional;

/**
 * The initial loading for this is done during the common setup event.
 */
public class Config {
    
    private static final ConfigManager MANAGER = ConfigManager.create( "SpecialAI", SpecialAI.MOD_ID );
    
    public static final GeneralConfig GENERAL = new GeneralConfig( MANAGER, "general" );
    public static final IdleConfig IDLE = new IdleConfig( MANAGER, "idle_ai" );
    public static final EliteAIConfig ELITE_AI = new EliteAIConfig( MANAGER, "elite_ai" );
    public static final VillagesConfig VILLAGES = new VillagesConfig( MANAGER, "villages" );
    
    /**
     *  Called from {@link SpecialAI#SpecialAI(FMLJavaModLoadingContext)} to load this class
     *  and create this mod's config manager early.<br>
     *  The actual config loading will be run later on the main thread
     *  during common setup.
     */
    public static void init() {
        DeferredWorkQueue.lookup( Optional.of( ModLoadingStage.COMMON_SETUP ) ).ifPresent(
                ( workQueue ) -> workQueue.enqueueWork( ModList.get().getModContainerById( SpecialAI.MOD_ID ).orElseThrow(),
                        Config::initialize )
        );
    }
    
    /** Performs initial loading of all configs in this mod. */
    private static void initialize() {
        GENERAL.SPEC.initialize();
        IDLE.SPEC.initialize();
        ELITE_AI.SPEC.initialize();
        VILLAGES.SPEC.initialize();
    }
}