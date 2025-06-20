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
 * This AI causes the entity to make a short sidestep 'hop' when a projectile is fired in its direction.
 */
public class DodgeProjectilesGoal extends Goal {
    
    /** Called for each projectile the first time it is spawned in the world to check if any entities should try to dodge it. */
    public static void doDodgeCheckForProjectile( Entity projectile ) {
        // Get the world instance
        if( !(projectile.level() instanceof ServerLevel level) ) return;

        // Calculate the projectile's width and direction
        float width = projectile.getBbWidth() + 0.3F;
        final Vec3 projectileMotion = projectile.getDeltaMovement();
        final double vH = Math.sqrt( projectileMotion.x * projectileMotion.x + projectileMotion.z * projectileMotion.z );
        final Vec3 projectileDirection = new Vec3( projectileMotion.x / vH, 0.0, projectileMotion.z / vH );
        
        // Check all entities in the world that may be in the projectile's line of fire
        final int rangeVertical = 16;
        final int rangeHorizontal = 24;
        for( Entity entity : level.getAllEntities() ) {
            if( entity instanceof PathfinderMob ) {
                // Check vertical range
                final int distanceY = Math.abs( (int) entity.position().y - (int) projectile.position().y );
                if( distanceY <= rangeVertical ) {
                    // Check horizontal range
                    final double distanceX = entity.position().x - projectile.position().x;
                    final double distanceZ = entity.position().z - projectile.position().z;
                    final double distanceH = Math.sqrt( distanceX * distanceX + distanceZ * distanceZ );
                    if( distanceH <= rangeHorizontal ) {
                        // Check ray width
                        final double cos = (projectileDirection.x * distanceX + projectileDirection.z * distanceZ) / distanceH;
                        final double sin = Math.sqrt( 1 - cos * cos );
                        if( width > distanceH * sin ) {
                            tryDodgeProjectile( (PathfinderMob) entity, projectileDirection );
                        }
                    }
                }
            }
        }
    }
    
    /** Alerts the entity's projectile dodge AI, if it has one, that a projectile has been fired at the entity. */
    private static void tryDodgeProjectile( PathfinderMob entity, Vec3 projectileDirection ) {
        for( WrappedGoal task : new ArrayList<>( entity.goalSelector.getAvailableGoals() ) ) {
            if( task.getGoal() instanceof DodgeProjectilesGoal ) {
                ((DodgeProjectilesGoal) task.getGoal()).setDodgeTarget( projectileDirection );
            }
        }
    }
    
    /** The owner of this AI. */
    protected final Mob mob;
    /** The chance that this AI will activate when a projectile is fired at the entity. */
    private final double dodgeChance;
    
    /** The horizontal direction of the projectile being avoided. */
    private Vec3 projectileMotionDirection;
    /** Ticks until the entity gives up. */
    private int giveUpDelay;
    /** Used to prevent mobs from leaping all over the place from multiple projectiles. */
    private int dodgeDelay;
    
    /**
     * @param entity The owner of this AI.
     * @param chance The chance for the entity to dodge projectile fired.
     */
    public DodgeProjectilesGoal(Mob entity, double chance ) {
        mob = entity;
        dodgeChance = chance;
        setFlags( EnumSet.of( Flag.JUMP ) );
    }
    
    /** Tells this AI that a projectile has been fired toward the entity and provides the projectile's facing. */
    private void setDodgeTarget( @Nullable Vec3 projectileDirection ) {
        if( projectileDirection == null ) {
            projectileMotionDirection = null;
            giveUpDelay = 0;
        }
        else if( dodgeDelay <= 0 && mob.getRandom().nextDouble() < dodgeChance ) {
            projectileMotionDirection = projectileDirection;
            giveUpDelay = 10;
        }
    }
    
    /** @return Returns true if this AI can be activated. */
    @Override
    public boolean canUse() {
        return dodgeDelay-- <= 0 && projectileMotionDirection != null && giveUpDelay-- > 0 && mob.onGround() && !mob.isPassenger();
    }
    
    /** Called when this AI is activated. */
    @Override
    public void start() {
        if( projectileMotionDirection != null ) {
            // Calculate a vector perpendicular to the projectile's movement direction
            Vec3 selfAxis = new Vec3( 0.0, 1.0, 0.0 );
            Vec3 dodgeDirection = selfAxis.cross( projectileMotionDirection );
            
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