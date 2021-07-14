package fathertoast.specialai;


import fathertoast.specialai.ai.AIManager;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Contains and automatically registers all needed forge events.
 * Each event passes itself off to interested sub-mods.
 */
@SuppressWarnings( "unused" )
@Mod.EventBusSubscriber( modid = ModCore.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE )
public final class GameEventHandler {
    /**
     * Called for the server at the start and end of each tick.
     * <p>
     * It is usually wise to check the phase (start/end) before doing anything.
     *
     * @param event The event data.
     */
    @SubscribeEvent( priority = EventPriority.NORMAL )
    public static void onServerTick( TickEvent.ServerTickEvent event ) {
        AIManager.onServerTick( event );
    }
    
    /**
     * Called when any entity is spawned in the world, including by chunk loading and dimension transition.
     *
     * @param event The event data.
     */
    @SubscribeEvent( priority = EventPriority.LOW )
    public static void onJoinWorld( EntityJoinWorldEvent event ) {
        AIManager.onJoinWorld( event );
    }
    
    /**
     * Called when a living entity dies for any reason.
     *
     * @param event The event data.
     */
    @SubscribeEvent( priority = EventPriority.NORMAL )
    public void onLivingDeath( LivingDeathEvent event ) {
        AIManager.onLivingDeath( event );
    }
}