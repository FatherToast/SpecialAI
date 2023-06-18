package fathertoast.specialai.ai.elite;

import fathertoast.crust.api.lib.LevelEventHelper;
import fathertoast.specialai.config.Config;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;

/**
 * This AI causes an entity to heal and buff its nearby allies and prefer to follow allies over attacking.
 */
public class ShamanEliteGoal extends AbstractPathingEliteGoal {
    /** Selects mobs that are alive. */
    private static final Predicate<Entity> MOB_SELECTOR = entity ->
            entity instanceof MobEntity && entity.isAlive() && !entity.isSpectator();
    
    /** The entity this mob is following. */
    private MobEntity followTarget;
    /** Ticks until next aura pulse. */
    private int pulseTime;
    
    ShamanEliteGoal( MobEntity entity, CompoundNBT aiTag ) {
        super( entity, aiTag );
        setFlags( EnumSet.of( Flag.MOVE, Flag.LOOK ) );
    }
    
    /** @return Returns true if this AI can be activated. */
    @Override
    public boolean canUse() {
        return findFollowTarget();
    }
    
    /** Called when this AI is activated. */
    @Override
    public void start() {
        startPathing( followTarget, 1.2 );
    }
    
    /** @return Called each update while active and returns true if this AI can remain active. */
    @Override
    public boolean canContinueToUse() {
        final LivingEntity target = mob.getTarget();
        return target != null && followTarget != null && followTarget.isAlive() &&
                (target == followTarget.getTarget() || findFollowTarget());
    }
    
    /** Called each tick while this AI is active. */
    @Override
    public void tick() {
        final LivingEntity target = mob.getTarget();
        
        // Follow an ally
        final double distanceSqr = mob.distanceToSqr( followTarget );
        if( distanceSqr > 36.0 ) {
            tickPathing( followTarget, 1.2 );
        }
        else if( distanceSqr < 4.0 && mob.getNavigation().isInProgress() ) {
            mob.getNavigation().stop();
        }
        
        // Look at the follow target (or attack target if not actively following)
        if( mob.getNavigation().isDone() ) {
            if( target != null ) {
                mob.getLookControl().setLookAt( target, 30.0F, 30.0F );
            }
        }
        else {
            mob.getLookControl().setLookAt( followTarget, 30.0F, 30.0F );
        }
        
        // Perform aura pulse
        pulseTime--;
        if( pulseTime <= 0 ) {
            pulseTime = 40;
            List<Entity> list = getHotMILFsInArea();
            for( Entity entity : list ) {
                if( isInRangeAlly( target, entity ) ) {
                    auraPulse( (MobEntity) entity );
                }
            }
        }
    }
    
    /** Called when this AI is deactivated. */
    @Override
    public void stop() {
        followTarget = null;
    }
    
    /** Applies all effects from one aura pulse to the target mob. */
    private static void auraPulse( MobEntity ally ) {
        // Heal and extinguish burning
        if( Config.ELITE_AI.SHAMAN.healAmount.get() > 0.0 ) {
            ally.heal( (float) Config.ELITE_AI.SHAMAN.healAmount.get() );
        }
        if( Config.ELITE_AI.SHAMAN.extinguish.get() ) {
            ally.clearFire();
        }
        
        // Apply potion effects
        if( Config.ELITE_AI.SHAMAN.strengthPotency.get() >= 0 ) {
            ally.addEffect( new EffectInstance( Effects.DAMAGE_BOOST, 60, Config.ELITE_AI.SHAMAN.strengthPotency.get() ) );
        }
        if( Config.ELITE_AI.SHAMAN.resistancePotency.get() >= 0 ) {
            ally.addEffect( new EffectInstance( Effects.DAMAGE_RESISTANCE, 60, Config.ELITE_AI.SHAMAN.resistancePotency.get() ) );
        }
        if( Config.ELITE_AI.SHAMAN.speedPotency.get() >= 0 ) {
            ally.addEffect( new EffectInstance( Effects.MOVEMENT_SPEED, 60, Config.ELITE_AI.SHAMAN.speedPotency.get() ) );
        }
        if( Config.ELITE_AI.SHAMAN.slowFalling.get() ) {
            ally.addEffect( new EffectInstance( Effects.SLOW_FALLING, 60, 0 ) );
        }
        if( Config.ELITE_AI.SHAMAN.fireResistance.get() ) {
            ally.addEffect( new EffectInstance( Effects.FIRE_RESISTANCE, 60, 0 ) );
        }
        if( Config.ELITE_AI.SHAMAN.waterBreathing.get() ) {
            ally.addEffect( new EffectInstance( Effects.WATER_BREATHING, 60, 0 ) );
        }
        
        // Play green particle effects
        LevelEventHelper.BONE_MEAL_USE.play( ally.level, null, ally.blockPosition() );
    }
    
    /** Searches for a nearby ally (targeting the same entity) to follow. Returns true if one is found. */
    private boolean findFollowTarget() {
        final LivingEntity target = mob.getTarget();
        if( target != null ) {
            List<Entity> list = getHotMILFsInArea();
            for( Entity entity : list ) {
                if( isInRangeAlly( target, entity ) ) {
                    followTarget = (MobEntity) entity;
                    return true;
                }
            }
        }
        return false;
    }
    
    /** @return Returns all living mobs that may be in range and could potentially be allies. */
    private List<Entity> getHotMILFsInArea() {
        List<Entity> list = mob.level.getEntities( mob,
                mob.getBoundingBox().inflate( Config.ELITE_AI.SHAMAN.auraRangeSqr.getSqrRoot() ), MOB_SELECTOR );
        Collections.shuffle( list );
        return list;
    }
    
    /** @return Returns true if the entity is in range and an ally (targeting the same entity). */
    private boolean isInRangeAlly( @Nullable LivingEntity target, Entity entity ) {
        return entity instanceof MobEntity && mob.distanceToSqr( entity ) <= Config.ELITE_AI.SHAMAN.auraRangeSqr.get() &&
                target == ((MobEntity) entity).getTarget();
    }
}