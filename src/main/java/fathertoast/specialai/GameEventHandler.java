package fathertoast.specialai;


import fathertoast.specialai.ai.AIManager;
import fathertoast.specialai.ai.griefing.IdleActionsGoal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.ChestContainer;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.tileentity.ChestTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.Random;

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
    public static void onLivingDeath( LivingDeathEvent event ) {
        AIManager.onLivingDeath( event );
    }

    /**
     * Called when a player opens a container.
     *
     * @param event The event data.
     */
    @SubscribeEvent( priority =  EventPriority.LOW )
    public static void onContainerOpen( PlayerContainerEvent.Open event ) {
        if ( event.getContainer() instanceof ChestContainer ) {
            ChestContainer container = (ChestContainer) event.getContainer();
            ChestTileEntity chestTileEntity = null;

            if ( container.getContainer() instanceof ChestTileEntity ) {
                if ( ((ChestTileEntity) container.getContainer() ).getTileData().contains( IdleActionsGoal.HIDDEN_MOB_TAG, Constants.NBT.TAG_COMPOUND ))
                    chestTileEntity = (ChestTileEntity) container.getContainer();
            }

            // TODO - check out what double chest containers are actually called in 1.16.5
            /*
            else if (container.getContainer() instanceof CompoundContainer compoundContainer) {
                Container container = event.getEntity().level.random.nextBoolean() ? compoundContainer.container1 : compoundContainer.container2;

                if (container instanceof ChestBlockEntity chest && chest.getPersistentData().contains(CreeperChestHideGoal.HIDDEN_CREEPER_TAG, Tag.TAG_BYTE)) {
                    chestBlockEntity = chest;
                }
            }

             */
            if ( chestTileEntity != null ) {
                spawnHiddenFromChest( chestTileEntity, event.getPlayer() );
            }
        }
    }

    /**
     * Called right before a block is destroyed.
     *
     * @param event The event data.
     */
    @SubscribeEvent( priority = EventPriority.LOWEST )
    public static void onBlockBreak( BlockEvent.BreakEvent event ) {
        if ( event.isCanceled() )
            return;

        PlayerEntity player = event.getPlayer();
        TileEntity te = player.level.getBlockEntity( event.getPos() );

        if ( te instanceof ChestTileEntity ) {
            spawnHiddenFromChest( ( ChestTileEntity ) te, player );
        }
    }



    private static void spawnHiddenFromChest( @Nonnull ChestTileEntity chestTileEntity, PlayerEntity player ) {
        CompoundNBT data = chestTileEntity.getTileData();

        if ( data.contains( IdleActionsGoal.HIDDEN_MOB_TAG, Constants.NBT.TAG_COMPOUND ) ) {
            CompoundNBT mobTag = data.getCompound( IdleActionsGoal.HIDDEN_MOB_TAG );

            if (mobTag.isEmpty())
                return;

            Optional<Entity> optional = EntityType.create( mobTag, player.level );
            BlockPos pos = chestTileEntity.getBlockPos().above();

            // NOTE: this shit doesn't work!
            optional.ifPresent((entity) -> {
                if ( !player.level.isClientSide ) {
                    ServerWorld serverWorld = ( ServerWorld ) player.level;
                    serverWorld.addFreshEntity( entity );

                    Random random = player.level.random;

                    serverWorld.sendParticles( ParticleTypes.CLOUD,
                            pos.getX() + 0.5D,
                            pos.getY() + 0.5D,
                            pos.getZ() + 0.5D,
                            10,
                            random.nextGaussian(), random.nextGaussian(), random.nextGaussian(),
                            0.1D );
                }
            });
        }
    }
}