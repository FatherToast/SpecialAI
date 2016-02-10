package toast.specialAI.ai;

import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAITarget;
import net.minecraft.entity.passive.EntityChicken;

public class EntityAIRiderTarget extends EntityAITarget {
    // The attack AI that should be put on the entity while active.
    private EntityAIAttackOnCollidePassive attackAI;

    public EntityAIRiderTarget(EntityCreature entity, boolean hasAttack) {
        super(entity, false, false);
        this.setMutexBits(AIHandler.TARGET_BIT);
        if (!hasAttack) {
            this.attackAI = new EntityAIAttackOnCollidePassive(entity, entity instanceof EntityChicken ? 1.6 : 1.4, false);
        }
    }

    // Returns whether the EntityAIBase should begin execution.
    @Override
    public boolean shouldExecute() {
        return this.taskOwner.riddenByEntity instanceof EntityLiving && this.taskOwner.riddenByEntity.isEntityAlive() && ((EntityLiving) this.taskOwner.riddenByEntity).getAttackTarget() != null;
    }

    // Returns whether an in-progress EntityAIBase should continue executing.
    @Override
    public boolean continueExecuting() {
        return this.shouldExecute();
    }

    // Execute a one shot task or start executing a continuous task.
    @Override
    public void startExecuting() {
        this.taskOwner.setAttackTarget( ((EntityLiving) this.taskOwner.riddenByEntity).getAttackTarget());
        if (this.attackAI != null) {
            this.taskOwner.tasks.addTask(0, this.attackAI);
        }
        super.startExecuting();
    }

    // Updates the task.
    @Override
    public void updateTask() {
        EntityLivingBase target = ((EntityLiving) this.taskOwner.riddenByEntity).getAttackTarget();
        if (this.taskOwner.getAttackTarget() != target) {
            this.taskOwner.setAttackTarget(target);
        }
    }

    // Resets the task.
    @Override
    public void resetTask() {
        if (this.attackAI != null) {
            this.taskOwner.tasks.removeTask(this.attackAI);
        }
        super.resetTask();
    }
}