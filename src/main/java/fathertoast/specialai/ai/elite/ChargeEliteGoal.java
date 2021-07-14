package fathertoast.specialai.ai.elite;

import fathertoast.specialai.ai.AnimalMeleeAttackGoal;
import fathertoast.specialai.config.Config;
import net.minecraft.command.arguments.EntityAnchorArgument;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.server.SEntityVelocityPacket;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Hand;
import net.minecraft.util.math.vector.Vector3d;

import java.util.EnumSet;
import java.util.List;

/**
 * This AI causes an entity to periodically charge up and quickly run forward, dealing high damage and knockback.
 * If the entity hits a wall while charging, they will enter a 'stunned' state for a short time, rendering them helpless.
 */
public class ChargeEliteGoal extends AbstractEliteGoal {
    /** Differentiates between the different actions that can be taken by this AI. */
    private enum Activity { NONE, CHARGE_UP, CHARGING, STUNNED }
    
    /** The current action being performed. */
    private Activity currentActivity = Activity.NONE;
    
    /** Ticks until the next attack. */
    private int attackTime;
    /** The direction of this mob's current attack. */
    private Vector3d attackVec;
    
    /** Keeps track of which arm to swing next when 'flailing' during the stunned state. */
    private boolean swingOffhand;
    
    /** The mob's original step height. */
    private float stepHeight = Float.NaN;
    
    ChargeEliteGoal( MobEntity entity ) {
        super( entity );
        setFlags( EnumSet.of( Flag.MOVE, Flag.LOOK, Flag.JUMP ) );
    }
    
    /** @return Returns true if this AI can be activated. */
    @Override
    public boolean canUse() {
        if( --attackTime > 0 || !mob.isOnGround() || mob.isPassenger() || mob.getRandom().nextInt( 10 ) != 0 )
            return false;
        
        final LivingEntity target = mob.getTarget();
        if( target != null ) {
            final double distanceSqr = mob.distanceToSqr( target );
            return distanceSqr <= Config.ELITE_AI.CHARGE.rangeSqrMax.get() && distanceSqr >= Config.ELITE_AI.CHARGE.rangeSqrMin.get()
                    && mob.canSee( target );
        }
        return false;
    }
    
    /** Called when this AI is activated. */
    @Override
    public void start() {
        mob.getNavigation().stop();
        attackTime = Config.ELITE_AI.CHARGE.chargeUpDuration.get();
        currentActivity = Activity.CHARGE_UP;
        mob.swing( Hand.OFF_HAND );
        mob.setDeltaMovement( mob.getDeltaMovement().add( 0.0, 0.3, 0.0 ) );
    }
    
    /** @return Called each update while active and returns true if this AI can remain active. */
    @Override
    public boolean canContinueToUse() {
        return currentActivity != Activity.NONE;
    }
    
    /** @return Returns true if this AI can be interrupted by a higher priority conflicting task. */
    @Override
    public boolean isInterruptable() { return false; }
    
    /** Called each tick while this AI is active. */
    @Override
    public void tick() {
        attackTime--;
        switch( currentActivity ) {
            case CHARGE_UP:
                tickChargeUp();
                break;
            case CHARGING:
                tickCharging();
                break;
            case STUNNED:
                tickStunned();
                break;
            default:
        }
    }
    
    /** Called each tick while this AI is active and in charge up mode. */
    private void tickChargeUp() {
        final LivingEntity target = mob.getTarget();
        if( target == null || !target.isAlive() ) {
            currentActivity = Activity.NONE;
            return;
        }
        
        // The entity does nothing while charging up except rapidly face its target
        mob.getLookControl().setLookAt( target, 100.0F, 100.0F );
        
        if( attackTime <= 0 ) {
            // Charge up complete, lock in target and transition to charging
            attackVec = new Vector3d( target.getX() - mob.getX(), 0.0, target.getZ() - mob.getZ() ).normalize();
            if( mob.maxUpStep < 1.0F ) {
                // Force step height to be at least 1 block
                stepHeight = mob.maxUpStep;
                mob.maxUpStep = 1.0F;
            }
            mob.setSprinting( true );
            
            attackTime = Config.ELITE_AI.CHARGE.chargingDuration.get();
            currentActivity = Activity.CHARGING;
        }
    }
    
    /** Called each tick while this AI is active and in charging mode. */
    private void tickCharging() {
        final LivingEntity target = mob.getTarget();
        
        mob.lookAt( EntityAnchorArgument.Type.FEET, mob.position().add( attackVec ) );
        mob.setDeltaMovement(
                attackVec.x * Config.ELITE_AI.CHARGE.chargingSpeed.get(),
                mob.getDeltaMovement().y,
                attackVec.z * Config.ELITE_AI.CHARGE.chargingSpeed.get()
        );
        final boolean hit;
        if( target != null ) {
            List<Entity> list = mob.level.getEntities( mob, mob.getBoundingBox().inflate( 0.2 ) );
            hit = list.contains( target );
        }
        else {
            hit = false;
        }
        
        if( hit ) {
            // Has hit the target
            AnimalMeleeAttackGoal.doHurtTarget( mob, target );
            mob.swing( Hand.MAIN_HAND );
            
            mob.setDeltaMovement(
                    attackVec.x * -1.2,
                    0.4,
                    attackVec.z * -1.2
            );
            target.setDeltaMovement(
                    attackVec.x * Config.ELITE_AI.CHARGE.knockbackSpeed.get(),
                    0.5,
                    attackVec.z * Config.ELITE_AI.CHARGE.knockbackSpeed.get()
            );
            if( target instanceof ServerPlayerEntity ) {
                try {
                    ((ServerPlayerEntity) target).connection.send( new SEntityVelocityPacket( target ) );
                }
                catch( Exception ex ) {
                    ex.printStackTrace();
                }
            }
            currentActivity = Activity.NONE;
        }
        else if( mob.horizontalCollision ) {
            // Has hit a wall
            mob.hurt( DamageSource.FLY_INTO_WALL, (float) Config.ELITE_AI.CHARGE.selfDamage.get() );
            mob.setDeltaMovement(
                    attackVec.x * -0.5,
                    0.5,
                    attackVec.z * -0.5
            );
            cancelCharging();
            attackTime = Config.ELITE_AI.CHARGE.stunnedDuration.get();
            currentActivity = Activity.STUNNED;
        }
        else if( attackTime <= 0 || mob.isInWater() || mob.isInLava() ) {
            // Charge is completed without hitting anything
            currentActivity = Activity.NONE;
        }
    }
    
    /** Called each tick while this AI is active and in stunned mode. */
    private void tickStunned() {
        // Perform 'flailing' animation
        if( !mob.swinging ) {
            if( swingOffhand ) mob.swing( Hand.OFF_HAND );
            else mob.swing( Hand.MAIN_HAND );
            swingOffhand = !swingOffhand;
        }
        
        if( attackTime <= 0 ) {
            // Stunned duration has ended
            currentActivity = Activity.NONE;
        }
    }
    
    /** Called when this AI is deactivated. */
    @Override
    public void stop() {
        attackTime = Config.ELITE_AI.CHARGE.cooldown.next( mob.getRandom() );
        cancelCharging();
    }
    
    /** Turns off the sprinting state and reverts to original step height, if needed. */
    private void cancelCharging() {
        if( !Float.isNaN( stepHeight ) ) {
            mob.maxUpStep = stepHeight;
            stepHeight = Float.NaN;
        }
        mob.setSprinting( false );
    }
}