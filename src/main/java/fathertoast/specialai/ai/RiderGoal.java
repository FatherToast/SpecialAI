package fathertoast.specialai.ai;

import fathertoast.specialai.config.Config;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.monster.SlimeEntity;
import net.minecraft.util.EntityPredicates;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Supplier;

/**
 * This AI causes the entity to search for and mount valid entities.
 * <p>
 * Rider size must match up to mount size to be a valid jockey pair.
 * Normal-sized riders and mounts count as small-sized if they are babies.
 */
public class RiderGoal extends Goal {
    /** The pathfinding speed multiplier to use when trying to mount. */
    private static final double SPEED_MULTIPLIER = 1.2;
    
    /** The owner of this AI. */
    protected final MobEntity mob;
    /** True if the entity is a small rider. */
    private final boolean isSmall;
    
    /** The mount the entity wants to ride. */
    private LivingEntity targetMount;
    /** Ticks until the entity will search for a mount. */
    private int checkTime;
    /** Ticks until the entity gives up trying to reach the mount. */
    private int giveUpDelay;
    
    /**
     * @param entity The owner of this AI.
     * @param small  Whether the entity is a small rider.
     */
    public RiderGoal( MobEntity entity, boolean small ) {
        mob = entity;
        isSmall = small;
        setFlags( EnumSet.of( Flag.MOVE, Flag.LOOK ) );
    }
    
    /** @return Returns true if this AI can be activated. */
    @Override
    public boolean canUse() {
        if( !mob.isPassenger() && ++checkTime > 50 ) {
            checkTime = 0;
            return findNearbyMount();
        }
        return false;
    }
    
    /** @return Called each update while active and returns true if this AI can remain active. */
    @Override
    public boolean canContinueToUse() {
        return !mob.isPassenger() && targetMount != null && !targetMount.isVehicle() && targetMount.isAlive() && ++giveUpDelay <= 400;
    }
    
    /** @return Returns true if this AI can be interrupted by a higher priority conflicting task. */
    @Override
    public boolean isInterruptable() { return false; }
    
    /** Called when this AI is activated. */
    @Override
    public void start() {
        mob.getNavigation().moveTo( targetMount.getX(), targetMount.getY(), targetMount.getZ(), SPEED_MULTIPLIER );
    }
    
    /** Called each tick while this AI is active. */
    @Override
    public void tick() {
        mob.getLookControl().setLookAt( targetMount, 30.0F, 30.0F );
        
        if( !targetMount.isVehicle() && mob.distanceToSqr( targetMount ) <= mob.getBbWidth() * mob.getBbWidth() * 4.0F + targetMount.getBbWidth() ) {
            AIManager.queue( new StartRiding( mob, targetMount ) );
            targetMount = null;
        }
        else if( mob.getNavigation().isDone() ) {
            // The target mount has moved away
            mob.getNavigation().moveTo( targetMount.getX(), targetMount.getY(), targetMount.getZ(), SPEED_MULTIPLIER );
        }
    }
    
    /** Called when this AI is deactivated. */
    @Override
    public void stop() {
        mob.getNavigation().stop();
        giveUpDelay = 0;
        targetMount = null;
    }
    
    /** @return Searches for a nearby mount and targets it. Returns true if one is found. */
    private boolean findNearbyMount() {
        List<Entity> list = mob.level.getEntities( mob, mob.getBoundingBox().inflate( 16.0, 8.0, 16.0 ),
                EntityPredicates.ENTITY_NOT_BEING_RIDDEN );
        Collections.shuffle( list );
        for( Entity entity : list ) {
            if( entity instanceof LivingEntity && isValidMount( (LivingEntity) entity ) ) {
                targetMount = (LivingEntity) entity;
                return true;
            }
        }
        return false;
    }
    
    /** @return Returns true if the given mount is a valid mount and is size-compatible with the rider. */
    private boolean isValidMount( LivingEntity mount ) {
        if( Config.GENERAL.JOCKEYS.mountBlacklist.get().contains( mount ) ) return false;
        
        if( isSmallMount( mount ) ) return isSmallRider();
        if( isNormalMount( mount ) ) return !isSmallRider();
        
        return false; // The mob was not a mount
    }
    
    /** @return Returns true if the given entity can be considered a small mount. */
    private boolean isSmallMount( LivingEntity mount ) {
        return Config.GENERAL.JOCKEYS.mountWhitelistSmall.get().contains( mount ) ||
                mount.isBaby() && Config.GENERAL.JOCKEYS.mountWhitelist.get().contains( mount );
    }
    
    /** @return Returns true if the given entity can be considered a normal mount. */
    private boolean isNormalMount( LivingEntity mount ) {
        return !mount.isBaby() && Config.GENERAL.JOCKEYS.mountWhitelist.get().contains( mount );
    }
    
    /** @return Returns true if the rider is a small rider, and false if the rider is normal-sized. */
    private boolean isSmallRider() {
        return isSmall || mob.isBaby() || mob instanceof SlimeEntity && ((SlimeEntity) mob).isTiny();
    }
    
    /**
     * Used to connect a rider to its target mount, so it can start riding.
     * This strategy is used because mounting during the AI tick can potentially cause issues.
     */
    private static class StartRiding implements Supplier<Boolean> {
        /** The entity that wants to ride. */
        private final MobEntity RIDER;
        /** The target entity to be ridden. */
        private final LivingEntity MOUNT;
        
        StartRiding( MobEntity rider, LivingEntity mount ) {
            RIDER = rider;
            MOUNT = mount;
        }
        
        /** Called to actually start riding. */
        @Override
        public Boolean get() {
            RIDER.startRiding( MOUNT, true );
            return true;
        }
    }
}