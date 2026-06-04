package fathertoast.specialai.config;

import fathertoast.crust.api.config.common.ConfigManager;
import fathertoast.specialai.SpecialAI;
import fathertoast.specialai.config.dimension.DimensionConfigGroup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModLoadingStage;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import javax.annotation.Nullable;
import java.util.HashMap;

/**
 * The initial loading for this is done during the common setup event.
 */
public class Config {
    
    private static final ConfigManager MANAGER = ConfigManager.create( "SpecialAI", SpecialAI.MOD_ID );
    
    public static final GeneralConfig GENERAL = new GeneralConfig( MANAGER, "general" );
    public static final IdleConfig IDLE = new IdleConfig( MANAGER, "idle_ai" );
    public static final EliteAIConfig ELITE_AI = new EliteAIConfig( MANAGER, "elite_ai" );
    public static final VillagesConfig VILLAGES = new VillagesConfig( MANAGER, "villages" );
    
    /** Mapping of each dimension type to its config. */
    private static HashMap<ResourceKey<Level>, DimensionConfigGroup> DIMENSIONS;
    /** The default dimension config group. */
    private static DimensionConfigGroup DEFAULT_CONFIGS;
    
    /**
     * @return The group of configs associated with the given world's dimension,
     * or the default configs if the requested dimension configs do not exist or are not loaded.
     * @throws IllegalStateException if dimension configs have not yet been loaded.
     */
    public static DimensionConfigGroup getDimensionConfigs( @Nullable Level level ) {
        return getDimensionConfigs( level == null ? null : level.dimension() );
    }
    
    /**
     * @return The group of configs associated with the given dimension type key,
     * or the default configs if the requested dimension configs do not exist or are not loaded.
     * @throws IllegalStateException if dimension configs have not yet been loaded.
     */
    public static DimensionConfigGroup getDimensionConfigs( @Nullable ResourceKey<Level> dimension ) {
        assertLoaded();
        if( dimension == null ) return DEFAULT_CONFIGS;
        final DimensionConfigGroup configs = DIMENSIONS.get( dimension );
        return configs == null ? DEFAULT_CONFIGS : configs;
    }
    
    /** @throws IllegalStateException if dimension configs have not yet been loaded. */
    private static void assertLoaded() {
        if( DEFAULT_CONFIGS == null )
            throw new IllegalStateException( "Attempted to access dimension configs before any have been loaded." );
    }
    
    /**
     * Called from {@link SpecialAI#SpecialAI(FMLJavaModLoadingContext)} to load this class
     * and create this mod's config manager early.
     * <br>
     * The actual config loading will be run later on the main thread
     * during common setup.
     */
    public static void init( ModContainer modContainer ) {
        ModLoadingStage.COMMON_SETUP.getDeferredWorkQueue().enqueueWork( modContainer, () -> {
            GENERAL.SPEC.initialize();
            IDLE.SPEC.initialize();
            ELITE_AI.SPEC.initialize();
            VILLAGES.SPEC.initialize();
            
            // Dimension configs
            DEFAULT_CONFIGS = new DimensionConfigGroup( MANAGER, Level.OVERWORLD );
            DEFAULT_CONFIGS.initialize();
            DIMENSIONS = new HashMap<>();
            DIMENSIONS.put( Level.OVERWORLD, DEFAULT_CONFIGS );
            
            for( String dimension : GENERAL.MAIN.extraDimensions.get() ) {
                ResourceKey<Level> key = ResourceKey.create( Registries.DIMENSION, ResourceLocation.parse( dimension ) );
                if( DIMENSIONS.containsKey( key ) ) continue;
                DimensionConfigGroup dimConfigs = new DimensionConfigGroup( MANAGER, key );
                dimConfigs.initialize();
                DIMENSIONS.put( key, dimConfigs );
            }
        } );
    }
}