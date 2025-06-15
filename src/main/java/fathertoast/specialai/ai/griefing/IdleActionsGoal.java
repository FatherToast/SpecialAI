package fathertoast.specialai.ai.griefing;

import fathertoast.crust.api.lib.LevelEventHelper;
import fathertoast.crust.api.lib.NBTHelper;
import fathertoast.specialai.SpecialAI;
import fathertoast.specialai.ai.AIManager;
import fathertoast.specialai.config.Config;
import fathertoast.specialai.util.BlockHelper;
import fathertoast.specialai.util.SpecialAIFakePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fluids.IFluidBlock;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.EnumSet;

/**
 * This AI causes the entity to seek out blocks to either destroy or interact with (usually right click),
 * depending on which actions are enabled.
 */
public class IdleActionsGoal extends Goal {
    /** Differentiates between the different actions that can be taken by this AI. */
    private enum Activity { NONE, HIDING, GRIEFING, FIDDLING }
    
    /** The owner of this AI. */
    protected final Mob mob;
    /** Whether this should perform idle container hiding. */
    private final boolean hidingEnabled;
    /** Whether this should perform idle griefing. */
    private final boolean griefingEnabled;
    /** Whether this should perform idle fiddling. */
    private final boolean fiddlingEnabled;
    
    /** The current action being performed. */
    private Activity currentActivity = Activity.NONE;
    
    /** The coordinates of the block this entity is attacking. */
    private BlockPos targetPos = BlockPos.ZERO;
    /** The block to attack. */
    private BlockState targetBlock;
    /** The ray trace resulting from sight check, if successful. */
    private BlockHitResult targetHitResult;
    /** True if the target is in the entity's range and can be seen. */
    private boolean canReach;
    /** Ticks until the entity can check line of sight again. */
    private int sightCounter;
    /** Ticks until the entity gives up. */
    private int giveUpDelay;
    
    // Griefing only
    
    /** Ticks to count how often to play the "hit" sound. */
    private int hitCounter;
    /** Current block damage. */
    private float blockDamage;
    /** Previous block damage int sent to clients. */
    private int lastBlockDamage = -1;
    
    // Fiddling only
    
    /** Used to prevent mobs from spamming right click on things. */
    private int fiddleDelay;
    /** The entity wrapped as a player. Required to perform the 'right-click block' action. */
    private SpecialAIFakePlayer fiddleWrapper;
    
    /**
     * @param entity   The owner of this AI.
     * @param hiding   True if the entity should idly hide inside containers.
     * @param griefing True if the entity should idly destroy blocks.
     * @param fiddling True if the entity should idly interact with blocks.
     */
    public IdleActionsGoal( Mob entity, boolean hiding, boolean griefing, boolean fiddling ) {
        mob = entity;
        hidingEnabled = hiding;
        griefingEnabled = griefing;
        fiddlingEnabled = fiddling;
        setFlags( EnumSet.of( Flag.MOVE, Flag.LOOK ) );
    }
    
    /** Attempt to pathfind to the currently set target. */
    private void pathToTarget() {
        mob.getNavigation().moveTo(
                targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5, 1.0 );
    }
    
    /** @return Returns true if this AI can be activated. */
    @Override
    public boolean canUse() {
        if( mob.isPassenger() ) return false;
        
        fiddleDelay--;
        sightCounter--;
        if( sightCounter <= 0 ) {
            sightCounter = Config.IDLE.GENERAL.scanDelay.get();
            
            // Try picking random blocks; the strategy used favors blocks closer to the mob
            final int rangeXZ = Config.IDLE.GENERAL.rangeHorizontal.get();
            final int rangeY = Config.IDLE.GENERAL.rangeVertical.get();
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for( int i = 0; i < Config.IDLE.GENERAL.scanCount.get() && AIManager.canScan(); i++ ) {
                pos.set(
                        mob.blockPosition().getX() + mob.getRandom().nextInt( rangeXZ ) - mob.getRandom().nextInt( rangeXZ ),
                        mob.blockPosition().getY() + mob.getRandom().nextInt( rangeY ) - mob.getRandom().nextInt( rangeY ),
                        mob.blockPosition().getZ() + mob.getRandom().nextInt( rangeXZ ) - mob.getRandom().nextInt( rangeXZ )
                );
                if( tryTargetBlock( pos ) ) return true;
            }
        }
        // No valid block was found
        return false;
    }
    
