package fathertoast.specialai.ai;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumSet;

/**
 * This AI causes the entity to make a short sidestep 'hop' when an arrow is fired in its direction.
 */
public class DodgeArrowsGoal extends Goal {
    
    /** Called for each arrow the first time it is spawned in the world to check if any entities should try to dodge it. */
    public static void doDodgeCheckForArrow( Entity arrow ) {
        // Get the world instance
        if( !(arrow.level() instanceof ServerLevel level) ) return;

        // Calculate the arrow's width and direction
        float width = arrow.getBbWidth() + 0.3F;
        final Vec3 arrowMotion = arrow.getDeltaMovement();
        final double vH = Math.sqrt( arrowMotion.x * arrowMotion.x + arrowMotion.z * arrowMotion.z );
        final Vec3 arrowDirection = new Vec3( arrowMotion.x / vH, 0.0, arrowMotion.z / vH );
        
        // Check all entities in the world that may be in the arrow's line of fire
        final int rangeVertical = 16;
        final int rangeHorizontal = 24;
        for( Entity entity : level.getAllEntities() ) {
            if( entity instanceof PathfinderMob ) {
                // Check vertical range
                final int distanceY = Math.abs( (int) entity.position().y - (int) arrow.position().y );
                if( distanceY <= rangeVertical ) {
                    // Check horizontal range
                    final double distanceX = entity.position().x - arrow.position().x;
                    final double distanceZ = entity.position().z - arrow.position().z;
                    final double distanceH = Math.sqrt( distanceX * distanceX + distanceZ * distanceZ );
                    if( distanceH <= rangeHorizontal ) {
                        // Check ray width
                        final double cos = (arrowDirection.x * distanceX + arrowDirection.z * distanceZ) / distanceH;
                        final double sin = Math.sqrt( 1 - cos * cos );
                        if( width > distanceH * sin ) {
                            tryDodgeArrow( (PathfinderMob) entity, arrowDirection );
                        }
                    }
                }
            }
        }
    }
    
    /** Alerts the entity's arrow dodge AI, if it has one, that an arrow has been fired at the entity. */
    private static void tryDodgeArrow( PathfinderMob entity, Vec3 arrowDirection ) {
        for( WrappedGoal task : new ArrayList<>( entity.goalSelector.getAvailableGoals() ) ) {
            if( task.getGoal() instanceof DodgeArrowsGoal ) {
                ((DodgeArrowsGoal) task.getGoal()).setDodgeTarget( arrowDirection );
            }
        }
    }
    
    /** The owner of this AI. */
    protected final Mob mob;
    /** The chance that this AI will activate when an arrow is fired at the entity. */
    private final double dodgeChance;
    
    /** The horizontal direction of the arrow being avoided. */
    private Vec3 arrowMotionDirection;
    /** Ticks until the entity gives up. */
    private int giveUpDelay;
    /** Used to prevent mobs from leaping all over the place from multiple arrows. */
    private int dodgeDelay;
    
    /**
     * @param entity The owner of this AI.
     * @param chance The chance for the entity to dodge arrows fired.
     */
    public DodgeArrowsGoal( Mob entity, double chance ) {
        mob = entity;
        dodgeChance = chance;
        setFlags( EnumSet.of( Flag.JUMP ) );
    }
    
    /** Tells this AI that an arrow has been fired toward the entity and provides the arrow's facing. */
    private void setDodgeTarget( @Nullable Vec3 arrowDirection ) {
        if( arrowDirection == null ) {
            arrowMotionDirection = null;
            giveUpDelay = 0;
        }
        else if( dodgeDelay <= 0 && mob.getRandom().nextDouble() < dodgeChance ) {
            arrowMotionDirection = arrowDirection;
            giveUpDelay = 10;
        }
    }
    
    /** @return Returns true if this AI can be activated. */
    @Override
    public boolean canUse() {
        return dodgeDelay-- <= 0 && arrowMotionDirection != null && giveUpDelay-- > 0 && mob.onGround() && !mob.isPassenger();
    }
    
    /** Called when this AI is activated. */
    @Override
    public void start() {
        if( arrowMotionDirection != null ) {
            // Calculate a vector perpendicular to the arrow's movement direction
            Vec3 selfAxis = new Vec3( 0.0, 1.0, 0.0 );
            Vec3 dodgeDirection = selfAxis.cross( arrowMotionDirection );
            
            // Pick a random direction to sidestep
            double velocity = 0.8;
            if( mob.getRandom().nextBoolean() ) velocity = -velocity;
            
            // Perform the dodge sidestep
            mob.setDeltaMovement( dodgeDirection.x * velocity, 0.3, dodgeDirection.z * velocity );
            
            // Put this AI on cooldown for 2 seconds
            setDodgeTarget( null );
            dodgeDelay = 40;
        }
    }
    
    /** @return Called each update while active and returns true if this AI can remain active. */
    @Override
    public boolean canContinueToUse() {
        return false; // This AI does not have any ongoing effects
    }
}