package fathertoast.specialai.ai;

import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.item.TNTEntity;
import net.minecraft.entity.monster.CreeperEntity;
import net.minecraft.entity.projectile.DragonFireballEntity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.entity.projectile.WitherSkullEntity;
import net.minecraft.pathfinding.Path;
import net.minecraft.util.math.vector.Vector3d;

import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;

/**
 * This AI is based on the vanilla avoid entity goal, but is modified to cause the entity the run away from
 * other entities that are likely to cause an explosion, such as primed TNT or a swelling creeper.
 *
 * @see net.minecraft.entity.ai.goal.AvoidEntityGoal
 */
public class AvoidExplosionsGoal extends Goal {
    /** Selects things that are likely to explode. */
    private static final Predicate<Entity> IS_EXPLODING_SELECTOR = entity ->
            isExplodingCreeper( entity ) ||
                    entity instanceof TNTEntity ||
                    entity instanceof FireballEntity ||
                    entity instanceof DragonFireballEntity ||
                    entity instanceof WitherSkullEntity;
    
    /** @return Returns true if the entity is a creeper about to explode. */
    private static boolean isExplodingCreeper( Entity entity ) {
        if( entity instanceof CreeperEntity ) {
            CreeperEntity creeper = (CreeperEntity) entity;
            return creeper.getSwellDir() > 0 || creeper.isIgnited();
        }
        return false;
    }
    
    /** The owner of this AI. */
    protected final CreatureEntity mob;
    /** The path speed modifier to use when near the target. */
    protected final double speedModifier;
    
    /** The entity currently being avoided. */
    private Entity entityToAvoid;
    /** The path this AI is attempting. */
    private Path path;
    
    /**
     * @param entity The owner of this AI.
     * @param speed  The speed multiplier to apply when running away from a possible explosion source.
     */
    public AvoidExplosionsGoal( CreatureEntity entity, double speed ) {
        mob = entity;
        speedModifier = speed;
        setFlags( EnumSet.of( Flag.MOVE ) );
    }
    
    /** @return Returns true if this AI can be activated. */
    @Override
    public boolean canUse() {
        List<Entity> nearby = mob.level.getEntities( mob, mob.getBoundingBox().inflate( 9.0, 3.0, 9.0 ), IS_EXPLODING_SELECTOR );
        if( nearby.isEmpty() )
            return false;
        entityToAvoid = getNearest( nearby );
        
        Vector3d target = RandomPositionGenerator.getPosAvoid( mob, 16, 7, entityToAvoid.position() );
        if( target == null )
            return false;
        if( entityToAvoid.distanceToSqr( target.x, target.y, target.z ) < entityToAvoid.distanceToSqr( mob ) )
            return false;
        
        path = mob.getNavigation().createPath( target.x, target.y, target.z, 0 );
        return path != null;
    }
    
    /** @return Returns the nearest entity in the list, or null if the list is empty. */
    private <T extends Entity> T getNearest( List<T> entities ) {
        double nearestDistSqr = Double.POSITIVE_INFINITY;
        T nearest = null;
        for( T entity : entities ) {
            double distSqr = mob.distanceToSqr( entity );
            if( distSqr < nearestDistSqr ) {
                nearestDistSqr = distSqr;
                nearest = entity;
            }
        }
        return nearest;
    }
    
    /** @return Called each update while active and returns true if this AI can remain active. */
    @Override
    public boolean canContinueToUse() {
        return mob.getNavigation().isInProgress() && entityToAvoid.isAlive();
    }
    
    /** @return Returns true if this AI can be interrupted by a higher priority conflicting task. */
    @Override
    public boolean isInterruptable() { return false; }
    
    /** Called when this AI is activated. */
    @Override
    public void start() {
        mob.getNavigation().moveTo( path, speedModifier );
    }
    
    /** Called each tick while this AI is active. */
    @Override
    public void tick() {
        mob.getNavigation().setSpeedModifier( mob.distanceToSqr( entityToAvoid ) < 64.0 ? speedModifier : 1.0 );
    }
    
    /** Called when this AI is deactivated. */
    @Override
    public void stop() {
        entityToAvoid = null;
    }
}