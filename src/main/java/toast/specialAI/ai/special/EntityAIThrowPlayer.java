package toast.specialAI.ai.special;

import java.util.List;
import java.util.UUID;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import toast.specialAI.EffectHelper;
import toast.specialAI.ai.AIHandler;

public class EntityAIThrowPlayer extends EntityAIBase implements ISpecialAI {
    // The weight of this AI pattern.
    private int WEIGHT;

    // The owner of this AI.
    protected EntityLiving theEntity;

    // The mob the host wants to throw its target to.
    private EntityLiving throwTarget;
    // Ticks until next attack.
    private byte attackTime;
    // Ticks until the entity gives up.
    private int giveUpDelay;

    public EntityAIThrowPlayer() {}

    private EntityAIThrowPlayer(EntityLiving entity) {
        this.theEntity = entity;
        this.setMutexBits(AIHandler.BIT_MOVEMENT | AIHandler.BIT_FACING);
    }

    // Returns the string name of this AI for use in Properties.
    @Override
    public String getName() {
        return "throw_player";
    }

    // Gets/sets the weight as defined in Properties.
    @Override
    public int getWeight() {
        return this.WEIGHT;
    }

    @Override
    public void setWeight(int weight) {
        this.WEIGHT = weight;
    }

    // Adds a copy of this AI to the given entity.
    @Override
    public void addTo(EntityLiving entity, NBTTagCompound aiTag) {
        entity.tasks.addTask(0, new EntityAIThrowPlayer(entity));
    }

    // Saves this AI to the tag with its default value.
    @Override
    public void save(NBTTagCompound aiTag) {
        aiTag.setByte(this.getName(), (byte) 1);
    }

    // Returns true if a copy of this AI is saved to the tag.
    @Override
    public boolean isSaved(NBTTagCompound aiTag) {
        return aiTag.getByte(this.getName()) > 0;
    }

    // Initializes any one-time effects on the entity.
    @Override
    public void initialize(EntityLiving entity) {
        ItemStack helmet = new ItemStack(Items.leather_helmet);
        helmet.setStackDisplayName("Helmet of Strength");
        EffectHelper.addModifier(helmet, SharedMonsterAttributes.attackDamage, 1.0, 0);
        Items.leather_helmet.func_82813_b(helmet, 0xff0000); /// Dyes the armor if it is leather.
        entity.setCurrentItemOrArmor(4, helmet);

        entity.getEntityAttribute(SharedMonsterAttributes.knockbackResistance).applyModifier(new AttributeModifier(UUID.randomUUID(), "Thrower knockback resistance", 1.0, 0));
        entity.getEntityAttribute(SharedMonsterAttributes.movementSpeed).applyModifier(new AttributeModifier(UUID.randomUUID(), "Thrower speed boost", 0.3, 1));
    }

    // Returns whether the AI should begin execution.
    @Override
    public boolean shouldExecute() {
        if (this.attackTime > 0) {
            this.attackTime--;
            return false;
        }
        return this.theEntity.getAttackTarget() != null && this.theEntity.getAttackTarget().onGround && this.theEntity.ridingEntity == null && this.theEntity.getRNG().nextInt(20) == 0 && this.findThrowTarget();
    }

    // Called once when the AI begins execution.
    @Override
    public void startExecuting() {
        this.theEntity.getNavigator().tryMoveToEntityLiving(this.theEntity.getAttackTarget(), 1.5);
    }

    // Returns whether an in-progress EntityAIBase should continue executing
    @Override
    public boolean continueExecuting() {
        return this.theEntity.getAttackTarget() != null && this.theEntity.ridingEntity == null && this.throwTarget != null && this.throwTarget.isEntityAlive();
    }

    // Determine if this AI task is interruptible by a higher priority task.
    @Override
    public boolean isInterruptible() {
        return false;
    }

