package fathertoast.specialai.util;

import fathertoast.crust.api.lib.EnvironmentHelper;
import fathertoast.specialai.SpecialAI;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

/**
 * Helps keeping track of block destroy progress made by mobs and cleaning up
 * "abandoned" destroy progress left behind when a mob dies or otherwise fails
 * to reset the destroy progress.
 */
@Mod.EventBusSubscriber( bus = Mod.EventBusSubscriber.Bus.FORGE, modid = SpecialAI.MOD_ID )
public class BlockDestroyTracker {

    private static final Map<ResourceKey<Level>, Queue<Entry>> ENTRIES_PER_LEVEL = new HashMap<>( 150 ); // No way there is ever 150 or MORE levels
    private static int timeNextUpdate = 0;

    /**
     * Adds a destroy progress entry to the queue of the given level.<br>
     * This method does not check for duplicate entries before insertion.
     */
    public static void putEntry( LivingEntity blockBreaker, Level level, BlockPos pos ) {
        ENTRIES_PER_LEVEL.get( level.dimension() ).add( new Entry( blockBreaker, GlobalPos.of( level.dimension(), pos ) ) );
    }

    /** Removes any found entries matching the given entity. */
    public static void removeEntryFor( LivingEntity entity, BlockPos pos ) {
        ENTRIES_PER_LEVEL.forEach( (dimension, queue) -> {
            if ( !queue.isEmpty() ) {
                queue.removeIf( (entry) -> entry.blockBreaker == entity && entry.pos.pos().equals( pos ) );
            }
        });
    }

    /**
     * Called at the end of each server tick.<br>
     * Updates the queue every 3 ticks and checks at most
     * 3 entries every cycle.
     */
    @SubscribeEvent
    public static void onServerTick( TickEvent.ServerTickEvent event ) {
        if ( event.phase == TickEvent.Phase.END && --timeNextUpdate <= 0 ) {
            for ( Level level : event.getServer().getAllLevels() ) {
                Queue<Entry> queue = ENTRIES_PER_LEVEL.get( level.dimension() );

                // Inspect 3 entries at a time per level, no need to go crazy here
                for ( int i = 0; i < 3; i++ ) {
                    if ( queue.isEmpty() ) break;

                    Entry entry = queue.peek();

                    // Is the entry expired? Reset destroy progress at location if possible.
                    if ( entry.isExpired() ) {
                        // Do not mess with the destroy progress in unloaded chunks
                        if ( EnvironmentHelper.isLoaded( level, entry.pos.pos() ) )
                            level.destroyBlockProgress( entry.blockBreaker.getId(), entry.pos.pos(), -1 );
                        queue.remove();
                    }
                }
            }
            timeNextUpdate = 3;
        }
    }

    /**
     * Called when the server has started.<br>
     * Populates the queue map for each loaded level.
     */
    @SubscribeEvent
    public static void onServerStarted( ServerStartedEvent event ) {
        for ( Level level : event.getServer().getAllLevels() ) {
            ENTRIES_PER_LEVEL.put( level.dimension(), new ArrayDeque<>() );
        }
    }

    /**
     * Called when the server has stopped.<br>
     * Clears the queue map.
     */
    @SubscribeEvent
    public static void onServerStopped( ServerStoppedEvent event ) {
        ENTRIES_PER_LEVEL.clear();
    }

    /** Contains info such as what entity is breaking a block, in what world, and where. */
    record Entry( LivingEntity blockBreaker, GlobalPos pos ) {

        /**
         *  @return True if the entity belonging to this entry
         *          is either dead or completely removed, or the
         *          entity changed level.
         */
        boolean isExpired() {
            return blockBreaker == null || !blockBreaker.isAlive() || !blockBreaker.level().dimension().equals( pos.dimension() );
        }
    }
}
