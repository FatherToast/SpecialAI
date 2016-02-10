package toast.specialAI.ai;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAttackOnCollide;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.pathfinding.PathEntity;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;

public class EntityAIAttackOnCollidePassive extends EntityAIAttackOnCollide {
    // The entity that owns this ai.
    private final EntityCreature attacker;
    // The entity's normal avoid water status.
    private final boolean avoidsWater;
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

    public EntityAIAttackOnCollidePassive(EntityCreature entity, double speed, boolean mem) {
        this(entity, (Class) null, speed, mem);
    }

    public EntityAIAttackOnCollidePassive(EntityCreature entity, Class targetType, double speed, boolean mem) {
        super(entity, targetType, speed, mem);
        this.attacker = entity;
        this.avoidsWater = entity.getNavigator().getAvoidsWater();
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
        this.attacker.getNavigator().setAvoidsWater(false);
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
                this.attackEntityAsMob(target);
            }
        }
    }

    // Resets the task.
    @Override
    public void resetTask() {
        super.resetTask();
        this.attacker.getNavigator().setAvoidsWater(this.avoidsWater);
    }

    // External implementation of EntityLivingBase.attackEntityAsMob(Entity) for normally passive mobs. Make sure the attacker has the attack damage attribute registered.
    private boolean attackEntityAsMob(EntityLivingBase target) {
        float damage;
        try {
            damage = (float) this.attacker.getEntityAttribute(SharedMonsterAttributes.attackDamage).getAttributeValue();
        }
        catch (Exception ex) {
            if (this.attacker instanceof EntitySheep) {
                damage = 2.0F;
            }
            else if (this.attacker instanceof EntityChicken) {
                damage = 1.0F;
            }
            else if (this.attacker instanceof EntityCow) {
                damage = 4.0F;
            }
            else {
                damage = 3.0F;
            }
        }
        damage += EnchantmentHelper.getEnchantmentModifierLiving(this.attacker, target);
        int knockback = EnchantmentHelper.getKnockbackModifier(this.attacker, target);

        if (target.attackEntityFrom(DamageSource.causeMobDamage(this.attacker), damage)) {
            if (knockback > 0) {
                target.addVelocity(-MathHelper.sin(this.attacker.rotationYaw * (float) Math.PI / 180.0F) * knockback * 0.5F, 0.1, MathHelper.cos(this.attacker.rotationYaw * (float) Math.PI / 180.0F) * knockback * 0.5F);
                this.attacker.motionX *= 0.6;
                this.attacker.motionZ *= 0.6;
            }

            int fire = EnchantmentHelper.getFireAspectModifier(this.attacker) << 2;
            if (this.attacker.isBurning()) {
                fire += 2;
            }
            if (fire > 0) {
                target.setFire(fire);
            }

            EnchantmentHelper.func_151384_a(target, this.attacker); // Triggers hit entity's enchants.
            EnchantmentHelper.func_151385_b(this.attacker, target); // Triggers attacker's enchants.
            return true;
        }
        return false;
    }
}