    // Called every tick while this AI is executing.
    @Override
    public void updateTask() {
        if (this.theEntity.riddenByEntity == null) {
            EntityLivingBase target = this.theEntity.getAttackTarget();
            this.theEntity.getLookHelper().setLookPositionWithEntity(target, 30.0F, 30.0F);

            double range = this.theEntity.width * 2.0F * this.theEntity.width * 2.0F + target.width;
            if (this.theEntity.getDistanceSq(target.posX, target.boundingBox.minY, target.posZ) <= range) {
                target.mountEntity(this.theEntity);
                this.theEntity.getNavigator().tryMoveToEntityLiving(this.throwTarget, 1.2);
                this.theEntity.swingItem();
                this.attackTime = 10;
            }
            else {
                if (this.theEntity.getNavigator().noPath()) {
                    this.theEntity.getNavigator().tryMoveToEntityLiving(target, 1.5);
                }
            }
        }
        else {
            this.theEntity.getLookHelper().setLookPositionWithEntity(this.throwTarget, 30.0F, 30.0F);

            if (this.attackTime > 0) {
                this.attackTime--;
            }
            else if (this.theEntity.getRNG().nextInt(10) == 0 && this.theEntity.getDistanceSqToEntity(this.throwTarget) <= 100.0) {
                double dX = this.throwTarget.posX - this.theEntity.posX;
                double dZ = this.throwTarget.posZ - this.theEntity.posZ;
                double dH = Math.sqrt(dX * dX + dZ * dZ);
                Entity entity = this.theEntity.riddenByEntity;
                entity.mountEntity((Entity) null);
                entity.motionX = dX / dH + this.theEntity.motionX * 0.2;
                entity.motionZ = dZ / dH + this.theEntity.motionZ * 0.2;
                entity.motionY = 0.4;
                entity.onGround = false;
                entity.fallDistance = 0.0F;
                if (entity instanceof EntityPlayerMP) {
                    try {
                        ((EntityPlayerMP) entity).playerNetServerHandler.sendPacket(new S12PacketEntityVelocity(entity));
                    }
                    catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
                this.theEntity.getNavigator().clearPathEntity();
                this.theEntity.swingItem();
                this.attackTime = 80;
            }
            else {
                if (this.theEntity.getNavigator().noPath()) {
                    this.theEntity.getNavigator().tryMoveToEntityLiving(this.throwTarget, 1.2);
                }
            }
        }
        if (++this.giveUpDelay > 400) {
            this.theEntity.getNavigator().clearPathEntity();
            this.throwTarget = null;
        }
    }

    // Resets the task.
    @Override
    public void resetTask() {
        if (this.theEntity.riddenByEntity != null) {
            this.theEntity.riddenByEntity.mountEntity((Entity) null);
        }
        this.theEntity.getNavigator().clearPathEntity();
        this.giveUpDelay = 0;
        this.throwTarget = null;
    }

    // Searches for a nearby mount and targets it. Returns true if one is found.
    private boolean findThrowTarget() {
        EntityLivingBase target = this.theEntity.getAttackTarget();
        List list = this.theEntity.worldObj.getEntitiesWithinAABBExcludingEntity(this.theEntity, target.boundingBox.expand(16.0, 8.0, 16.0));
        EntityLiving entity;
        int mostNearby = -1;
        int nearby;
        for (Object obj : list) {
            if (obj instanceof EntityLiving) {
                entity = (EntityLiving) obj;
                if (target == entity.getAttackTarget()) {
                    if (entity.getDistanceSqToEntity(target) <= 9.0) {
                        this.throwTarget = null;
                        return false;
                    }
                    nearby = entity.worldObj.getEntitiesWithinAABBExcludingEntity(entity, entity.boundingBox.expand(4.0, 1.0, 4.0)).size();
                    if (nearby > mostNearby) {
                        mostNearby = nearby;
                        this.throwTarget = entity;
                    }
                }
            }
        }
        return this.throwTarget != null;
    }
}