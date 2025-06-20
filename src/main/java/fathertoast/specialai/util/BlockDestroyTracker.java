package fathertoast.specialai.util;

import fathertoast.crust.api.lib.EnvironmentHelper;
import fathertoast.specialai.SpecialAI;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.codehaus.plexus.util.FastMap;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Helps keeping track of block destroy progress made by mobs and cleaning up
 * "abandoned" destroy progress left behind when a mob dies or otherwise fails
 * to reset the destroy progress.
 */
@Mod.EventBusSubscriber( bus = Mod.EventBusSubscriber.Bus.FORGE, modid = SpecialAI.MOD_ID )
public class BlockDestroyTracker {

    private static final FastMap<Level, Deque<Entry>> ENTRIES_PER_LEVEL = new FastMap<>( 150 ); // No way there is ever 150 or MORE levels
    private static int timeNextUpdate = 0;

    /**
     * Adds a destroy progress entry to the queue of the given level.<br>
     * Note that duplicate checks are not done.
     */
    public static void putEntry( LivingEntity blockBreaker, Level level, BlockPos pos ) {
        ENTRIES_PER_LEVEL.get( level ).add( new Entry( blockBreaker, level, pos ) );
    }

    /** Removes any found entries matching the given entity and block position. */
    public static void removeEntryFor( LivingEntity entity, BlockPos pos ) {
        ENTRIES_PER_LEVEL.forEach( (level, deque) -> {
            if ( !deque.isEmpty() ) {
                deque.removeIf( (entry) -> entry.blockBreaker == entity && entry.pos.equals( pos ) );
            }
        });
    }

    /**
     * Called at the end of each server tick.<br>
     * Updates the deques every 3 ticks and checks at most
     * 3 entries every cycle.
     */
    @SubscribeEvent
    public static void onServerTick( TickEvent.ServerTickEvent event ) {
        if ( event.phase == TickEvent.Phase.END && --timeNextUpdate <= 0 ) {
            for ( Level level : event.getServer().getAllLevels() ) {
                Deque<Entry> deque = ENTRIES_PER_LEVEL.get( level );

                // Inspect 3 entries at a time per level, no need to go crazy here
                for ( int i = 0; i < 3; i++ ) {
                    if ( deque.isEmpty() ) break;

                    Entry entry = deque.peek();

                    if ( entry.isExpired() ) {
                        // Do not mess with the destroy progress in unloaded chunks
                        if ( EnvironmentHelper.isLoaded( level, entry.pos ) )
                            level.destroyBlockProgress( entry.blockBreaker.getId(), entry.pos, -1 );
                        deque.pollFirst();
                    }
                }
            }
            timeNextUpdate = 3;
        }
    }

    /**
     * Called when the server has started.<br>
     * Populates the deque map for each loaded level.
     */
    @SubscribeEvent
    public static void onServerStarted( ServerStartedEvent event ) {
        for ( Level level : event.getServer().getAllLevels() ) {
            ENTRIES_PER_LEVEL.put( level, new ArrayDeque<>() );
        }
    }

    /**
     * Called when the server has stopped.<br>
     * Clears the deque map.
     */
    @SubscribeEvent
    public static void onServerStopped( ServerStoppedEvent event ) {
        ENTRIES_PER_LEVEL.clear();
    }

    /** Contains info such as what entity is breaking a block, in what world, and where. */
    record Entry( LivingEntity blockBreaker, Level level, BlockPos pos ) {

        /**
         *  @return True if the entity belonging to this entry
         *          is either dead or completely removed, or the
         *          entity changed level.
         */
        boolean isExpired() {
            return blockBreaker == null || !blockBreaker.isAlive() || blockBreaker.level() != level;
        }
    }
}
