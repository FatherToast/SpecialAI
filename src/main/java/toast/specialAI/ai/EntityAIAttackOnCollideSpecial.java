package toast.specialAI.ai;

import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIAttackOnCollide;
import net.minecraft.pathfinding.PathEntity;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.world.World;

public class EntityAIAttackOnCollideSpecial extends EntityAIAttackOnCollide {
    // The entity that owns this ai.
    private final EntityCreature attacker;
    // The world the entity is in.
    private final World worldObj;
    // The speed with which the mob will approach the target relative to its normal speed.
    private final double speedTowardsTarget;
    // When true, the mob will continue chasing its target, even if it can't find a path immediately.
    private final boolean longMemory;
    // The specific class to target, or null if any.
    private final Class classTarget;

    // Ticks until the entity can attack.
    private int attackTick;
    // The attacker's current path.
    private PathEntity entityPath;
    // Ticks until the entity can look for a new path.
    private int pathDelay;
    // Increases with each failed pathing attempt, determines pathDelay.
    private int failedPathPenalty;

    public EntityAIAttackOnCollideSpecial(EntityCreature entity, double speed, boolean mem) {
        this(entity, (Class) null, speed, mem);
    }

    public EntityAIAttackOnCollideSpecial(EntityCreature entity, Class targetType, double speed, boolean mem) {
        super(entity, targetType, speed, mem);
        this.attacker = entity;
        this.worldObj = entity.worldObj;
        this.speedTowardsTarget = speed;
        this.longMemory = mem;
        this.classTarget = targetType;
    }

    // Returns whether the EntityAIBase should begin execution.
    @Override
    public boolean shouldExecute() {
        EntityLivingBase target = this.attacker.getAttackTarget();
        if (target == null)
            return false;
        else if (!target.isEntityAlive())
            return false;
        else if (this.classTarget != null && !this.classTarget.isAssignableFrom(target.getClass()))
            return false;
        else {
            if (--this.pathDelay <= 0) {
                this.entityPath = this.attacker.getNavigator().getPathToEntityLiving(target);
                this.pathDelay = 4 + this.attacker.getRNG().nextInt(7);
                return this.entityPath != null;
            }
            return true;
        }
    }

    // Execute a one shot task or start executing a continuous task.
    @Override
    public void startExecuting() {
        super.startExecuting();
        this.pathDelay = 0;
    }

    // Updates the task.
    @Override
    public void updateTask() {
        EntityLivingBase target = this.attacker.getAttackTarget();
        this.attacker.getLookHelper().setLookPositionWithEntity(target, 30.0F, 30.0F);

        if ( (this.longMemory || this.attacker.getEntitySenses().canSee(target)) && --this.pathDelay <= 0) {
            this.pathDelay = this.failedPathPenalty + 4 + this.attacker.getRNG().nextInt(7);
            this.attacker.getNavigator().tryMoveToEntityLiving(target, this.speedTowardsTarget);
            if (this.attacker.getNavigator().getPath() != null) {
                PathPoint finalPathPoint = this.attacker.getNavigator().getPath().getFinalPathPoint();
                float minDistance = this.attacker.width + target.width; // Fix for vanilla bug.
                if (finalPathPoint != null && target.getDistanceSq(finalPathPoint.xCoord, finalPathPoint.yCoord, finalPathPoint.zCoord) < minDistance * minDistance) {
                    this.failedPathPenalty = 0;
                }
                else {
                    this.failedPathPenalty += 10;
                }
            }
            else {
                this.failedPathPenalty += 10;
            }
        }

        this.attackTick = Math.max(this.attackTick - 1, 0);
        double range = this.attacker.width * 2.0F * this.attacker.width * 2.0F + target.width;
        if (this.attacker.getDistanceSq(target.posX, target.boundingBox.minY, target.posZ) <= range) {
            if (this.attackTick <= 0) {
                this.attackTick = 20;
                this.attacker.attackEntityAsMob(target);
            }
        }
    }
}