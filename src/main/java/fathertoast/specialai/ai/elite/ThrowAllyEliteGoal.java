package fathertoast.specialai.ai.elite;

import fathertoast.specialai.config.Config;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Hand;
import net.minecraft.util.math.vector.Vector3d;

import java.util.EnumSet;
import java.util.List;

/**
 * This AI causes an entity to throw nearby allies at its target when at range.
 */
public class ThrowAllyEliteGoal extends AbstractPathingEliteGoal {
    /** The entity this mob is following. */
    private MobEntity throwTarget;
    /** Ticks until next attack. */
    private int attackTime;
    /** Ticks until the entity gives up. */
    private int giveUpDelay;
    
    ThrowAllyEliteGoal( MobEntity entity, CompoundNBT aiTag ) {
        super( entity, aiTag );
        setFlags( EnumSet.of( Flag.MOVE, Flag.LOOK ) );
    }
    
    /** @return Returns true if this AI can be activated. */
    @Override
    public boolean canUse() {
        final LivingEntity target = mob.getTarget();
        if( attackTime-- > 0 || target == null || mob.isPassenger() ) return false;
        if( mob.isVehicle() ) return true;
        return mob.getRandom().nextInt( 20 ) == 0 && findThrowTarget();
    }
    
    /** Called when this AI is activated. */
    @Override
    public void start() {
        giveUpDelay = 0;
        if( mob.isVehicle() ) {
            // Move to throw ally at target
            final LivingEntity target = mob.getTarget();
            if( target != null ) {
                startPathing( target, Config.ELITE_AI.THROW_ALLY.speedToTarget.get() );
            }
        }
        else if( throwTarget != null ) {
            // Move to pick up ally
            startPathing( throwTarget, Config.ELITE_AI.THROW_ALLY.speedToAlly.get() );
        }
    }
    
    /** @return Called each update while active and returns true if this AI can remain active. */
    @Override
    public boolean canContinueToUse() {
        return giveUpDelay++ < 400 && mob.getTarget() != null && !mob.isPassenger() && // Can still use the AI
                (throwTarget != null && throwTarget.isAlive() || mob.isVehicle()); // Still has an ally to throw
    }
    
    /** @return Returns true if this AI can be interrupted by a higher priority conflicting task. */
    @Override
    public boolean isInterruptable() { return false; }
    
    /** Called each tick while this AI is active. */
    @Override
    public void tick() {
        final LivingEntity target = mob.getTarget();
        if( target == null ) return;
        
        if( mob.isVehicle() ) {
            // Try to throw
            mob.getLookControl().setLookAt( target, 30.0F, 30.0F );
            
            if( attackTime-- <= 0 && mob.getRandom().nextInt( 10 ) == 0 &&
                    mob.distanceToSqr( target ) <= Config.ELITE_AI.THROW_ALLY.throwRangeSqrMax.get() ) {
                // Throw the held entity
                final Entity throwEntity = mob.getPassengers().get( 0 );
                throwEntity.stopRiding();
                throwEntity.setOnGround( false );
                throwEntity.fallDistance = 0.0F;
                if( throwEntity instanceof MobEntity ) {
                    ((MobEntity) throwEntity).getLookControl().setLookAt( target, 180.0F, 0.0F );
                }
                final Vector3d jumpXZ = new Vector3d( target.getX() - mob.getX(), 0.0, target.getZ() - mob.getZ() )
                        .normalize().scale( Config.ELITE_AI.THROW_ALLY.throwSpeedForward.get() ).add( mob.getDeltaMovement().scale( 0.2 ) );
                throwEntity.setDeltaMovement( jumpXZ.x, Config.ELITE_AI.THROW_ALLY.throwSpeedUpward.get(), jumpXZ.z );
                
                mob.getNavigation().stop();
                mob.swing( Hand.MAIN_HAND );
                giveUpDelay = 666;
                attackTime = Config.ELITE_AI.THROW_ALLY.cooldown.next( mob.getRandom() );
            }
            else {
                tickPathing( target, Config.ELITE_AI.THROW_ALLY.speedToTarget.get() );
            }
        }
        else if( throwTarget != null ) {
            // Try to pick up ally
            mob.getLookControl().setLookAt( throwTarget, 30.0F, 30.0F );
            
            if( mob.distanceToSqr( throwTarget ) <= mob.getBbWidth() * mob.getBbWidth() * 4.0F + throwTarget.getBbWidth() ) {
                // Pick up the ally
                throwTarget.startRiding( mob, true );
                throwTarget = null;
                
                startPathing( target, Config.ELITE_AI.THROW_ALLY.speedToTarget.get() );
                mob.swing( Hand.MAIN_HAND );
                attackTime = 20 + mob.getRandom().nextInt( 10 );
            }
            else {
                tickPathing( throwTarget, Config.ELITE_AI.THROW_ALLY.speedToAlly.get() );
            }
        }
    }
    
    /** Called when this AI is deactivated. */
    @Override
    public void stop() {
        if( mob.isVehicle() ) {
            mob.getPassengers().get( 0 ).stopRiding();
        }
        mob.getNavigation().stop();
        throwTarget = null;
    }
    
    /** Searches for a nearby ally that can be thrown and targets it. Returns true if one is found. */
    private boolean findThrowTarget() {
        final LivingEntity target = mob.getTarget();
        if( target == null ) return false;
        
        // Get distance to target; note that in addition to checking for minimum distance, this also acts as the maximum range for throw targets
        double closestDistanceSqr = mob.distanceToSqr( target );
        if( closestDistanceSqr < Config.ELITE_AI.THROW_ALLY.throwRangeSqrMin.get() ) return false;
        
        final List<Entity> nearbyEntities = mob.level.getEntities( mob, mob.getBoundingBox().inflate( Math.sqrt( closestDistanceSqr ) + 2.0 ) );
        for( Entity entity : nearbyEntities ) {
            // Check if the entity is a valid throw target
            if( !(entity instanceof MobEntity) || !entity.isAlive() || !entity.isOnGround() || entity.isPassenger() ||
                    target != ((MobEntity) entity).getTarget() || entity.distanceToSqr( target ) < Config.ELITE_AI.THROW_ALLY.allyRangeSqrMin.get() )
                continue;
            
            // Pick the closest target only
            final double distanceSqr = mob.distanceToSqr( entity );
            if( distanceSqr < closestDistanceSqr ) {
                closestDistanceSqr = distanceSqr;
                throwTarget = (MobEntity) entity;
            }
        }
        return throwTarget != null;
    }
}