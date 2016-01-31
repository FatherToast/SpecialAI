package toast.specialAI.ai;

import java.util.List;

import net.minecraft.command.IEntitySelector;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.projectile.EntityLargeFireball;
import net.minecraft.entity.projectile.EntityWitherSkull;
import net.minecraft.pathfinding.PathEntity;
import net.minecraft.util.Vec3;

public class EntityAIAvoidExplosions extends EntityAIBase {
    // The entity selector for explosion avoidance.
    private static final IEntitySelector entitySelector = new EntitySelectorExploding();

    // The owner of this AI.
    protected EntityCreature theEntity;

    // The entity currently being avoided.
    private Entity runFromEntity;
    // The path this AI is attempting.
    private PathEntity pathEntity;

    public EntityAIAvoidExplosions(EntityCreature entity) {
        this.theEntity = entity;
        this.setMutexBits(3);
    }

    // Returns whether the AI should begin execution.
    @Override
    public boolean shouldExecute() {
        List nearby = this.theEntity.worldObj.getEntitiesWithinAABBExcludingEntity(this.theEntity, this.theEntity.boundingBox.expand(9.0, 3.0, 9.0), EntityAIAvoidExplosions.entitySelector);
        if (nearby.isEmpty())
            return false;
        this.runFromEntity = (Entity)nearby.get(0);

        Vec3 target = RandomPositionGenerator.findRandomTargetBlockAwayFrom(this.theEntity, 16, 7, Vec3.createVectorHelper(this.runFromEntity.posX, this.runFromEntity.posY, this.runFromEntity.posZ));
        if (target == null)
            return false;
        if (this.runFromEntity.getDistanceSq(target.xCoord, target.yCoord, target.zCoord) < this.runFromEntity.getDistanceSqToEntity(this.theEntity))
            return false;

        this.pathEntity = this.theEntity.getNavigator().getPathToXYZ(target.xCoord, target.yCoord, target.zCoord);
        return this.pathEntity == null ? false : this.pathEntity.isDestinationSame(target);
    }

    // Returns whether an in-progress EntityAIBase should continue executing
    @Override
    public boolean continueExecuting() {
        return !this.theEntity.getNavigator().noPath() && !this.runFromEntity.isDead;
    }

    // Determine if this AI task is interruptible by a higher priority task.
    @Override
    public boolean isInterruptible() {
        return false;
    }

    // Execute a one shot task or start executing a continuous task.
    @Override
    public void startExecuting() {
        this.theEntity.getNavigator().setPath(this.pathEntity, 1.1);
    }

    // Called every tick while this AI is executing.
    @Override
    public void updateTask() {
        if (this.theEntity.getDistanceSqToEntity(this.runFromEntity) < 64.0) {
            this.theEntity.getNavigator().setSpeed(1.3);
        }
        else {
            this.theEntity.getNavigator().setSpeed(1.1);
        }
    }

    // Resets the task.
    @Override
    public void resetTask() {
        this.runFromEntity = null;
    }

    // Selects things that are likely to explode.
    protected static class EntitySelectorExploding implements IEntitySelector {
        // Return whether the specified entity is applicable to this filter.
        @Override
        public boolean isEntityApplicable(Entity entity) {
            return entity instanceof EntityCreeper && ((EntityCreeper) entity).getCreeperState() > 0 || entity instanceof EntityTNTPrimed || entity instanceof EntityLargeFireball || entity instanceof EntityWitherSkull;
        }
    }
}