    /** @return Called each update while active and returns true if this AI can remain active. */
    @Override
    public boolean canContinueToUse() {
        // If the target changes, everything needs to be validated again - just cancel instead and it can re-pick later if still valid
        if( mob.isPassenger() || targetBlock == null || mob.level().getBlockState( targetPos ).getBlock() != targetBlock.getBlock() ) {
            return false;
        }

        return switch (currentActivity) {
            case FIDDLING -> canContinueFiddling();
            case GRIEFING -> canContinueGriefing();
            case HIDING -> canContinueHiding();
            default -> false;
        };
    }
    
    private boolean canContinueHiding() { return giveUpDelay < 400 && BlockHelper.canHideMob( mob.level(), targetPos ); }
    
    private boolean canContinueGriefing() { return blockDamage > 0.0F || giveUpDelay < 400; }
    
    private boolean canContinueFiddling() { return giveUpDelay < 400; }
    
    /** @return Returns true if this AI can be interrupted by a higher priority conflicting task. */
    @Override
    public boolean isInterruptable() { return currentActivity != Activity.GRIEFING || blockDamage <= 0.0F; }
    
    /** Called when this AI is activated. */
    @Override
    public void start() {
        targetHitResult = null;
        canReach = false;
        sightCounter = 0;
        giveUpDelay = 0;
        
        hitCounter = 0;
        blockDamage = 0.0F;
        lastBlockDamage = -1;
        
        pathToTarget();
    }
    
    /** Called when this AI is deactivated. */
    @Override
    public void stop() {
        targetBlock = null;
        targetHitResult = null;
        canReach = false;
        sightCounter = 20;
        giveUpDelay = 0;
        
        mob.getNavigation().stop();
        
        switch( currentActivity ) {
            case FIDDLING:
                stopFiddling();
                break;
            case GRIEFING:
                stopGriefing();
                break;
            case HIDING:
                stopHiding();
                break;
            default:
        }
        currentActivity = Activity.NONE;
    }
    
    /** Called when this AI is deactivated while in hiding mode. */
    private void stopHiding() { }
    
    /** Called when this AI is deactivated while in griefing mode. */
    private void stopGriefing() {
        hitCounter = 0;
        blockDamage = 0.0F;
        lastBlockDamage = -1;
        
        if( !madCreeper() ) {
            mob.level().destroyBlockProgress( mob.getId(), targetPos, -1 );
        }
    }
    
    /** Called when this AI is deactivated while in fiddling mode. */
    private void stopFiddling() { fiddleDelay = 80 + mob.getRandom().nextInt( 81 ); }
    
