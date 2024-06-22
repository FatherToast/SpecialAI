package fathertoast.specialai.util;

import fathertoast.crust.api.lib.NBTHelper;
import fathertoast.specialai.SpecialAI;
import fathertoast.specialai.config.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.ForgeMod;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Contains helper methods and info needed for block-breaking logic.
 */
public final class BlockHelper {
    
    private static final String TAG_HIDDEN_MOB = SpecialAI.MOD_ID + "_hiding";
    private static final String TAG_HIDE_DISABLED = SpecialAI.MOD_ID + "_hide_disabled";
    
    /** @return Returns true if the entity can target the block. */
    public static boolean shouldDamage(BlockState block, Mob entity, boolean needsTool, Level level, BlockPos pos ) {
        return block.getDestroySpeed( level, pos ) >= 0.0F && !block.liquid() &&
                (!needsTool || BlockHelper.hasCorrectTool( entity.getMainHandItem(), block )) &&
                ForgeHooks.canEntityDestroy( entity.level(), pos, entity );
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
            digSpeed *= 1.0F + (float) ( MobEffectUtil.getDigSpeedAmplification( entity ) + 1 ) * 0.2F;
        }
        if( entity.hasEffect( MobEffects.DIG_SLOWDOWN ) ) {
            digSpeed *= getDigSpeedSlowdown( entity.getEffect( MobEffects.DIG_SLOWDOWN ) );
        }
        
        // Apply environment effects
        if( entity.isEyeInFluidType( ForgeMod.WATER_TYPE.get() ) && !EnchantmentHelper.hasAquaAffinity( entity ) ) {
            digSpeed /= 5.0F;
        }
        if( !entity.onGround() ) {
            digSpeed /= 5.0F;
        }
        
        // Normally, the player fires a Forge event here that can modify break speed - it only accepts a player, so we skip it
        return Math.max( digSpeed, 0.0F );
    }
    
    /**
     * @return Returns the dig speed multiplier based on the given dig slowdown (mining fatigue) effect.
     * @see Player#getDigSpeed(BlockState, BlockPos)
     */
    private static float getDigSpeedSlowdown( @Nullable MobEffectInstance effect ) {
        if( effect == null ) return 1.0F;
        return switch (effect.getAmplifier()) {
            case 0 -> 0.3F;
            case 1 -> 0.09F;
            case 2 -> 0.0027F;
            default -> 8.1E-4F;
        };
    }
    
    /**
     * Checks whether a mob can be hidden in a block.
     * <p>
     * @param level The world we live in. Absolutely mad.
     * @param pos   Position to hide at.
     * @return True if a mob can be hidden here.
     */
    public static boolean canHideMob( Level level, BlockPos pos ) {
        BlockEntity blockEntity = level.getExistingBlockEntity( pos );
        if( blockEntity == null ) return false;

        CompoundTag tag = blockEntity.getPersistentData( );
        if( NBTHelper.containsNumber( tag, TAG_HIDE_DISABLED ) ) {
            if( tag.getBoolean( TAG_HIDE_DISABLED ) ) return false;
        }
        else if( blockEntity instanceof RandomizableContainerBlockEntity ) {
            tag.putBoolean( TAG_HIDE_DISABLED, !Config.IDLE.HIDING.lootableChance.rollChance( level.getRandom(), level, pos )
                    && NBTHelper.containsString( blockEntity.saveWithoutMetadata( ), "LootTable" ) );
        }
        return !NBTHelper.containsCompound( tag, TAG_HIDDEN_MOB );
    }
    
    /**
     * Hides a mob in a block. Prior to calling this, make sure the mob can be hidden here
     * via {@link #canHideMob(Level, BlockPos)}.
     * <p>
     * @param world The world we live in. Absolutely mad.
     * @param pos   Position to hide at.
     * @param mob   The entity to hide.
     */
    public static void hideMob( Level world, BlockPos pos, Mob mob ) {
        BlockEntity blockEntity = world.getExistingBlockEntity( pos );
        if( blockEntity == null ) return;
        
        if( mob.saveAsPassenger( NBTHelper.getOrCreateCompound( blockEntity.getPersistentData(), TAG_HIDDEN_MOB ) ) ) {
            // Successfully saved, remove the mob and play effects
            mob.spawnAnim();
            mob.discard();
        }
    }
    
    /**
     * Checks if there is a hiding mob. If so, unhides the mob and targets the entity that disturbed it.
     * <p>
     * @param level  The world we live in. Absolutely mad.
     * @param pos    Position to check for a hidden mob.
     * @param player The player triggering this check.
     * @param forceUnhide If true and a hidden mob is found, force the mob to spawn even
     *                    if there isn't really enough space for it.
     */
    public static void spawnHiddenMob( LevelReader level, BlockPos pos, @Nullable Player player, boolean forceUnhide ) {
        if( !(level instanceof ServerLevel serverLevel) ) return;
        BlockEntity tileEntity = level.getBlockEntity( pos );
        if( tileEntity == null ) return;

        // Get the tag if it exists
        CompoundTag data = tileEntity.getPersistentData();
        if( !NBTHelper.containsCompound( data, TAG_HIDDEN_MOB ) ) return;
        CompoundTag mobTag = data.getCompound( TAG_HIDDEN_MOB );
        
        // Validate and load from tag
        if( mobTag.isEmpty() || !NBTHelper.containsString( mobTag, "id" ) ) return;
        Optional<Entity> optional = EntityType.create( mobTag, serverLevel );
        if( optional.isEmpty() ) return;
        
        // Load successful!
        Entity mob = optional.get();;
        BlockPos spawnPos = pos.above();
        mob.setPos( spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D );

        // If we are not forcefully spawning the mob, check if it
        // has space to be placed above where it is hiding
        if ( !forceUnhide ) {
            // Check if the mob has space to unhide
            if (!level.noCollision(mob.getType().getAABB(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D))) {
                mob.discard();
                return;
            }
        }
        // Remove the mob data from tile entity nbt
        data.remove( TAG_HIDDEN_MOB );

        // Add the mob to the world and play effects
        serverLevel.addWithUUID( mob );

        if( mob instanceof Mob mobEntity ) {
            if( player != null && !player.isSpectator() && player.isAlive() && mobEntity.canAttack( player ) ) {
                mobEntity.setTarget( player );
            }
            mobEntity.spawnAnim();
        }
        RandomSource random = serverLevel.getRandom();
        serverLevel.sendParticles( ParticleTypes.CLOUD,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 10,
                random.nextGaussian(), random.nextGaussian(), random.nextGaussian(), 0.1 );
    }
}