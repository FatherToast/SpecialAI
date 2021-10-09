package fathertoast.specialai.util;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;

/**
 * Contains helper methods and info needed for block-breaking logic.
 */
public final class BlockHelper {
    
    /** @return Returns true if the entity can target the block. */
    public static boolean shouldDamage(BlockState block, Mob entity, boolean needsTool, Level level, BlockPos pos ) {
        return block.getDestroySpeed( level, pos ) >= 0.0F && !block.getMaterial().isLiquid() &&
                (!needsTool || BlockHelper.hasCorrectTool( entity.getMainHandItem(), block )) &&
                ForgeHooks.canEntityDestroy( entity.level, pos, entity );
    }
    
    /**
     * @return Returns the percentage (0 to 1) block destruction progress to make per tick.
     * @see net.minecraft.world.level.block.state.BlockBehaviour#getDestroyProgress(BlockState, Player, BlockGetter, BlockPos)
     */
    @SuppressWarnings( "deprecation" )
    public static float getDestroyProgress( BlockState block, Mob entity, Level level, BlockPos pos ) {
        float hardness = block.getDestroySpeed( level, pos );
        if( hardness < 0.0F ) return 0.0F; // Special case for unbreakable blocks (like bedrock)
        
        final float effectiveHardness = hardness * (BlockHelper.hasCorrectTool( entity.getMainHandItem(), block ) ? 30.0F : 100.0F);
        return BlockHelper.getDigSpeed( entity, block ) / effectiveHardness;
    }
    
    /**
     * @param tool  The item being used to dig with.
     * @param block The block state to dig.
     * @return Returns true if the tool is valid for the target block. Analogous to PlayerEntity::hasCorrectToolForDrops.
     * @see net.minecraft.world.entity.player.Player#hasCorrectToolForDrops(BlockState)
     */
    public static boolean hasCorrectTool( ItemStack tool, BlockState block ) {
        return !block.requiresCorrectToolForDrops() || tool.isCorrectToolForDrops( block );
    }
    
    /**
     * @param entity The digging entity.
     * @param state  The block state to dig.
     * @return Returns the mob's dig speed against the target block. Analogous to PlayerEntity::getDigSpeed.
     * @see net.minecraft.world.entity.player.Player#getDigSpeed(BlockState, BlockPos)
     */
    public static float getDigSpeed( Mob entity, BlockState state ) {
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
        if( MobEffectUtil.hasDigSpeed( entity ) ) {
            digSpeed *= 1.0F + (float) (MobEffectUtil.getDigSpeedAmplification( entity ) + 1) * 0.2F;
        }
        if( entity.hasEffect( MobEffects.DIG_SLOWDOWN ) ) {
            digSpeed *= getDigSpeedSlowdown( entity.getEffect( MobEffects.DIG_SLOWDOWN ) );
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
    private static float getDigSpeedSlowdown( MobEffectInstance effect ) {
        if( effect == null ) return 1.0F;

        return switch (effect.getAmplifier()) {
            case 0 -> 0.3F;
            case 1 -> 0.09F;
            case 2 -> 0.0027F;
            default -> 8.1E-4F;
        };
    }
    
    /**
     * This class plays various 'level effect' events of interest and stores data needed to communicate the event to the client.
     * These level events do not use metadata. For the events that do, see LevelEventMeta.
     *
     * @see net.minecraft.client.renderer.LevelRenderer#levelEvent(Player, int, BlockPos, int)
     */
    public enum LevelEvent {
        /** @see net.minecraft.sounds.SoundEvents#DISPENSER_LAUNCH */
        DISPENSER_LAUNCH( 1002 ),
        /** @see net.minecraft.sounds.SoundEvents#ZOMBIE_ATTACK_WOODEN_DOOR */
        ATTACK_DOOR_WOOD( 1019 ),
        /** @see net.minecraft.sounds.SoundEvents#ZOMBIE_ATTACK_IRON_DOOR */
        ATTACK_DOOR_IRON( 1020 ),
        /** @see net.minecraft.sounds.SoundEvents#ZOMBIE_BREAK_WOODEN_DOOR */
        BREAK_DOOR_WOOD( 1021 ),
        /** Spawns a burst of smoke and flame particles. */
        SPAWNER_PARTICLES( 2004 ),
        /** @see net.minecraft.world.item.BoneMealItem#addGrowthParticles(LevelAccessor, BlockPos, int) */
        BONEMEAL_PARTICLES( 2005 );
        
        /** The event id, as it appears in LevelRenderer#levelEvent(). */
        private final int EVENT_ID;
        
        LevelEvent( int eventId ) { EVENT_ID = eventId; }
        
        /** Plays the event for an entity at that entity's position. */
        public final void play( Entity entity ) { play( entity.level, entity.blockPosition() ); }
        
        /** Plays the event for an entity at a specified position. */
        public final void play( Entity entity, BlockPos pos ) { play( entity.level, pos ); }
        
        /** Plays the event in the world at a specified position. */
        public final void play( Level level, BlockPos pos ) { level.levelEvent( EVENT_ID, pos, 0 ); }
    }
    
    /**
     * This class contains ids for various 'level effect' events of interest and helper methods to play those events.
     *
     * @see net.minecraft.client.renderer.LevelRenderer#levelEvent(Player, int, BlockPos, int)
     */
    public static final class LevelEventMeta {
        /** Plays the break sound and spawns a burst of block particles for the block. Metadata is block state id. */
        private static final int BREAK_BLOCK = 2001;
        
        /** Plays the event for an entity at a specified position. */
        public static void playBreakBlock( Entity entity, BlockPos pos ) { playBreakBlock( entity.level, pos ); }
        
        /** Plays the event in the world at a specified position. */
        public static void playBreakBlock( Level world, BlockPos pos ) { play( BREAK_BLOCK, world, pos, Block.getId( world.getBlockState( pos ) ) ); }
        
        /** Plays the event in the world at a specified position with metadata. */
        @SuppressWarnings( "SameParameterValue" )
        private static void play( int eventId, Level world, BlockPos pos, int meta ) { world.levelEvent( eventId, pos, meta ); }
    }
    
    // This is a static-only helper class.
    private BlockHelper() {}
}