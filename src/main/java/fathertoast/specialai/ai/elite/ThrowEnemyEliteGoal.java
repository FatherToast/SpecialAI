package fathertoast.specialai.ai.elite;

import fathertoast.specialai.config.Config;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.play.server.SEntityVelocityPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.vector.Vector3d;

import java.util.EnumSet;
import java.util.List;

/**
 * This AI causes an entity to throw its target at nearby allies if they are far.
 */
public class ThrowEnemyEliteGoal extends AbstractPathingEliteGoal {
    /** Differentiates between the different actions that can be taken by this AI. */
    private enum Activity { NONE, GRAB, CARRY }
    
    /** The current action being performed. */
    private Activity currentActivity = Activity.NONE;
    
    /** The entity this mob is following. */
    private MobEntity throwTarget;
    /** Ticks until next attack. */
    private int attackTime;
    /** Ticks until the entity gives up. */
    private int giveUpDelay;
    /** Number of times the entity will re-grab escaping players before giving up. */
    private int extraGrabAttempts;
    
    ThrowEnemyEliteGoal( MobEntity entity, CompoundNBT aiTag ) {
        super( entity, aiTag );
        setFlags( EnumSet.of( Flag.MOVE, Flag.LOOK ) );
    }
    
    /** @return Returns true if this AI can be activated. */
    @Override
    public boolean canUse() {
        final LivingEntity target = mob.getTarget();
        return attackTime-- <= 0 && target != null && target.isOnGround() && !target.isPassenger() &&
                !mob.isPassenger() && mob.getRandom().nextInt( 20 ) == 0 && findThrowTarget();
    }
    
    /** Called when this AI is activated. */
    @Override
    public void start() {
        giveUpDelay = 0;
        currentActivity = Activity.GRAB;
        extraGrabAttempts = Config.ELITE_AI.THROW_ENEMY.reGrabs.next( mob.getRandom() );
        final LivingEntity target = mob.getTarget();
        if( target != null ) {
            startPathing( target, Config.ELITE_AI.THROW_ENEMY.speedToTarget.get() );
        }
    }
    
    /** @return Called each update while active and returns true if this AI can remain active. */
    @Override
    public boolean canContinueToUse() {
        final LivingEntity target = mob.getTarget();
        return giveUpDelay++ <= 400 && currentActivity != Activity.NONE && !mob.isPassenger() && // Can still use
                throwTarget != null && throwTarget.isAlive() && target != null && target.isAlive() && // Targets are still valid
                (currentActivity != Activity.GRAB || !target.isPassenger()); // Target wasn't grabbed by something else
    }
    
    /** @return Returns true if this AI can be interrupted by a higher priority conflicting task. */
    @Override
    public boolean isInterruptable() { return false; }
    
    /** Called each tick while this AI is active. */
    @Override
    public void tick() {
        switch( currentActivity ) {
            case GRAB:
                tickGrab();
                break;
            case CARRY:
                tickCarry();
                break;
            default:
        }
    }
    
    /** Called each tick while this AI is active and in grab mode. */
    private void tickGrab() {
        final LivingEntity target = mob.getTarget();
        if( target == null ) return;
        
        mob.getLookControl().setLookAt( target, 30.0F, 30.0F );
        
        if( mob.distanceToSqr( target ) <= mob.getBbWidth() * mob.getBbWidth() * 4.0F + target.getBbWidth() ) {
            // Pick up the target
            target.startRiding( mob, true );
            startPathing( throwTarget, Config.ELITE_AI.THROW_ENEMY.speedToAlly.get() );
            mob.swing( Hand.MAIN_HAND );
            attackTime = 20 + mob.getRandom().nextInt( 10 );
            currentActivity = Activity.CARRY;
        }
        else {
            tickPathing( target, Config.ELITE_AI.THROW_ENEMY.speedToTarget.get() );
        }
    }
    
    /** Called each tick while this AI is active and in grab mode. */
    private void tickCarry() {
        if( mob.isVehicle() ) {
            // Is carrying the target
            mob.getLookControl().setLookAt( throwTarget, 30.0F, 30.0F );
            
            if( attackTime-- <= 0 && mob.getRandom().nextInt( 10 ) == 0 &&
                    mob.distanceToSqr( throwTarget ) <= Config.ELITE_AI.THROW_ENEMY.throwRangeSqrMax.get() ) {
                // Throw the held entity
                final Entity throwEntity = mob.getPassengers().get( 0 );
                throwEntity.stopRiding();
                throwEntity.setOnGround( false );
                throwEntity.fallDistance = 0.0F;
                final Vector3d jumpXZ = new Vector3d( throwTarget.getX() - mob.getX(), 0.0, throwTarget.getZ() - mob.getZ() )
                        .normalize().scale( Config.ELITE_AI.THROW_ENEMY.throwSpeedForward.get() ).add( mob.getDeltaMovement().scale( 0.2 ) );
                throwEntity.setDeltaMovement( jumpXZ.x, Config.ELITE_AI.THROW_ENEMY.throwSpeedUpward.get(), jumpXZ.z );
                if( throwEntity instanceof ServerPlayerEntity ) {
                    try {
                        ((ServerPlayerEntity) throwEntity).connection.send( new SEntityVelocityPacket( throwEntity ) );
                    }
                    catch( Exception ex ) {
                        ex.printStackTrace();
                    }
                }
                
                mob.getNavigation().stop();
                mob.swing( Hand.MAIN_HAND );
                currentActivity = Activity.NONE;
            }
            else {
                tickPathing( throwTarget, Config.ELITE_AI.THROW_ENEMY.speedToAlly.get() );
            }
        }
        else if( extraGrabAttempts > 0 ) {
            // Try to re-grab escaping target
            extraGrabAttempts--;
            currentActivity = Activity.GRAB;
        }
        else {
            // The target has escaped
            currentActivity = Activity.NONE;
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
        attackTime = Config.ELITE_AI.THROW_ENEMY.cooldown.next( mob.getRandom() );
    }
    
    /** Searches for a nearby ally that the target can be thrown to and targets it. Returns true if one is found. */
    private boolean findThrowTarget() {
        final LivingEntity target = mob.getTarget();
        if( target == null ) return false;
        
        final double maxTargetRange = Config.ELITE_AI.THROW_ENEMY.throwRangeSqrMax.getSqrRoot() + Config.ELITE_AI.THROW_ENEMY.carryRange.get();
        final List<Entity> entitiesNearTarget = mob.level.getEntities( mob, target.getBoundingBox().inflate( maxTargetRange + 2.0 ) );
        int mostNearby = -1;
        for( Entity entity : entitiesNearTarget ) {
            // Check if the entity is a valid ally
            if( !(entity instanceof MobEntity) || !entity.isAlive() || target != ((MobEntity) entity).getTarget() )
                continue;
            final double distanceSqr = entity.distanceToSqr( target );
            if( distanceSqr > maxTargetRange * maxTargetRange ) continue;
            if( distanceSqr < Config.ELITE_AI.THROW_ENEMY.throwRangeSqrMin.get() ) {
                // Do not throw the target if allies are already near it; cancel everything
                throwTarget = null;
                return false;
            }
            
            // Pick the ally that has the most other entities around it
            final int nearby = entity.level.getEntities( entity, entity.getBoundingBox().inflate( 4.0 ) ).size();
            if( nearby > mostNearby ) {
                mostNearby = nearby;
                throwTarget = (MobEntity) entity;
            }
        }
        return throwTarget != null;
    }
}