package fathertoast.specialai;


import fathertoast.crust.api.config.common.field.EntityListField;
import fathertoast.specialai.ai.AIManager;
import fathertoast.specialai.ai.VillagerAI;
import fathertoast.specialai.config.Config;
import fathertoast.specialai.util.VillagerNameHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.behavior.ResetProfession;
import net.minecraft.world.entity.ai.behavior.VillageBoundRandomStroll;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.hoglin.HoglinAi;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.NameTagItem;
import net.minecraft.world.level.block.BellBlock;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingMakeBrainEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
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
    public static void onLivingDeath( LivingDeathEvent event ) {
        AIManager.onLivingDeath( event );

        if ( event.getSource().getEntity() instanceof ServerPlayer player ) {
            VillagerAI.onPlayerKillLiving( event, player );
        }
    }

    /**
     * Called when a living entity is constructing its Brain AI.
     *
     * @param event The event data.
     */
    @SubscribeEvent
    public static void onLivingMakeBrain( LivingMakeBrainEvent event ) {
        // TODO - yeah
        /*
        if ( event.getEntity() instanceof Villager villager ) {
            VillagerAI.initSpecialAI( event, villager );
        }

         */
    }

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

    @SubscribeEvent
    public static void onRightClickEntity( PlayerInteractEvent.EntityInteract event ) {
        if ( event.getTarget() instanceof Villager villager
                && event.getItemStack().getItem() == Items.NAME_TAG ) {
            VillagerNameHelper.handleNameTagUse( event, villager, event.getItemStack() );
        }
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) { }

    /**
     * Called right before a block is broken by a player.
     *
     * @param event The event data.
     */
    @SubscribeEvent( priority = EventPriority.LOWEST )
    public static void onBlockBreak( BlockEvent.BreakEvent event ) {
        if( event.isCanceled() ) return;
        AIManager.onBlockBreak( event );
        VillagerAI.onBlockBreak( event );
    }
    
    /**
     * Called after an explosion has calculated targets, but before applying effects.
     *
     * @param event The event data.
     */
    @SubscribeEvent( priority = EventPriority.LOWEST )
    public static void onExplosionDetonate( ExplosionEvent.Detonate event ) { AIManager.onExplosionDetonate( event ); }

    @SubscribeEvent( priority = EventPriority.LOW )
    public static void onTrampleFarmland( BlockEvent.FarmlandTrampleEvent event ) {
        if ( !event.getLevel().isClientSide() && event.getEntity() instanceof ServerPlayer player ) {
            VillagerAI.onTrampleFarmland( player, event.getPos() );
        }
    }

    /**
     * Called when a container is about to be opened for a player.<br>
     * Should generally only be fired on the server, but who knows.
     *
     * @param event The event data.
     */
    @SubscribeEvent( priority = EventPriority.LOWEST )
    public static void onOpenContainer( PlayerContainerEvent.Open event ) {
        if ( event.getContainer() instanceof MerchantMenu merchantMenu ) {
            VillagerAI.onPlayerTryTrade( event, merchantMenu );
        }
    }
}