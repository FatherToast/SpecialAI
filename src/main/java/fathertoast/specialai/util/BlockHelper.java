package fathertoast.specialai.util;

import fathertoast.crust.api.lib.NBTHelper;
import fathertoast.specialai.SpecialAI;
import fathertoast.specialai.config.Config;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.EffectUtils;
import net.minecraft.potion.Effects;
import net.minecraft.tags.FluidTags;
import net.minecraft.tileentity.LockableLootTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.spawner.WorldEntitySpawner;
import net.minecraftforge.common.ForgeHooks;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Contains helper methods and info needed for block-breaking logic.
 */
public final class BlockHelper {
    
    private static final String TAG_HIDDEN_MOB = SpecialAI.MOD_ID + "_hiding";
    private static final String TAG_HIDE_DISABLED = SpecialAI.MOD_ID + "_hide_disabled";
    
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
    
    /**
     * Checks whether a mob can be hidden in a block.
     * <p>
     * @param world The world we live in. Absolutely mad.
     * @param pos   Position to hide at.
     * @return True if a mob can be hidden here.
     */
    public static boolean canHideMob( World world, BlockPos pos ) {
        TileEntity tileEntity = world.getBlockEntity( pos );
        if( tileEntity == null ) return false;
        
        CompoundNBT tag = tileEntity.getTileData();
        if( NBTHelper.containsNumber( tag, TAG_HIDE_DISABLED ) ) {
            if( tag.getBoolean( TAG_HIDE_DISABLED ) ) return false;
        }
        else if( tileEntity instanceof LockableLootTileEntity ) {
            tag.putBoolean( TAG_HIDE_DISABLED, !Config.IDLE.HIDING.lootableChance.rollChance( world.getRandom(), world, pos )
                    && NBTHelper.containsString( tileEntity.save( new CompoundNBT() ), "LootTable" ) );
        }
        return !NBTHelper.containsCompound( tag, TAG_HIDDEN_MOB );
    }
    
    /**
     * Hides a mob in a block. Prior to calling this, make sure the mob can be hidden here
     * via {@link #canHideMob(World, BlockPos)}.
     * <p>
     * @param world The world we live in. Absolutely mad.
     * @param pos   Position to hide at.
     * @param mob   The entity to hide.
     */
    public static void hideMob( World world, BlockPos pos, MobEntity mob ) {
        TileEntity tileEntity = world.getBlockEntity( pos );
        if( tileEntity == null ) return;
        
        if( mob.saveAsPassenger( NBTHelper.getOrCreateCompound( tileEntity.getTileData(), TAG_HIDDEN_MOB ) ) ) {
            // Successfully saved, remove the mob and play effects
            mob.spawnAnim();
            mob.remove();
        }
    }
    
    /**
     * Checks if there is a hiding mob. If so, unhides the mob and targets the entity that disturbed it.
     * <p>
     * @param world  The world we live in. Absolutely mad.
     * @param pos    Position to check for a hidden mob.
     * @param player The player triggering this check.
     * @param forceUnhide If true and a hidden mob is found, force the mob to spawn even
     *                    if there isn't really enough space for it.
     */
    public static void spawnHiddenMob( IWorld world, BlockPos pos, @Nullable PlayerEntity player, boolean forceUnhide ) {
        if( !(world instanceof ServerWorld) ) return;
        TileEntity tileEntity = world.getBlockEntity( pos );
        if( tileEntity == null ) return;
        ServerWorld serverWorld = (ServerWorld) world;
        
        // Get the tag if it exists
        CompoundNBT data = tileEntity.getTileData();
        if( !NBTHelper.containsCompound( data, TAG_HIDDEN_MOB ) ) return;
        CompoundNBT mobTag = data.getCompound( TAG_HIDDEN_MOB );
        
        // Validate and load from tag
        if( mobTag.isEmpty() || !NBTHelper.containsString( mobTag, "id" ) ) return;
        Optional<Entity> optional = EntityType.create( mobTag, serverWorld );
        if( !optional.isPresent() ) return;
        
        // Load successful!
        Entity mob = optional.get();;
        BlockPos spawnPos = pos.above();
        mob.setPos( spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D );

        // If we are not forcefully spawning the mob, check if it
        // has space to be placed above where it is hiding
        if ( !forceUnhide ) {
            // Check if the mob has space to unhide
            if (!world.noCollision(mob.getType().getAABB(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D))) {
                mob.remove();
                return;
            }
        }
        // Remove the mob data from tile entity nbt
        data.remove( TAG_HIDDEN_MOB );

        // Add the mob to the world and play effects
        serverWorld.addWithUUID( mob );

        if( mob instanceof MobEntity ) {
            if( player != null && !player.isSpectator() && player.isAlive() && ((MobEntity) mob).canAttack( player ) ) {
                ((MobEntity) mob).setTarget( player );
            }
            ((MobEntity) mob).spawnAnim();
        }
        Random random = world.getRandom();
        serverWorld.sendParticles( ParticleTypes.CLOUD,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 10,
                random.nextGaussian(), random.nextGaussian(), random.nextGaussian(), 0.1 );
    }
}