package fathertoast.specialai;


import fathertoast.specialai.config.Config;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
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
public class GameEventHandler {
    /**
     * Called for each world at the start and end of each tick.
     * <p>
     * It is usually wise to check the phase (start/end) and world side (server/client) before doing anything.
     *
     * @param event The event data.
     */
    @SubscribeEvent( priority = EventPriority.NORMAL )
    public static void onWorldTick( TickEvent.WorldTickEvent event ) {
    }
    
    /**
     * Called when any entity is spawned in the world, including by chunk loading and dimension transition.
     *
     * @param event The event data.
     */
    @SubscribeEvent( priority = EventPriority.NORMAL )
    public static void onJoinWorld( EntityJoinWorldEvent event ) {
    }
    
    /**
     * Called when a living entity is damaged - before armor, potions, and enchantments reduce damage.
     *
     * @param event The event data.
     */
    @SubscribeEvent( priority = EventPriority.LOWEST )
    public void onLivingHurt( LivingHurtEvent event ) {
    }
}