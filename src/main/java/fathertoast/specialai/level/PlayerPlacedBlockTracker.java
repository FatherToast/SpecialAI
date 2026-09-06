package fathertoast.specialai.level;

import fathertoast.crust.api.lib.DeferredAction;
import fathertoast.crust.api.lib.NBTHelper;
import fathertoast.specialai.core.SpecialAI;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.VanillaGameEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ChunkDataEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.Arrays;

/**
 * Keeps track of blocks that have been placed by a player, with the goal
 * of being able to distinguish them from natural/world-gen blocks.
 */
@Mod.EventBusSubscriber( bus = Mod.EventBusSubscriber.Bus.FORGE, modid = SpecialAI.MOD_ID )
public final class PlayerPlacedBlockTracker {
    
    /** @return True if the block at the position was NOT placed by a player. */
    public static boolean isNotPlacedByPlayer( Level level, BlockPos pos ) {
        Slice slice = getSlice( level, pos, false );
        return slice == null || !slice.get( pos );
    }
    
    /** Sets the block at a position as either placed by a plyer, or not. */
    public static void setPlacedByPlayer( LevelAccessor level, BlockPos pos, boolean value ) {
        if( level instanceof ServerLevelAccessor access ) setPlacedByPlayer( access.getLevel(), pos, value );
    }
    
    /** Sets the block at a position as either placed by a plyer, or not. */
    public static void setPlacedByPlayer( Level level, BlockPos pos, boolean value ) {
        Slice slice = getSlice( level, pos, value );
        if( slice != null ) slice.set( pos, value );
    }
    
    
    // ---- Block Event Tracking ---- // Note: Some tracking is done via AIManager
    
    /**
     * Called when a block is placed. If canceled, the block will not be placed.
     *
     * @param event The event data.
     */
    @SubscribeEvent( priority = EventPriority.NORMAL )
    public static void onEntityPlaceBlock( BlockEvent.EntityPlaceEvent event ) {
        if( !event.isCanceled() && !event.getLevel().isClientSide() &&
                event.getLevel() instanceof ServerLevelAccessor access ) {
            setPlacedByPlayer( access.getLevel(), event.getPos(),
                    isPlayerPlacing( event.getPlacedBlock(), event.getEntity() ) );
        }
    }
    
    /**
     * Called when a vanilla game event is triggered. If canceled, the event will not be broadcast.
     *
     * @param event The event data.
     */
    @SubscribeEvent( priority = EventPriority.NORMAL )
    public static void onVanillaGameEvent( VanillaGameEvent event ) {
        if( event.getVanillaEvent() == GameEvent.BLOCK_DESTROY ) {
            setPlacedByPlayer( event, false );
        }
        else if( event.getVanillaEvent() == GameEvent.BLOCK_CHANGE ||
                event.getVanillaEvent() == GameEvent.BLOCK_PLACE ) {
            GameEvent.Context context = event.getContext();
            setPlacedByPlayer( event, isPlayerPlacing( context.affectedState(), context.sourceEntity() ) );
        }
    }
    
    /** Sets the block referenced by a vanilla game event as either placed by a plyer, or not. */
    private static void setPlacedByPlayer( VanillaGameEvent event, boolean value ) {
        setPlacedByPlayer( event.getLevel(), BlockPos.containing( event.getEventPosition() ), value );
    }
    
    /** @return True if the block is non-air and the entity is a real player. */
    private static boolean isPlayerPlacing( @Nullable BlockState block, @Nullable Entity entity ) {
        return block != null && !block.isAir() && entity instanceof Player && !(entity instanceof FakePlayer);
    }
    
    
    // ---- Data Saving/Loading ---- //
    
