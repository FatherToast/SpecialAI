package toast.specialAI.ai.special;

import java.util.List;
import java.util.UUID;

import net.minecraft.enchantment.EnchantmentHelper;
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
import net.minecraft.util.DamageSource;
import toast.specialAI.ai.AIHandler;

public class EntityAICharge extends EntityAIBase implements ISpecialAI {
    // Possible states for this AI.
    private static final byte STATE_END = 0;
    private static final byte STATE_START = 1;
    private static final byte STATE_CHARGE = 2;
    private static final byte STATE_STUN = -1;

    // The weight of this AI pattern.
    private int WEIGHT;

    // The owner of this AI.
    protected EntityLiving theEntity;
    // The knockback multiplier.
    private float knockbackMult;

    // The state the host is in.
    private byte state;
    // Ticks until next attack.
    private byte attackTime;
    // The vector of this mob's current charge.
    private double x, z;
    // The host's step height.
    private float stepHeight;

    public EntityAICharge() {}

    private EntityAICharge(EntityLiving entity, float knockbackMult) {
        this.theEntity = entity;
        this.knockbackMult = knockbackMult;
        this.stepHeight = entity.stepHeight;
        this.setMutexBits(AIHandler.BIT_MOVEMENT | AIHandler.BIT_FACING | AIHandler.BIT_SWIMMING);
    }

    // Returns the string name of this AI for use in Properties.
    @Override
    public String getName() {
        return "charge";
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
        entity.tasks.addTask(0, new EntityAICharge(entity, aiTag.getFloat(this.getName())));
    }

    // Saves this AI to the tag with its default value.
    @Override
    public void save(NBTTagCompound aiTag) {
        aiTag.setFloat(this.getName(), 0.5F);
    }

    // Returns true if a copy of this AI is saved to the tag.
    @Override
    public boolean isSaved(NBTTagCompound aiTag) {
        return aiTag.getFloat(this.getName()) > 0.0F;
    }

    // Initializes any one-time effects on the entity.
    @Override
    public void initialize(EntityLiving entity) {
        ItemStack helmet = new ItemStack(Items.leather_helmet);
        EnchantmentHelper.addRandomEnchantment(entity.getRNG(), helmet, 30);
        helmet.setStackDisplayName("Charger's Helmet");
        Items.leather_helmet.func_82813_b(helmet, 0xffff00); // Dyes the armor if it is leather.
        entity.setCurrentItemOrArmor(4, helmet);

        entity.getEntityAttribute(SharedMonsterAttributes.knockbackResistance).applyModifier(new AttributeModifier(UUID.randomUUID(), "Charger knockback resistance", 1.0, 0));
        entity.getEntityAttribute(SharedMonsterAttributes.maxHealth).applyModifier(new AttributeModifier(UUID.randomUUID(), "Charger health boost", 20.0, 0));
        entity.setHealth(entity.getHealth() + 20.0F);
    }

    // Returns whether the AI should begin execution.
    @Override
    public boolean shouldExecute() {
        if (!this.theEntity.onGround || this.theEntity.ridingEntity != null || this.attackTime-- > 0 || this.theEntity.getRNG().nextInt(10) != 0)
            return false;
        EntityLivingBase target = this.theEntity.getAttackTarget();
        if (target != null) {
            double distance = this.theEntity.getDistanceSqToEntity(target);
            return distance <= 256.0 && distance >= 25.0 && this.theEntity.getEntitySenses().canSee(target);
        }
        return false;
    }

    // Returns whether an in-progress EntityAIBase should continue executing
    @Override
    public boolean continueExecuting() {
        return this.state != EntityAICharge.STATE_END;
    }

    // Determine if this AI task is interruptible by a higher priority task.
    @Override
    public boolean isInterruptible() {
        return false;
    }

    // Called once when the AI begins execution.
    @Override
    public void startExecuting() {
        this.theEntity.getNavigator().clearPathEntity();
        this.attackTime = 30;
        this.state = EntityAICharge.STATE_START;
        this.theEntity.motionY = 0.3;
        this.theEntity.swingItem();
    }

    // Called every tick while this AI is executing.
    @Override
    public void updateTask() {
        EntityLivingBase target = this.theEntity.getAttackTarget();
        this.attackTime--;
        if (this.state == EntityAICharge.STATE_START) {
            if (target == null || !target.isEntityAlive()) {
                this.state = EntityAICharge.STATE_END;
                return;
            }
            this.theEntity.getLookHelper().setLookPositionWithEntity(target, 100.0F, 100.0F);
            if (this.attackTime <= 0) {
                double dX = target.posX - this.theEntity.posX;
                double dZ = target.posZ - this.theEntity.posZ;
                double v = Math.sqrt(dX * dX + dZ * dZ);
                this.x = dX / v + this.theEntity.motionX * 0.2;
                this.z = dZ / v + this.theEntity.motionZ * 0.2;
                if (this.theEntity.stepHeight < 1.0F) {
                    this.theEntity.stepHeight = 1.0F;
                }
                this.theEntity.setSprinting(true);
                this.attackTime = 20;
                this.state = EntityAICharge.STATE_CHARGE;
            }
        }
        else if (this.state == EntityAICharge.STATE_CHARGE) {
            this.theEntity.getLookHelper().setLookPosition(this.theEntity.posX + this.x, this.theEntity.posY + this.theEntity.getEyeHeight(), this.theEntity.posZ + this.z, 100.0F, 100.0F);
            this.theEntity.motionX = this.x;
            this.theEntity.motionZ = this.z;
            boolean hit = false;
            if (target != null) {
                List list = this.theEntity.worldObj.getEntitiesWithinAABBExcludingEntity(this.theEntity, this.theEntity.boundingBox.expand(0.2, 0.2, 0.2));
                for (int i = 0; i < list.size(); i++) {
                    if (target.equals(list.get(i))) {
                        hit = true;
                        break;
                    }
                }
            }
            if (hit && target != null) {
                this.theEntity.attackEntityAsMob(target);
                this.theEntity.swingItem();
                this.theEntity.motionX *= -0.8;
                this.theEntity.motionY = 0.4;
                this.theEntity.motionZ *= -0.8;
                target.motionX += this.x * this.knockbackMult;
                target.motionY = 0.4;
                target.motionZ += this.z * this.knockbackMult;
                if (target instanceof EntityPlayerMP) {
                    try {
                        ((EntityPlayerMP) target).playerNetServerHandler.sendPacket(new S12PacketEntityVelocity(target));
                    }
                    catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
                this.state = EntityAICharge.STATE_END;
            }
            else if (this.theEntity.isCollidedHorizontally) {
                this.theEntity.attackEntityFrom(DamageSource.fallingBlock, 5);
                this.theEntity.motionX *= -0.5;
                this.theEntity.motionY = 0.4;
                this.theEntity.motionZ *= -0.5;
                this.theEntity.stepHeight = this.stepHeight;
                this.theEntity.setSprinting(false);
                this.attackTime = 80;
                this.state = EntityAICharge.STATE_STUN;
            }
            else if (this.attackTime <= 0 || this.theEntity.handleWaterMovement() || this.theEntity.handleLavaMovement()) {
                this.attackTime = 40;
                this.state = EntityAICharge.STATE_END;
            }
        }
        else if (this.state == EntityAICharge.STATE_STUN) {
            this.theEntity.swingItem();
            if (this.attackTime <= 0) {
                this.state = EntityAICharge.STATE_END;
            }
        }
    }

    // Resets the task.
    @Override
    public void resetTask() {
        this.theEntity.stepHeight = this.stepHeight;
        this.theEntity.setSprinting(false);
    }
}