    /** Called each tick while this AI is active. */
    @Override
    public void tick() {
        giveUpDelay++;

        if ( targetPos != null ) {
            mob.getLookControl().setLookAt(targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5,
                    30.0F, 30.0F);
        }
        
        if( canReach && targetHitResult != null && targetBlock != null ) {
            // The target can be reached, perform the activity
            switch( currentActivity ) {
                case FIDDLING:
                    performFiddling();
                    break;
                case GRIEFING:
                    performGriefing();
                    break;
                case HIDING:
                    performHiding();
                    break;
                default:
            }
        }
        else {
            // Check if the target is in range and can be seen
            if( sightCounter-- <= 0 ) {
                sightCounter = 8 + mob.getRandom().nextInt( 5 );
                if( checkSight() ) {
                    sightCounter += 5;
                }
            }
            
            if( giveUpDelay > 400 ) {
                targetBlock = null;
            }
            else if( mob.getNavigation().isDone() ) {
                pathToTarget();
            }
        }
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    /** Called each tick while this AI is active, in hiding mode, and the mob can reach its target. */
    private void performHiding() {
        // Try to hide in the block
        if( BlockHelper.canHideMob( mob.level(), targetPos ) ) {
            BlockHelper.hideMob( mob.level(), targetPos, mob );
        }
        targetBlock = null;
    }
    
    /** Called each tick while this AI is active, in griefing mode, and the mob can reach its target. */
    private void performGriefing() {
        // Stop moving while attacking the target
        if( mob.getNavigation().isInProgress() ) {
            mob.getNavigation().stop();
        }

        final double blockReach = mob.getAttribute( ForgeMod.BLOCK_REACH.get() ) == null
                ? 4.0D // Half a block shorter range than players by default
                : mob.getAttributeValue( ForgeMod.BLOCK_REACH.get() );

        // Too far away from the target, abort
        if ( mob.distanceToSqr( targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5 )
                > ( blockReach * blockReach ) ) {
            targetBlock = null;
            return;
        }
        
        if( madCreeper() ) {
            // Goal complete
            ((Creeper) mob).ignite();
            targetBlock = null;
            return;
        }
        Level level = mob.level();
        
        // Play hit effects
        if( hitCounter == 0 ) {
            SoundType sound = targetBlock.getBlock().getSoundType( targetBlock, level, targetPos, mob );
            if( !mob.isSilent() ) {
                level.playSound( null, targetPos, sound.getBreakSound(), mob.getSoundSource(),
                        sound.getVolume(), sound.getPitch() * 0.8F );
            }
            if( !mob.swinging ) {
                mob.swing( mob.getUsedItemHand() );
            }
        }
        if( ++hitCounter >= 5 ) {
            hitCounter = 0;
        }
        
        // Perform block breaking
        blockDamage += (float) (BlockHelper.getDestroyProgress( targetBlock, mob, level, targetPos ) * Config.IDLE.GRIEFING.breakSpeed.get());
        if( blockDamage >= 1.0F ) {
            // Block is broken
            // Handle special cases
            if( targetBlock.getBlock() == Blocks.FARMLAND ) {
                level.setBlock( targetPos, Blocks.DIRT.defaultBlockState(), 3 );

                // Help mobs not fall through the farmland they break
                if ( mob.blockPosition().equals( targetPos ) ) {
                    mob.setPos( mob.getX(), targetPos.getY() + 1, mob.getZ() );
                }
            }
            // Otherwise, destroy the block
            else {
                BlockHelper.spawnHiddenMob( level, targetPos, null, true );
                level.destroyBlock( targetPos, Config.IDLE.GRIEFING.leaveDrops.get() );
                if( Config.IDLE.GRIEFING.breakSound.get() ) {
                    LevelEventHelper.ZOMBIE_BREAK_WOODEN_DOOR.play( mob.level(), targetPos );
                }
                LevelEventHelper.BLOCK_BREAK_FX.play( mob.level(), null, targetPos, targetBlock );
            }
            
            // Play animation; goal complete
            if( !mob.swinging ) {
                mob.swing( mob.getUsedItemHand() );
            }
            blockDamage = 0.0F;
            targetBlock = null;
        }
        
        // Update block damage
        final int damage = (int) Math.ceil( blockDamage * 10.0F ) - 1;
        if( damage != lastBlockDamage ) {
            mob.level().destroyBlockProgress( mob.getId(), targetPos, damage );
            lastBlockDamage = damage;
        }
    }
    
    /** Called each tick while this AI is active, in fiddling mode, and the mob can reach its target. */
    private void performFiddling() {
        if( mob.level() instanceof ServerLevel ) { // This should always be true, but check just in case
            // Handle special cases
            if( targetBlock.getBlock() instanceof TntBlock ) {
                targetBlock.getBlock().onCaughtFire( targetBlock, mob.level(), targetPos, null, mob );
                mob.level().removeBlock( targetPos, false );
            }
            // Otherwise, interact like a player right-clicking the block
            else {
                if( fiddleWrapper == null ) {
                    // Surrounded with try/catch in case the fake player creation causes issues
                    try {
                        fiddleWrapper = new SpecialAIFakePlayer( mob );
                    }
                    catch( Exception ex ) {
                        SpecialAI.LOG.error( "Failed to create fake player wrapper for entity '{}'",
                                ForgeRegistries.ENTITY_TYPES.getKey( mob.getType() ), ex );
                        // Forcibly disable fiddling for this entity until reload; also disables special cases, but oh well
                        stopFiddling();
                        fiddleDelay = Integer.MAX_VALUE;
                        currentActivity = Activity.NONE;
                    }
                }
                if( fiddleWrapper != null ) {
                    // Surrounded with try/catch in case the fake player interaction causes issues
                    try {
                        fiddleWrapper.updateFakePlayerState();
                        targetBlock.use( mob.level(), fiddleWrapper, InteractionHand.MAIN_HAND, targetHitResult );
                        fiddleWrapper.updateWrappedEntityState();
                    }
                    catch( Exception ex ) {
                        SpecialAI.LOG.warn( "Failed to fiddle with block '{}'", ForgeRegistries.BLOCKS.getKey( targetBlock.getBlock() ), ex );
                    }
                }
            }
        }
        
        // Play animation; goal complete
        if( !mob.swinging ) {
            mob.swing( mob.getUsedItemHand() );
        }
        targetBlock = null;
    }
    
    /**
     * Checks line of sight to the target block using a ray trace.
     * Will not attempt the ray trace if the target is not in range.
     *
     * @return Returns true if a ray trace was made.
     */
    private boolean checkSight() {
        double x = targetPos.getX() + 0.5;
        double y = targetPos.getY() + 0.5;
        double z = targetPos.getZ() + 0.5;
        if( mob.distanceToSqr( x, y - mob.getEyeHeight(), z ) <= Config.IDLE.GENERAL.reach.get() * Config.IDLE.GENERAL.reach.get() ) {
            Vec3 posVec = new Vec3( mob.getX(), mob.getY() + mob.getEyeHeight(), mob.getZ() );
            
            // Ray trace to the center of the nearest x-, y-, and z-axis faces
            if( checkSight( posVec, x, y + (mob.getY() > y ? 0.5 : -0.5), z ) ||
                    checkSight( posVec, x + (mob.getX() > x ? 0.5 : -0.5), y, z ) ||
                    checkSight( posVec, x, y, z + (mob.getZ() > z ? 0.5 : -0.5) ) ) {
                canReach = true;
            }
            return true;
        }
        return false;
    }
    
    /**
     * Ray traces to check sight. Will change the target to any obstructing block, if it is a valid target.
     *
     * @return Returns true if there is an unobstructed view of the (possibly new) target.
     */
    private boolean checkSight( final Vec3 posVec, double x, double y, double z ) {
        final Vec3 targetVec = new Vec3( x, y, z );
        BlockHitResult hit = mob.level().clip( new ClipContext( posVec, targetVec,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mob ) );
        
        if( HitResult.Type.MISS.equals( hit.getType() ) ) {
            // A miss is okay; this means the target was unobstructed, but its hitbox is smaller than a full cube - convert to a hit
            hit = new BlockHitResult( hit.getLocation(), hit.getDirection(), hit.getBlockPos(), hit.isInside() );
        }
        // If the target is hit, move forward - otherwise see if what we did hit would make a valid target
        if( targetPos.equals( hit.getBlockPos() ) ||
                tryTargetObstructingBlock( hit ) ) {
            targetHitResult = hit;
            return true;
        }
        return false;
    }
    
    /** @return Called when a sight check hits blocks other than the target. Changes the target to the hit block if possible. */
    private boolean tryTargetObstructingBlock( BlockHitResult hit ) {
        BlockState block = mob.level().getBlockState( hit.getBlockPos() );
        return switch (currentActivity) {
            case FIDDLING -> tryTargetBlockFiddling(block, hit.getBlockPos());
            case GRIEFING -> tryTargetBlockGriefing(block, hit.getBlockPos());
            case HIDING -> tryTargetBlockHiding(block, hit.getBlockPos());
            default -> false;
        };
    }
    
    /** @return Tries to target the block at a position for any of the enabled actions. Returns true if successful. */
    private boolean tryTargetBlock( BlockPos pos ) {
        BlockState block = mob.level().getBlockState( pos );
        if( block.isAir( ) ) return false;
        
        return hidingEnabled && tryTargetBlockHiding( block, pos ) ||
                griefingEnabled && tryTargetBlockGriefing( block, pos ) ||
                fiddlingEnabled && fiddleDelay <= 0 && tryTargetBlockFiddling( block, pos );
    }
    
    /** @return Tries to target a block for hiding. Returns true if successful. */
    private boolean tryTargetBlockHiding( BlockState block, BlockPos pos ) {
        if( isValidTargetForHiding( block, pos ) && BlockHelper.canHideMob( mob.level(), pos ) ) {
            currentActivity = Activity.HIDING;
            targetPos = pos.immutable();
            targetBlock = block;
            return true;
        }
        return false;
    }
    
    /** @return Tries to target a block for griefing. Returns true if successful. */
    private boolean tryTargetBlockGriefing( BlockState block, BlockPos pos ) {
        if( isValidTargetForGriefing( block, pos ) &&
                BlockHelper.shouldDamage( block, mob, Config.IDLE.GRIEFING.requiresTools.get() && !madCreeper(), mob.level(), pos ) ) {
            currentActivity = Activity.GRIEFING;
            targetPos = pos.immutable();
            targetBlock = block;
            return true;
        }
        return false;
    }
    
    /** @return Tries to target a block for fiddling. Returns true if successful. */
    private boolean tryTargetBlockFiddling( BlockState block, BlockPos pos ) {
        if( isValidTargetForFiddling( block ) && ForgeEventFactory.getMobGriefingEvent( mob.level(), mob ) ) {
            currentActivity = Activity.FIDDLING;
            targetPos = pos.immutable();
            targetBlock = block;
            return true;
        }
        return false;
    }
    
    /** @return Returns true if the specified block can be targeted for hiding. */
    private boolean isValidTargetForHiding( BlockState state, BlockPos pos ) {
        if( Config.IDLE.HIDING.targetList.BLACKLIST.get().matches( state ) ) {
            return false;
        }
        return Config.IDLE.HIDING.targetList.WHITELIST.get().matches( state );
    }
    
    /** @return Returns true if the specified block can be targeted for griefing. */
    private boolean isValidTargetForGriefing( BlockState state, BlockPos pos ) {
        if( state.liquid() || Config.IDLE.GRIEFING.targetBlacklist.get().matches( state ) ) {
            return false;
        }
        if( Config.IDLE.GRIEFING.targetLights.get() && state.getLightEmission( mob.level(), pos ) > 1 && !isNaturalLightBlock( state.getBlock() ) ) {
            return true;
        }
        if( Config.IDLE.GRIEFING.targetBeds.get() && state.getBlock() instanceof BedBlock ) {
            return true;
        }
        if( Config.IDLE.GRIEFING.targetWhitelistLootable.get().matches( state ) ) {
            return isLootContainerTargetable( pos );
        }
        return Config.IDLE.GRIEFING.targetWhitelist.get().matches( state );
    }
    
    /** @return Returns true if the specified block can be targeted for fiddling. */
    private boolean isValidTargetForFiddling( BlockState state ) {
        if( Config.IDLE.FIDDLING.targetList.BLACKLIST.get().matches( state ) ) {
            return false;
        }
        final Block block = state.getBlock();
        if( Config.IDLE.FIDDLING.targetDoors.get() &&
                state.is( BlockTags.WOODEN_DOORS ) || state.is( BlockTags.WOODEN_TRAPDOORS ) || state.is( Tags.Blocks.FENCE_GATES_WOODEN ) ) {
            return true;
        }
        if( Config.IDLE.FIDDLING.targetSwitches.get() && (block instanceof LeverBlock || block instanceof ButtonBlock ) ) {
            return true;
        }
        return Config.IDLE.FIDDLING.targetList.WHITELIST.get().matches( state );
    }

    // TODO - Consider making dimension based configs for this. What can be considered natural
    //        and not highly depends on what other mods might add to worldgen
    /** @return Returns true if the block is a natural light source. */
    private boolean isNaturalLightBlock( Block block ) {
        return block instanceof BaseFireBlock || block instanceof RedStoneOreBlock ||
                block == Blocks.SEA_PICKLE || block == Blocks.MAGMA_BLOCK || block == Blocks.SHROOMLIGHT ||
                block == Blocks.GLOW_LICHEN || block == Blocks.CAVE_VINES || block == Blocks.CAVE_VINES_PLANT ||
                block instanceof AmethystClusterBlock ||
                // Unnatural when outside the Nether
                Level.NETHER.equals( mob.level().dimension() ) && block == Blocks.GLOWSTONE;
    }
    
    /**
     * @return Returns true if the specified block is not a container with a loot table tag.
     * @see net.minecraft.tileentity.LockableLootTileEntity#tryLoadLootTable(CompoundNBT)
     */
    @SuppressWarnings("JavadocReference")
    private boolean isLootContainerTargetable(BlockPos pos ) {
        BlockEntity container = mob.level().getBlockEntity( pos );
        if( container == null ) return true;
        
        return !NBTHelper.containsString( container.saveWithoutMetadata( ), "LootTable" );
    }
    
    /** @return Returns true if the entity is a creeper and should explode instead of attacking the block. */
    private boolean madCreeper() { return Config.IDLE.GRIEFING.madCreepers.get() && mob instanceof Creeper; }
}