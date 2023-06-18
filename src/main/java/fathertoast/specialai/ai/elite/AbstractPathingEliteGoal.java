package fathertoast.specialai.ai.elite;

import net.minecraft.entity.Entity;
import net.minecraft.entity.MobEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.vector.Vector3d;

/**
 * Contains the most basic implementations used by all (or most) elite AI goals that regularly pathfind to entities.
 */
public abstract class AbstractPathingEliteGoal extends AbstractEliteGoal {
    
    /** The last position pathed to. */
    private Vector3d pathedTargetPos = Vector3d.ZERO;
    /** Time until the entity can update its path. */
    private int ticksUntilNextPathRecalculation;
    
    AbstractPathingEliteGoal( MobEntity entity, CompoundNBT aiTag ) { super( entity, aiTag ); }
    
    /** Called to path to a target; should only be directly called when starting to path to a new target. */
    public void startPathing( Entity entity, double speed ) {
        // Start with a basic recalculation delay; randomness staggers similar logic
        ticksUntilNextPathRecalculation = 4 + mob.getRandom().nextInt( 7 );
        
        // Update pathing less frequently at long range
        final double distanceSqr = mob.distanceToSqr( entity );
        if( distanceSqr > 1024.0 ) {
            ticksUntilNextPathRecalculation += 10;
        }
        else if( distanceSqr > 256.0 ) {
            ticksUntilNextPathRecalculation += 5;
        }
        
        // Actually path to the target
        pathedTargetPos = entity.position();
        if( !mob.getNavigation().moveTo( entity, speed ) ) {
            // Apply larger penalty on failure
            ticksUntilNextPathRecalculation += 15;
        }
    }
    
    /** Called to update pathing; used each tick while this AI is active and is trying to follow. */
    public void tickPathing( Entity entity, double speed ) {
        ticksUntilNextPathRecalculation--;
        if( ticksUntilNextPathRecalculation <= 0 && (entity.distanceToSqr( pathedTargetPos ) >= 1.0 || mob.getRandom().nextFloat() < 0.05F) ) {
            startPathing( entity, speed );
        }
    }
}