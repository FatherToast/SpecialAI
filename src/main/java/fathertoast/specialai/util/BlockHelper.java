package fathertoast.specialai.util;

import net.minecraft.block.BlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.EffectUtils;
import net.minecraft.potion.Effects;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;

import javax.annotation.Nullable;

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
    
    /**
     * @return Returns the dig speed multiplier based on the given dig slowdown (mining fatigue) effect.
     * @see PlayerEntity#getDigSpeed(BlockState, BlockPos)
     */
    private static float getDigSpeedSlowdown( @Nullable EffectInstance effect ) {
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
}