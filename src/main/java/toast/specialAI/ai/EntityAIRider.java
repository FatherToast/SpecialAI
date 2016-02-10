package toast.specialAI.ai;

import java.util.Collections;
import java.util.List;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.EntityAIBase;

public class EntityAIRider extends EntityAIBase {
    // The owner of this AI.
    protected EntityLiving theEntity;

    // True if this mob is small.
    private boolean isSmall;
    // The mob the host wants to mount.
    private EntityLiving target;
    // Ticks until the entity will search for a mount.
    private int checkTime;
    // Ticks until the entity gives up.
    private int giveUpDelay;

    public EntityAIRider(EntityLiving entity, boolean small) {
        this.theEntity = entity;
        this.isSmall = small;
        this.setMutexBits(AIHandler.BIT_MOVEMENT | AIHandler.BIT_FACING);
    }

    // Returns whether the AI should begin execution.
    @Override
    public boolean shouldExecute() {
        if (this.theEntity.ridingEntity == null && ++this.checkTime > 50) {
            this.checkTime = 0;
            return this.findNearbyMount();
        }
        return false;
    }

    // Returns whether an in-progress EntityAIBase should continue executing
    @Override
    public boolean continueExecuting() {
        return this.theEntity.ridingEntity == null && this.target != null && this.target.riddenByEntity == null && this.target.isEntityAlive() && !this.theEntity.getNavigator().noPath();
    }

    // Determine if this AI task is interruptible by a higher priority task.
    @Override
    public boolean isInterruptible() {
        return false;
    }

    // Execute a one shot task or start executing a continuous task.
    @Override
    public void startExecuting() {
        this.theEntity.getNavigator().tryMoveToEntityLiving(this.target, 1.3);
    }

    // Called every tick while this AI is executing.
    @Override
    public void updateTask() {
        this.theEntity.getLookHelper().setLookPositionWithEntity(this.target, 30.0F, 30.0F);

        double range = this.theEntity.width * 2.0F * this.theEntity.width * 2.0F + this.target.width;
        if (this.theEntity.getDistanceSq(this.target.posX, this.target.boundingBox.minY, this.target.posZ) <= range) {
            this.theEntity.mountEntity(this.target);
            this.target = null;
        }
        else if (++this.giveUpDelay > 400) {
            this.theEntity.getNavigator().clearPathEntity();
        }
        else {
            if (this.theEntity.getNavigator().noPath()) {
                this.theEntity.getNavigator().tryMoveToEntityLiving(this.target, 1.3);
            }
        }
    }

    // Resets the task.
    @Override
    public void resetTask() {
        this.theEntity.getNavigator().clearPathEntity();
        this.giveUpDelay = 0;
        this.target = null;
    }

    // Searches for a nearby mount and targets it. Returns true if one is found.
    private boolean findNearbyMount() {
        List list = this.theEntity.worldObj.getEntitiesWithinAABBExcludingEntity(this.theEntity, this.theEntity.boundingBox.expand(16.0, 8.0, 16.0));
        Collections.shuffle(list);
        for (Object entity : list) {
            if (entity instanceof EntityLiving && this.isValidTarget((EntityLiving) entity)) {
                this.target = (EntityLiving) entity;
                return true;
            }
        }
        return false;
    }

    // Returns true if the entity can be mounted by the host.
    private boolean isValidTarget(EntityLiving entity) {
        if (this.isSmall || this.theEntity.isChild())
            return AIHandler.MOUNT_SET_SMALL.contains(entity) || entity.isChild() && AIHandler.MOUNT_SET.contains(entity);
        return !entity.isChild() && AIHandler.MOUNT_SET.contains(entity);
    }
}