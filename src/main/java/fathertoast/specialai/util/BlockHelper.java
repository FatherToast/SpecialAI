package fathertoast.specialai.util;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.EffectUtils;
import net.minecraft.potion.Effects;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;

/**
 * Contains helper methods and info needed for block-breaking logic.
 */
public final class BlockHelper {
    
    /** @return Returns true if the entity can target the block. */
    public static boolean shouldDamage( BlockState block, MobEntity entity, boolean needsTool, World world, BlockPos pos ) {
        return block.getDestroySpeed( world, pos ) >= 0.0F && !block.getMaterial().isLiquid() &&
                (!needsTool || BlockHelper.hasCorrectTool( entity.getMainHandItem(), block )) &&
                ForgeHooks.canEntityDestroy( entity.level, pos, entity );
    }
    
    /**
     * @return Returns the percentage (0 to 1) block destruction progress to make per tick.
     * @see net.minecraft.block.AbstractBlock#getDestroyProgress(BlockState, PlayerEntity, IBlockReader, BlockPos)
     */
    @SuppressWarnings( "deprecation" )
    public static float getDestroyProgress( BlockState block, MobEntity entity, World world, BlockPos pos ) {
        float hardness = block.getDestroySpeed( world, pos );
        if( hardness < 0.0F ) return 0.0F; // Special case for unbreakable blocks (like bedrock)
        
        final float effectiveHardness = hardness * (BlockHelper.hasCorrectTool( entity.getMainHandItem(), block ) ? 30.0F : 100.0F);
        return BlockHelper.getDigSpeed( entity, block ) / effectiveHardness;
    }
    
    /**
     * @param tool  The item being used to dig with.
     * @param block The block state to dig.
     * @return Returns true if the tool is valid for the target block. Analogous to PlayerEntity::hasCorrectToolForDrops.
     * @see net.minecraft.entity.player.PlayerEntity#hasCorrectToolForDrops(BlockState)
     */
    public static boolean hasCorrectTool( ItemStack tool, BlockState block ) {
        return !block.requiresCorrectToolForDrops() || tool.isCorrectToolForDrops( block );
    }
    
    /**
     * @param entity The digging entity.
     * @param state  The block state to dig.
     * @return Returns the mob's dig speed against the target block. Analogous to PlayerEntity::getDigSpeed.
     * @see net.minecraft.entity.player.PlayerEntity#getDigSpeed(BlockState, BlockPos)
     */
    public static float getDigSpeed( MobEntity entity, BlockState state ) {
        // Get held item's base speed
        ItemStack held = entity.getMainHandItem();
        float digSpeed = held.isEmpty() ? 1.0F : held.getDestroySpeed( state );
        
        // Apply Efficiency enchantment
        if( digSpeed > 1.0F ) {
            int efficiency = EnchantmentHelper.getBlockEfficiency( entity );
            if( efficiency > 0 ) {
                digSpeed += efficiency * efficiency + 1;
            }
        }
        
        // Apply potion effects
        if( EffectUtils.hasDigSpeed( entity ) ) {
            digSpeed *= 1.0F + (float) (EffectUtils.getDigSpeedAmplification( entity ) + 1) * 0.2F;
        }
        if( entity.hasEffect( Effects.DIG_SLOWDOWN ) ) {
            digSpeed *= getDigSpeedSlowdown( entity.getEffect( Effects.DIG_SLOWDOWN ) );
        }
        
        // Apply environment effects
        if( entity.isEyeInFluid( FluidTags.WATER ) && !EnchantmentHelper.hasAquaAffinity( entity ) ) {
            digSpeed /= 5.0F;
        }
        if( !entity.isOnGround() ) {
            digSpeed /= 5.0F;
        }
        
        // Normally, the player fires a Forge event here that can modify break speed - it only accepts a player, so we skip it
        return Math.max( digSpeed, 0.0F );
    }
    
    /** @return Returns the dig speed multiplier based on the given dig slowdown (mining fatigue) effect. */
    private static float getDigSpeedSlowdown( EffectInstance effect ) {
        if( effect == null ) return 1.0F;
        switch( effect.getAmplifier() ) {
            case 0:
                return 0.3F;
            case 1:
                return 0.09F;
            case 2:
                return 0.0027F;
            case 3:
            default:
                return 8.1E-4F;
        }
    }
    
    /**
     * This class plays various 'level effect' events of interest and stores data needed to communicate the event to the client.
     * These level events do not use metadata. For the events that do, see LevelEventMeta.
     *
     * @see net.minecraft.client.renderer.WorldRenderer#levelEvent(PlayerEntity, int, BlockPos, int)
     */
    public enum LevelEvent {
        /** @see net.minecraft.util.SoundEvents#DISPENSER_LAUNCH */
        DISPENSER_LAUNCH( 1002 ),
        /** @see net.minecraft.util.SoundEvents#ZOMBIE_ATTACK_WOODEN_DOOR */
        ATTACK_DOOR_WOOD( 1019 ),
        /** @see net.minecraft.util.SoundEvents#ZOMBIE_ATTACK_IRON_DOOR */
        ATTACK_DOOR_IRON( 1020 ),
        /** @see net.minecraft.util.SoundEvents#ZOMBIE_BREAK_WOODEN_DOOR */
        BREAK_DOOR_WOOD( 1021 ),
        /** Spawns a burst of smoke and flame particles. */
        SPAWNER_PARTICLES( 2004 ),
        /** @see net.minecraft.item.BoneMealItem#addGrowthParticles(IWorld, BlockPos, int) */
        BONEMEAL_PARTICLES( 2005 );
        
        /** The event id, as it appears in WorldRenderer#levelEvent(). */
        private final int EVENT_ID;
        
        LevelEvent( int eventId ) { EVENT_ID = eventId; }
        
        /** Plays the event for an entity at that entity's position. */
        public final void play( Entity entity ) { play( entity.level, entity.blockPosition() ); }
        
        /** Plays the event for an entity at a specified position. */
        public final void play( Entity entity, BlockPos pos ) { play( entity.level, pos ); }
        
        /** Plays the event in the world at a specified position. */
        public final void play( World world, BlockPos pos ) { world.levelEvent( EVENT_ID, pos, 0 ); }
    }
    
    /**
     * This class contains ids for various 'level effect' events of interest and helper methods to play those events.
     *
     * @see net.minecraft.client.renderer.WorldRenderer#levelEvent(PlayerEntity, int, BlockPos, int)
     */
    public static final class LevelEventMeta {
        /** Plays the break sound and spawns a burst of block particles for the block. Metadata is block state id. */
        private static final int BREAK_BLOCK = 2001;
        
        /** Plays the event for an entity at a specified position. */
        public static void playBreakBlock( Entity entity, BlockPos pos ) { playBreakBlock( entity.level, pos ); }
        
        /** Plays the event in the world at a specified position. */
        public static void playBreakBlock( World world, BlockPos pos ) { play( BREAK_BLOCK, world, pos, Block.getId( world.getBlockState( pos ) ) ); }
        
        /** Plays the event in the world at a specified position with metadata. */
        @SuppressWarnings( "SameParameterValue" )
        private static void play( int eventId, World world, BlockPos pos, int meta ) { world.levelEvent( eventId, pos, meta ); }
    }
    
    // This is a static-only helper class.
    private BlockHelper() {}
}