    /**
     * Called during chunk loading (async).
     *
     * @param event The event data.
     */
    @SubscribeEvent( priority = EventPriority.NORMAL )
    public static void onChunkDataLoad( ChunkDataEvent.Load event ) {
        if( NBTHelper.containsCompound( event.getData(), SpecialAI.MOD_ID ) ) {
            LevelCache levelCache = getLevel( event, true );
            if( levelCache != null ) {
                // Load the chunk cache from NBT
                final CompoundTag saiTag = event.getData().getCompound( SpecialAI.MOD_ID );
                final long pos = event.getChunk().getPos().toLong();
                final ChunkCache chunkCache = new ChunkCache();
                for( String sY : saiTag.getAllKeys() ) {
                    int y;
                    try { y = Integer.parseInt( sY ); }
                    catch( NumberFormatException ex ) { continue; } // Tag isn't a valid slice
                    
                    chunkCache.cacheSlice( y, saiTag.getIntArray( sY ) );
                }
                // Queue chunk to be added on main/server thread
                if( chunkCache.hasData() ) {
                    DeferredAction.queue( () -> levelCache.cacheChunk( pos, chunkCache ) );
                }
            }
        }
    }
    
    /**
     * Called during chunk saving.
     *
     * @param event The event data.
     */
    @SubscribeEvent( priority = EventPriority.NORMAL )
    public static void onChunkDataSave( ChunkDataEvent.Save event ) {
        ChunkCache chunkCache = getChunk( event );
        if( chunkCache != null ) chunkCache.save( event.getData() );
    }
    
    /**
     * Called during chunk unloading.
     *
     * @param event The event data.
     */
    @SubscribeEvent( priority = EventPriority.NORMAL )
    public static void onChunkUnload( ChunkEvent.Unload event ) {
        if( !event.getLevel().isClientSide() ) {
            LevelCache levelCache = getLevel( event, false );
            if( levelCache != null ) levelCache.removeChunk( event );
        }
    }
    
    /**
     * Called during level unloading.
     *
     * @param event The event data.
     */
    @SubscribeEvent( priority = EventPriority.NORMAL )
    public static void onLevelUnload( LevelEvent.Unload event ) {
        if( !event.getLevel().isClientSide() ) {
            ResourceKey<Level> dim = getDimension( event );
            if( dim != null ) LEVEL_MAP.remove( dim );
        }
    }
    
    /**
     * Called after the server has completely stopped.
     *
     * @param event The event data.
     */
    @SubscribeEvent( priority = EventPriority.NORMAL )
    public static void onLevelUnload( ServerStoppedEvent event ) {
        // Should already be cleared by each individual level unloading, but clear just in case
        LEVEL_MAP.clear();
    }
    
    
    // ---- Data Storage ---- //
    
    /** All level caches, mapped to their dimensions. */
    private static final Object2ObjectOpenHashMap<ResourceKey<Level>, LevelCache> LEVEL_MAP = new Object2ObjectOpenHashMap<>( 64 );
    
    /** @return The level event's dimension, if it exists. */
    @Nullable
    private static ResourceKey<Level> getDimension( LevelEvent event ) {
        return event.getLevel() instanceof ServerLevelAccessor access ? access.getLevel().dimension() : null;
    }
    
    /**
     * @return The specified level cache, if it exists.
     * If forced, a new level cache will be made instead of returning null;
     * however, if the event's dimension is null, this will always return null.
     */
    @Nullable
    private static LevelCache getLevel( LevelEvent event, boolean force ) { return getLevel( getDimension( event ), force ); }
    
    /**
     * @return The specified level cache, if it exists.
     * If forced, a new level cache will be made instead of returning null;
     * however, if the dimension is null, this will always return null.
     */
    @Nullable
    private static LevelCache getLevel( @Nullable ResourceKey<Level> dimension, boolean force ) {
        if( dimension == null ) return null;
        LevelCache levelCache = LEVEL_MAP.get( dimension );
        return levelCache == null && force ? makeLevel( dimension ) : levelCache;
    }
    
