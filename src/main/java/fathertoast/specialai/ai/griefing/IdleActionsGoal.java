package fathertoast.specialai.ai.griefing;

import fathertoast.crust.api.lib.LevelEventHelper;
import fathertoast.crust.api.lib.NBTHelper;
import fathertoast.specialai.SpecialAI;
import fathertoast.specialai.ai.AIManager;
import fathertoast.specialai.config.Config;
import fathertoast.specialai.util.BlockHelper;
import fathertoast.specialai.util.SpecialAIFakePlayer;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.monster.CreeperEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.ForgeEventFactory;
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
    protected final MobEntity mob;
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
    private BlockRayTraceResult targetHitResult;
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
    public IdleActionsGoal( MobEntity entity, boolean hiding, boolean griefing, boolean fiddling ) {
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
            BlockPos.Mutable pos = new BlockPos.Mutable();
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
        if( mob.isPassenger() || targetBlock == null || mob.level.getBlockState( targetPos ).getBlock() != targetBlock.getBlock() ) {
            return false;
        }
        
        switch( currentActivity ) {
            case FIDDLING:
                return canContinueFiddling();
            case GRIEFING:
                return canContinueGriefing();
            case HIDING:
                return canContinueHiding();
            default:
                return false;
        }
    }
    
    private boolean canContinueHiding() { return giveUpDelay < 400 && BlockHelper.canHideMob( mob.level, targetPos ); }
    
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
            mob.level.destroyBlockProgress( mob.getId(), targetPos, -1 );
        }
    }
    
    /** Called when this AI is deactivated while in fiddling mode. */
    private void stopFiddling() { fiddleDelay = 80 + mob.getRandom().nextInt( 81 ); }
    
    /** Called each tick while this AI is active. */
    @Override
    public void tick() {
        giveUpDelay++;
        mob.getLookControl().setLookAt( targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5,
                30.0F, 30.0F );
        
        if( canReach && targetHitResult != null ) {
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
    
    /** Called each tick while this AI is active, in hiding mode, and the mob can reach its target. */
    private void performHiding() {
        // Try to hide in the block
        if( BlockHelper.canHideMob( mob.level, targetPos ) ) {
            BlockHelper.hideMob( mob.level, targetPos, mob );
        }
        targetBlock = null;
    }
    
    /** Called each tick while this AI is active, in griefing mode, and the mob can reach its target. */
    private void performGriefing() {
        // Stop moving while attacking the target
        if( mob.getNavigation().isInProgress() ) {
            mob.getNavigation().stop();
        }
        
        if( madCreeper() ) {
            // Goal complete
            ((CreeperEntity) mob).ignite();
            targetBlock = null;
            return;
        }
        World world = mob.level;
        
        // Play hit effects
        if( hitCounter == 0 ) {
            SoundType sound = targetBlock.getBlock().getSoundType( targetBlock, world, targetPos, mob );
            if( !mob.isSilent() ) {
                world.playSound( null, targetPos, sound.getBreakSound(), mob.getSoundSource(),
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
        blockDamage += BlockHelper.getDestroyProgress( targetBlock, mob, world, targetPos ) * Config.IDLE.GRIEFING.breakSpeed.get();
        if( blockDamage >= 1.0F ) {
            // Block is broken
            // Handle special cases
            if( targetBlock.getBlock() == Blocks.FARMLAND ) {
                world.setBlock( targetPos, Blocks.DIRT.defaultBlockState(), 3 );
            }
            // Otherwise, destroy the block
            else {
                BlockHelper.spawnHiddenMob( world, targetPos, null );
                world.destroyBlock( targetPos, Config.IDLE.GRIEFING.leaveDrops.get() );
                if( Config.IDLE.GRIEFING.breakSound.get() ) {
                    LevelEventHelper.ZOMBIE_BREAK_WOODEN_DOOR.play( mob.level, targetPos );
                }
                LevelEventHelper.BLOCK_BREAK_FX.play( mob.level, null, targetPos, targetBlock );
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
            mob.level.destroyBlockProgress( mob.getId(), targetPos, damage );
            lastBlockDamage = damage;
        }
    }
    
    /** Called each tick while this AI is active, in fiddling mode, and the mob can reach its target. */
    private void performFiddling() {
        if( mob.level instanceof ServerWorld ) { // This should always be true, but check just in case
            // Handle special cases
            if( targetBlock.getBlock() instanceof TNTBlock ) {
                targetBlock.getBlock().catchFire( targetBlock, mob.level, targetPos, null, mob );
                mob.level.removeBlock( targetPos, false );
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
                                mob.getType().getRegistryName(), ex );
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
                        targetBlock.use( mob.level, fiddleWrapper, Hand.MAIN_HAND, targetHitResult );
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
            Vector3d posVec = new Vector3d( mob.getX(), mob.getY() + mob.getEyeHeight(), mob.getZ() );
            
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
    private boolean checkSight( final Vector3d posVec, double x, double y, double z ) {
        final Vector3d targetVec = new Vector3d( x, y, z );
        BlockRayTraceResult hit = mob.level.clip( new RayTraceContext( posVec, targetVec,
                RayTraceContext.BlockMode.OUTLINE, RayTraceContext.FluidMode.NONE, mob ) );
        
        if( RayTraceResult.Type.MISS.equals( hit.getType() ) ) {
            // A miss is okay; this means the target was unobstructed, but its hitbox is smaller than a full cube - convert to a hit
            hit = new BlockRayTraceResult( hit.getLocation(), hit.getDirection(), hit.getBlockPos(), hit.isInside() );
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
    private boolean tryTargetObstructingBlock( BlockRayTraceResult hit ) {
        BlockState block = mob.level.getBlockState( hit.getBlockPos() );
        switch( currentActivity ) {
            case FIDDLING:
                return tryTargetBlockFiddling( block, hit.getBlockPos() );
            case GRIEFING:
                return tryTargetBlockGriefing( block, hit.getBlockPos() );
            case HIDING:
                return tryTargetBlockHiding( block, hit.getBlockPos() );
            default:
                return false;
        }
    }
    
    /** @return Tries to target the block at a position for any of the enabled actions. Returns true if successful. */
    private boolean tryTargetBlock( BlockPos pos ) {
        BlockState block = mob.level.getBlockState( pos );
        //noinspection deprecation
        if( block.isAir( mob.level, pos ) /* Note: this will be replaced by #isAir() in 1.17+ */ ) return false;
        
        return hidingEnabled && tryTargetBlockHiding( block, pos ) ||
                griefingEnabled && tryTargetBlockGriefing( block, pos ) ||
                fiddlingEnabled && fiddleDelay <= 0 && tryTargetBlockFiddling( block, pos );
    }
    
    /** @return Tries to target a block for hiding. Returns true if successful. */
    private boolean tryTargetBlockHiding( BlockState block, BlockPos pos ) {
        if( isValidTargetForHiding( block, pos ) && BlockHelper.canHideMob( mob.level, pos ) ) {
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
                BlockHelper.shouldDamage( block, mob, Config.IDLE.GRIEFING.requiresTools.get() && !madCreeper(), mob.level, pos ) ) {
            currentActivity = Activity.GRIEFING;
            targetPos = pos.immutable();
            targetBlock = block;
            return true;
        }
        return false;
    }
    
    /** @return Tries to target a block for fiddling. Returns true if successful. */
    private boolean tryTargetBlockFiddling( BlockState block, BlockPos pos ) {
        if( isValidTargetForFiddling( block ) && ForgeEventFactory.getMobGriefingEvent( mob.level, mob ) ) {
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
        if( state.getMaterial().isLiquid() || Config.IDLE.GRIEFING.targetBlacklist.get().matches( state ) ) {
            return false;
        }
        if( Config.IDLE.GRIEFING.targetLights.get() && state.getLightValue( mob.level, pos ) > 1 && !isNaturalLightBlock( state.getBlock() ) ) {
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
        if( Config.IDLE.FIDDLING.targetDoors.get() && state.getMaterial() != Material.METAL && state.getMaterial() != Material.HEAVY_METAL &&
                (block instanceof DoorBlock || block instanceof TrapDoorBlock || block instanceof FenceGateBlock) ) {
            return true;
        }
        if( Config.IDLE.FIDDLING.targetSwitches.get() && (block instanceof LeverBlock || block instanceof AbstractButtonBlock) ) {
            return true;
        }
        return Config.IDLE.FIDDLING.targetList.WHITELIST.get().matches( state );
    }
    
    /** @return Returns true if the block is a natural light source. */
    private boolean isNaturalLightBlock( Block block ) {
        // Note: 1.17+ has Glow Berries/Lichen & Amethyst Bud/Cluster
        return block instanceof AbstractFireBlock || block instanceof OreBlock || block instanceof RedstoneOreBlock ||
                block == Blocks.SEA_PICKLE || block == Blocks.MAGMA_BLOCK || block == Blocks.SHROOMLIGHT ||
                // Unnatural when outside the Nether
                World.NETHER.equals( mob.level.dimension() ) && block == Blocks.GLOWSTONE;
    }
    
    /**
     * @return Returns true if the specified block is not a container with a loot table tag.
     * @see net.minecraft.tileentity.LockableLootTileEntity#tryLoadLootTable(CompoundNBT)
     */
    private boolean isLootContainerTargetable( BlockPos pos ) {
        TileEntity container = mob.level.getBlockEntity( pos );
        if( container == null ) return true;
        
        return !NBTHelper.containsString( container.save( new CompoundNBT() ), "LootTable" );
    }
    
    /** @return Returns true if the entity is a creeper and should explode instead of attacking the block. */
    private boolean madCreeper() { return Config.IDLE.GRIEFING.madCreepers.get() && mob instanceof CreeperEntity; }
}