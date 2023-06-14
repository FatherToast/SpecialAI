package fathertoast.specialai.ai.elite;

import fathertoast.crust.api.lib.LevelEventHelper;
import fathertoast.specialai.config.Config;
import net.minecraft.command.arguments.EntityAnchorArgument;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.vector.Vector3d;

import java.util.EnumSet;

/**
 * This AI causes an entity to periodically charge up and fire a stream of arrows.
 */
public class BarrageEliteGoal extends AbstractEliteGoal {
    /** Differentiates between the different actions that can be taken by this AI. */
    private enum Activity { NONE, CHARGE_UP, SHOOTING }
    
    /** The current action being performed. */
    private Activity currentActivity = Activity.NONE;
    
    /** Ticks until the next attack. */
    private int attackTime;
    /** The direction of this mob's current attack. */
    private Vector3d attackVec;
    
    BarrageEliteGoal( MobEntity entity ) {
        super( entity );
        setFlags( EnumSet.of( Flag.MOVE, Flag.LOOK, Flag.JUMP ) );
    }
    
    /** @return Returns true if this AI can be activated. */
    @Override
    public boolean canUse() {
        if( --attackTime > 0 || !mob.isOnGround() || mob.getRandom().nextInt( 10 ) != 0 )
            return false;
        
        final LivingEntity target = mob.getTarget();
        if( target != null ) {
            final double distanceSqr = mob.distanceToSqr( target );
            return distanceSqr <= Config.ELITE_AI.BARRAGE.rangeSqrMax.get() && distanceSqr >= Config.ELITE_AI.BARRAGE.rangeSqrMin.get()
                    && mob.canSee( target );
        }
        return false;
    }
    
    /** Called when this AI is activated. */
    @Override
    public void start() {
        mob.getNavigation().stop();
        attackTime = Config.ELITE_AI.BARRAGE.chargeUpDuration.get();
        currentActivity = Activity.CHARGE_UP;
        mob.swing( Hand.OFF_HAND );
        mob.level.playSound( null, mob.blockPosition(), SoundEvents.STONE_BUTTON_CLICK_ON, mob.getSoundSource(),
                1.0F, 1.0F / (mob.getRandom().nextFloat() * 0.4F + 0.8F) );
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
            case SHOOTING:
                tickShooting();
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
        
        // The entity does nothing while charging except rapidly face its target
        mob.getLookControl().setLookAt( target, 100.0F, 100.0F );
        
        if( attackTime <= 0 ) {
            // Charge up complete, lock in target and transition to shooting
            attackVec = new Vector3d(
                    target.getX(),
                    target.getY( 0.333 ) - mob.getEyeHeight(),
                    target.getZ()
            ).subtract( mob.position() );
            attackVec = attackVec.add(
                    0.0,
                    Math.sqrt( attackVec.x * attackVec.x + attackVec.z * attackVec.z ) * 0.2,
                    0.0 );
            attackTime = Config.ELITE_AI.BARRAGE.shootingDuration.get();
            currentActivity = Activity.SHOOTING;
        }
    }
    
    /** Called each tick while this AI is active and in shooting mode. */
    private void tickShooting() {
        mob.lookAt( EntityAnchorArgument.Type.FEET, mob.position().add( attackVec ) );
        if( attackTime % Config.ELITE_AI.BARRAGE.shotTime.get() == 0 ) {
            // Fire an arrow
            ArrowEntity arrow = new ArrowEntity( mob.level, mob.getX(), mob.getY() + mob.getEyeHeight(), mob.getZ() );
            arrow.setOwner( mob );
            arrow.setBaseDamage( Config.ELITE_AI.BARRAGE.arrowDamage.get() +
                    mob.getRandom().nextGaussian() * 0.25 + mob.level.getDifficulty().getId() * 0.11 );
            if( mob.isOnFire() ) {
                arrow.setSecondsOnFire( 100 );
            }
            arrow.shoot( attackVec.x, attackVec.y, attackVec.z, 1.8F, (float) Config.ELITE_AI.BARRAGE.arrowVariance.get() );
            
            LevelEventHelper.DISPENSER_LAUNCH.play( mob );
            mob.level.addFreshEntity( arrow );
        }
        else if( this.attackTime <= 0 ) {
            // Shooting complete, transition to end
            currentActivity = Activity.NONE;
        }
    }
    
    /** Called when this AI is deactivated. */
    @Override
    public void stop() {
        attackTime = Config.ELITE_AI.BARRAGE.cooldown.next( mob.getRandom() );
    }
}