    /** @return A new level cache for the dimension. */
    private static LevelCache makeLevel( ResourceKey<Level> dimension ) {
        LevelCache levelCache = new LevelCache();
        LEVEL_MAP.put( dimension, levelCache );
        return levelCache;
    }
    
    /** @return The chunk cache referenced by a particular event, if it exists. */
    @Nullable
    private static ChunkCache getChunk( ChunkEvent event ) {
        LevelCache levelCache = getLevel( event, false );
        return levelCache != null ? levelCache.getChunk( event ) : null;
    }
    
    /** @return The specified chunk cache, if it exists. */
    @Nullable
    private static ChunkCache getChunk( Level level, int x, int z ) {
        LevelCache levelCache = getLevel( level.dimension(), false );
        return levelCache != null ? levelCache.getChunk( ChunkPos.asLong( x, z ), false ) : null;
    }
    
    /**
     * @return The slice containing a particular block position, if it exists.
     * If forced, a new slice will be made instead of returning null;
     * however, if the dimension is null, this will always return null.
     */
    @Nullable
    private static Slice getSlice( Level level, BlockPos pos, boolean force ) {
        LevelCache levelCache = getLevel( level.dimension(), force );
        if( levelCache == null ) return null;
        
        ChunkCache chunkCache = levelCache.getChunk( pos, force );
        if( chunkCache == null ) return null;
        
        return chunkCache.getSlice( pos, force );
    }
    
    /**
     * Represents one level; that is, an entire dimension.
     * This is basically a map of loaded {@link ChunkCache}s.
     */
    private static class LevelCache {
        /** All chunk caches in this level cache, mapped to their chunk positions. */
        final Long2ObjectOpenHashMap<ChunkCache> CHUNK_MAP = new Long2ObjectOpenHashMap<>( 64 );
        
        /** @return The chunk cache referenced by a particular event, if it exists. */
        @Nullable
        ChunkCache getChunk( ChunkEvent event ) {
            return getChunk( event.getChunk().getPos().toLong(), false );
        }
        
        /**
         * @return The chunk cache containing a particular block position, if it exists.
         * If forced, a new chunk cache will be made instead of returning null.
         */
        @Nullable
        ChunkCache getChunk( BlockPos pos, boolean force ) {
            return getChunk( ChunkPos.asLong( pos ), force );
        }
        
        /**
         * @return The specified chunk cache from this level cache, if it exists.
         * If forced, a new chunk cache will be made instead of returning null.
         */
        @Nullable
        ChunkCache getChunk( long pos, boolean force ) {
            ChunkCache chunkCache = CHUNK_MAP.get( pos );
            return chunkCache == null && force ? makeChunk( pos ) : chunkCache;
        }
        
        /** @return A new chunk cache from this level cache. */
        ChunkCache makeChunk( long pos ) {
            ChunkCache chunkCache = new ChunkCache();
            CHUNK_MAP.put( pos, chunkCache );
            return chunkCache;
        }
        
        /** Stores a chunk cache in this level cache. */
        boolean cacheChunk( long pos, ChunkCache chunkCache ) {
            CHUNK_MAP.put( pos, chunkCache );
            return true; // Because we queue this as a deferred action (marks the action as completed)
        }
        
        /** Removes the chunk cache referenced by a particular event, if it exists. */
        void removeChunk( ChunkEvent event ) { CHUNK_MAP.remove( event.getChunk().getPos().toLong() ); }
        
        //        /** @return True if any chunk caches in this level cache have data. */
        //        boolean hasData() {
        //            if( !CHUNK_MAP.isEmpty() ) {
        //                for( ChunkCache chunkCache : CHUNK_MAP.values() ) if( chunkCache.hasData() ) return true;
        //            }
        //            return false;
        //        }
    }
    
