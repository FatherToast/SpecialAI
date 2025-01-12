package fathertoast.specialai;


import fathertoast.specialai.ai.AIManager;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Contains and automatically registers all needed forge events.
 * Each event passes itself off to interested sub-mods.
 */
@SuppressWarnings( "unused" )
@Mod.EventBusSubscriber( modid = SpecialAI.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE )
public final class GameEventHandler {
    /**
     * Called for the server at the start and end of each tick.
     * <p>
     * It is usually wise to check the phase (start/end) before doing anything.
     *
     * @param event The event data.
     */
    @SubscribeEvent( priority = EventPriority.NORMAL )
    public static void onServerTick( TickEvent.ServerTickEvent event ) { AIManager.onServerTick( event ); }
    
    /**
     * Called when any entity is spawned in the world, including by chunk loading and dimension transition.
     *
     * @param event The event data.
     */
    @SubscribeEvent( priority = EventPriority.LOW )
    public static void onEntityJoinLevel( EntityJoinLevelEvent event ) { AIManager.onJoinWorld( event ); }
    
    /**
     * Called when a living entity dies for any reason.
     *
     * @param event The event data.
     */
    @SubscribeEvent( priority = EventPriority.NORMAL )
    public static void onLivingDeath( LivingDeathEvent event ) { AIManager.onLivingDeath( event ); }
    
    /**
     * Called when a player right-clicks while targeting a block.
     *
     * @param event The event data.
     */
    @SubscribeEvent( priority = EventPriority.LOWEST )
    public static void onRightClickBlock( PlayerInteractEvent.RightClickBlock event ) {
        if( event.isCanceled() ) return;
        AIManager.onRightClickBlock( event );
    }
    
    /**
     * Called right before a block is broken by a player.
     *
     * @param event The event data.
     */
    @SubscribeEvent( priority = EventPriority.LOWEST )
    public static void onBlockBreak( BlockEvent.BreakEvent event ) {
        if( event.isCanceled() ) return;
        AIManager.onBlockBreak( event );
    }
    
    /**
     * Called after an explosion has calculated targets, but before applying effects.
     *
     * @param event The event data.
     */
    @SubscribeEvent( priority = EventPriority.LOWEST )
    public static void onExplosionDetonate( ExplosionEvent.Detonate event ) { AIManager.onExplosionDetonate( event ); }
}