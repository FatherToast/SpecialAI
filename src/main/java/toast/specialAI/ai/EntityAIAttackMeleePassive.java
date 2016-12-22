package toast.specialAI.ai;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAttackMelee;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.passive.EntityRabbit;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemStack;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.MathHelper;

public class EntityAIAttackMeleePassive extends EntityAIAttackMelee {

    // The entity that owns this ai.
    private final EntityCreature attacker;
    // The speed with which the mob will approach the target relative to its normal speed.
    private final double speedTowardsTarget;
    // When true, the mob will continue chasing its target, even if it can't find a path immediately.
    private final boolean longMemory;

    // Ticks until the entity can attack.
    private int attackTick;
    // The attacker's current path.
    private Path entityPath;
    // Ticks until the entity can look for a new path.
    private int pathDelay;
    // Increases with each failed pathing attempt, determines pathDelay.
    private int failedPathPenalty;

    public EntityAIAttackMeleePassive(EntityCreature entity, double speed, boolean mem) {
        super(entity, speed, mem);
        this.attacker = entity;
        this.speedTowardsTarget = speed;
        this.longMemory = mem;
    }

    // Returns whether the EntityAIBase should begin execution.
    @Override
    public boolean shouldExecute() {
        EntityLivingBase target = this.attacker.getAttackTarget();
        if (target == null)
            return false;
        else if (!target.isEntityAlive())
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
        if (this.attacker.getDistanceSq(target.posX, target.getEntityBoundingBox().minY, target.posZ) <= range) {
            if (this.attackTick <= 0) {
                this.attackTick = 20;
                if (this.attacker instanceof EntityMob)
                	((EntityMob) this.attacker).attackEntityAsMob(target);
                else
                	this.attackEntityAsMob(target);
            }
        }
    }

    // External implementation of EntityLivingBase.attackEntityAsMob(Entity) for normally passive mobs. Make sure the attacker has the attack damage attribute registered.
    private boolean attackEntityAsMob(EntityLivingBase target) {
        float damage;
        try {
            damage = (float) this.attacker.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue();
        }
        catch (Exception ex) {
            if (this.attacker instanceof EntitySheep || this.attacker instanceof EntityRabbit) {
                damage = 2.0F;
            }
            else if (this.attacker instanceof EntityChicken) {
                damage = 1.0F;
            }
            else if (this.attacker instanceof EntityCow || this.attacker instanceof EntityHorse) {
                damage = 4.0F;
            }
            else {
                damage = 3.0F;
            }
        }
        damage += EnchantmentHelper.getModifierForCreature(this.attacker.getHeldItemMainhand(), target.getCreatureAttribute());
        int knockback = EnchantmentHelper.getKnockbackModifier(this.attacker);

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

            if (target instanceof EntityPlayer) {
                EntityPlayer entityplayer = (EntityPlayer) target;
                ItemStack weapon = this.attacker.getHeldItemMainhand();
                ItemStack blocking = entityplayer.isHandActive() ? entityplayer.getActiveItemStack() : null;
                if (weapon != null && blocking != null && weapon.getItem() instanceof ItemAxe && blocking.getItem() == Items.SHIELD) {
                    float shieldBreakChance = 0.25F + EnchantmentHelper.getEfficiencyModifier(this.attacker) * 0.05F;
                    if (this.attacker.getRNG().nextFloat() < shieldBreakChance) {
                        entityplayer.getCooldownTracker().setCooldown(Items.SHIELD, 100);
                        this.attacker.worldObj.setEntityState(entityplayer, (byte) 30);
                    }
                }
            }

            EnchantmentHelper.applyThornEnchantments(target, this.attacker); // Triggers hit entity's enchants.
            EnchantmentHelper.applyArthropodEnchantments(this.attacker, target); // Triggers attacker's enchants.
            return true;
        }
        return false;
    }
}