    /**
     * Represents one chunk; that is, a 16xHx16-block region (where H = level height).
     * This is basically a stack of {@link Slice}s.
     */
    private static class ChunkCache {
        /** All slices in this chunk cache, mapped to their y-values. */
        final Int2ObjectOpenHashMap<Slice> SLICE_MAP = new Int2ObjectOpenHashMap<>();
        
        /**
         * @return The slice containing a particular block position, if it exists.
         * If forced, a new slice will be made instead of returning null.
         */
        @Nullable
        Slice getSlice( BlockPos pos, boolean force ) { return getSlice( pos.getY(), force ); }
        
        /**
         * @return The specified slice from this chunk cache, if it exists.
         * If forced, a new slice will be made instead of returning null.
         */
        @Nullable
        Slice getSlice( int y, boolean force ) {
            Slice slice = SLICE_MAP.get( y );
            return slice == null && force ? makeSlice( y ) : slice;
        }
        
        /** @return A new slice from this chunk cache. */
        Slice makeSlice( int y ) {
            Slice slice = new Slice();
            SLICE_MAP.put( y, slice );
            return slice;
        }
        
        /** Creates a new slice with the provided data and stores it in this chunk cache. */
        void cacheSlice( int y, int[] data ) { SLICE_MAP.put( y, new Slice( data ) ); }
        
        /** @return True if any slices in this chunk cache have data. */
        boolean hasData() {
            if( !SLICE_MAP.isEmpty() ) {
                for( Slice slice : SLICE_MAP.values() ) if( slice.hasData() ) return true;
            }
            return false;
        }
        
        /** Saves this chunk cache to NBT, if needed. */
        void save( CompoundTag tag ) {
            if( hasData() ) {
                final CompoundTag saiTag = NBTHelper.getOrCreateCompound( tag, SpecialAI.MOD_ID );
                SLICE_MAP.forEach( ( y, slice ) -> slice.save( saiTag, y ) );
            }
        }
    }
    
    /**
     * Represents one horizontal "slice" of a chunk; that is, a 16x1x16-block region.
     */
    private static class Slice {
        /**
         * This slice's data. This is an array of ints, where each int holds data for 32 blocks (two rows)
         * in a chunk. High bits (1) represent blocks that have been placed by a player.
         */
        int[] data;
        
        /** Creates a new, empty slice. */
        Slice() { data = new int[8]; }
        
        /** Creates a new slice with the provided data. */
        Slice( int[] d ) { data = Arrays.copyOf( d, 8 ); }
        
        /** Sets the flag for a block position. */
        void set( BlockPos pos, boolean flag ) { set( rel( pos.getX() ), rel( pos.getZ() ), flag ); }
        
        /** Sets the flag for an x-z coordinate (0-15 each coord). */
        void set( int x, int z, boolean flag ) {
            if( flag ) data[index( z )] |= mask( x, z );
            else data[index( z )] &= ~mask( x, z );
        }
        
        /** @return The flag for a block position. */
        boolean get( BlockPos pos ) { return get( rel( pos.getX() ), rel( pos.getZ() ) ); }
        
        /** @return The flag for an x-z coordinate (0-15 each coord). */
        boolean get( int x, int z ) { return (data[index( z )] & mask( x, z )) != 0; }
        
        /** @return True if any flags in this slice are set to true. */
        boolean hasData() {
            for( int flags : data ) if( flags != 0 ) return true;
            return false;
        }
        
        /** @return The relative position of a block position coordinate within its chunk/section. */
        int rel( int b ) { return SectionPos.sectionRelative( b ); }
        
        /** @return The flag mask for the relative position coordinates. */
        int mask( int x, int z ) { return (z & 1) == 0 ? 1 << x : 1 << x << 16; }
        
        /** @return The data index for the relative z coordinate. */
        int index( int z ) { return z >> 1; }
        
        /** Saves this slice to NBT, if needed. */
        void save( CompoundTag tag, int y ) {
            if( hasData() ) {
                tag.putIntArray( Integer.toString( y ), data );
            }
        }
    }
}