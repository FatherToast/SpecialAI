package fathertoast.specialai.ai.elite;

import fathertoast.specialai.config.Config;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * This AI causes an entity to periodically charge up and quickly run forward, dealing high damage and knockback.
 * If the entity hits a wall while charging, they will enter a 'stunned' state for a short time, rendering them helpless.
 */
public class SlamEliteGoal extends AbstractEliteGoal {
    /** Differentiates between the different actions that can be taken by this AI. */
    private enum Activity { NONE, CHARGE_UP, SWINGING }
    
    /** The current action being performed. */
    private Activity currentActivity = Activity.NONE;
    
    /** Ticks until the next attack. */
    private int attackTime;
    
    SlamEliteGoal( Mob entity, CompoundTag aiTag ) {
        super( entity, aiTag );
        setFlags( EnumSet.of( Flag.MOVE, Flag.LOOK ) );
    }
    
    /** @return Returns true if this AI can be activated. */
    @Override
    public boolean canUse() {
        final LivingEntity lastHurtBy = mob.getLastHurtByMob();
        if( --attackTime > 0 || lastHurtBy == null ) return false;
        
        // Wait until invulnerability window ends, then chance to activate gets lower with time
        int timeSinceHurt = mob.tickCount - mob.getLastHurtByMobTimestamp();
        if( timeSinceHurt <= 10 || mob.getRandom().nextInt( timeSinceHurt - 5 ) != 0 ) return false;
        
        final LivingEntity target = mob.getTarget();
        if( target == lastHurtBy ) {
            final double distanceSqr = mob.distanceToSqr( target );
            return distanceSqr <= Config.ELITE_AI.SLAM.rangeSqrMax.get() && distanceSqr >= Config.ELITE_AI.SLAM.rangeSqrMin.get()
                    && mob.hasLineOfSight( target );
        }
        return false;
    }
    
    /** Called when this AI is activated. */
    @Override
    public void start() {
        mob.getNavigation().stop();
        attackTime = Config.ELITE_AI.SLAM.chargeUpDuration.get();
        currentActivity = Activity.CHARGE_UP;
        mob.setAggressive( true );
        mob.setDeltaMovement( mob.getDeltaMovement().scale( 0.2 ).add( 0.0, 0.4, 0.0 ) );
        mob.playSound( SoundEvents.ARMOR_EQUIP_IRON,
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
            case SWINGING:
                tickSwinging();
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
        
        // The entity does nothing while charging up except try to keep its hand raised and rapidly face its target
        mob.getLookControl().setLookAt( target, 100.0F, 100.0F );
        
        if( attackTime <= 0 ) {
            // Charge up complete, transition to swinging
            mob.setAggressive( false );
            mob.swing( InteractionHand.MAIN_HAND );
            attackTime = getSwingDuration();
            currentActivity = Activity.SWINGING;
            mob.playSound( SoundEvents.PLAYER_ATTACK_SWEEP,
                    1.0F, 1.0F / (mob.getRandom().nextFloat() * 0.4F + 0.8F) );
        }
    }
    
    /** @return The expected swing animation time, calculated the same way as {@link LivingEntity#getCurrentSwingDuration()}. */
    @SuppressWarnings("JavadocReference")
    private int getSwingDuration() {
        if( MobEffectUtil.hasDigSpeed( mob ) ) {
            return 6 - (1 + MobEffectUtil.getDigSpeedAmplification( mob ));
        }
        MobEffectInstance miningFatigue = mob.getEffect( MobEffects.DIG_SLOWDOWN );
        return miningFatigue == null ? 6 : 6 + (1 + miningFatigue.getAmplifier()) * 2;
    }
    
    /** Called each tick while this AI is active and in swinging mode. */
    private void tickSwinging() {
        // Literally do nothing until the very end of this state
        if( attackTime <= 0 ) {
            // Animation should be completed, perform slam damage and effects
            mob.playSound( SoundEvents.AXE_STRIP,
                    1.0F, 1.0F / (mob.getRandom().nextFloat() * 0.4F + 0.8F) );
            
            Vec3 offset = mob.getViewVector( 1.0F )
                    .multiply( 1.0 + mob.getBbWidth(), 0.0, 1.0 + mob.getBbWidth() );
            mob.level().explode( mob,
                    mob.getX() + offset.x, mob.getY() + offset.y, mob.getZ() + offset.z,
                    (float) Config.ELITE_AI.SLAM.power.get(), false, Level.ExplosionInteraction.NONE );
            
            currentActivity = Activity.NONE;
        }
    }
    
    /** Called when this AI is deactivated. */
    @Override
    public void stop() {
        attackTime = Config.ELITE_AI.SLAM.cooldown.next( mob.getRandom() );
    }
}