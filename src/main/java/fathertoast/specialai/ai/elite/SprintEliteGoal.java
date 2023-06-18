package fathertoast.specialai.ai.elite;

import fathertoast.specialai.config.Config;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.nbt.CompoundNBT;

import java.util.EnumSet;

/**
 * This AI causes an entity to sprint to their target when at range.
 */
public class SprintEliteGoal extends AbstractEliteGoal {
    /** Ticks until the AI can activate again. */
    private int cooldownTimer;
    /** Ticks until the entity gives up. */
    private int giveUpDelay;
    
    SprintEliteGoal( MobEntity entity, CompoundNBT aiTag ) {
        super( entity, aiTag );
        setFlags( EnumSet.of( Flag.MOVE, Flag.LOOK ) );
    }
    
    /** @return Returns true if this AI can be activated. */
    @Override
    public boolean canUse() {
        final LivingEntity target = mob.getTarget();
        if( --cooldownTimer > 0 || target == null || mob.isPassenger() || !target.isSprinting() && mob.getRandom().nextInt( 20 ) != 0 )
            return false;
        
        return mob.distanceToSqr( target ) > Config.ELITE_AI.SPRINT.rangeSqrMin.get();
    }
    
    /** Called when this AI is activated. */
    @Override
    public void start() {
        giveUpDelay = 0;
        mob.setSprinting( true );
        mob.getNavigation().stop();
    }
    
    /** @return Called each update while active and returns true if this AI can remain active. */
    @Override
    public boolean canContinueToUse() {
        final LivingEntity target = mob.getTarget();
        if( ++giveUpDelay < 200 && target != null && !mob.isPassenger() ) {
            final double distance = mob.distanceToSqr( target );
            return distance > Config.ELITE_AI.SPRINT.endRangeSqrMax.get() ||
                    distance > Config.ELITE_AI.SPRINT.endRangeSqrMin.get() && mob.getRandom().nextInt( 10 ) != 0;
        }
        return false;
    }
    
    /** @return Returns true if this AI can be interrupted by a higher priority conflicting task. */
    @Override
    public boolean isInterruptable() { return false; }
    
    /** Called each tick while this AI is active. */
    @Override
    public void tick() {
        final LivingEntity target = mob.getTarget();
        if( target == null ) return;
        
        mob.getLookControl().setLookAt( target, 30.0F, 30.0F );
        if( mob.getNavigation().isDone() ) {
            mob.getNavigation().moveTo( target, Config.ELITE_AI.SPRINT.runSpeed.get() );
        }
        cooldownTimer++;
    }
    
    /** Called when this AI is deactivated. */
    @Override
    public void stop() {
        mob.setSprinting( false );
        mob.getNavigation().stop();
    }
}