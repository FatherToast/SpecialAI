package toast.specialAI.ai.special;

import java.util.UUID;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import toast.specialAI.ai.AIHandler;

public class EntityAIBarrage extends EntityAIBase implements ISpecialAI {
    // Possible states for this AI.
    private static final byte STATE_END = 0;
    private static final byte STATE_START = 1;
    private static final byte STATE_SHOOT = 2;

    // The weight of this AI pattern.
    private int WEIGHT;

    // The owner of this AI.
    protected EntityLiving theEntity;
    // The arrow damage dealt by this AI.
    private float arrowDamage;

    // The state the host is in.
    private byte state;
    // Ticks until next attack.
    private byte attackTime;
    // The vector of this mob's current attack.
    private double x, y, z;

    public EntityAIBarrage() {}

    private EntityAIBarrage(EntityLiving entity, float arrowDamage) {
        this.theEntity = entity;
        this.arrowDamage = arrowDamage;
        this.setMutexBits(AIHandler.BIT_MOVEMENT | AIHandler.BIT_FACING | AIHandler.BIT_SWIMMING);
    }

    // Returns the string name of this AI for use in Properties.
    @Override
    public String getName() {
        return "barrage";
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
        entity.tasks.addTask(0, new EntityAIBarrage(entity, aiTag.getFloat(this.getName())));
    }

    // Saves this AI to the tag with its default value.
    @Override
    public void save(NBTTagCompound aiTag) {
        aiTag.setFloat(this.getName(), 3.0F);
    }

    // Returns true if a copy of this AI is saved to the tag.
    @Override
    public boolean isSaved(NBTTagCompound aiTag) {
        return aiTag.getFloat(this.getName()) > 0.0F;
    }

    // Initializes any one-time effects on the entity.
    @Override
    public void initialize(EntityLiving entity) {
        entity.setCurrentItemOrArmor(4, new ItemStack(Blocks.dispenser));

        float healthDiff = entity.getMaxHealth() - entity.getHealth();
        entity.getEntityAttribute(SharedMonsterAttributes.maxHealth).applyModifier(new AttributeModifier(UUID.randomUUID(), "Barrager health boost", 20.0, 0));
        entity.setHealth(entity.getMaxHealth() - healthDiff);
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
        return this.state != EntityAIBarrage.STATE_END;
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
        this.state = EntityAIBarrage.STATE_START;
        this.theEntity.swingItem();
        this.theEntity.worldObj.playSoundAtEntity(this.theEntity, "random.click", 1.0F, 1.0F / (this.theEntity.getRNG().nextFloat() * 0.4F + 0.8F));
    }

    // Called every tick while this AI is executing.
    @Override
    public void updateTask() {
        EntityLivingBase target = this.theEntity.getAttackTarget();
        this.attackTime--;
        if (this.state == EntityAIBarrage.STATE_START) {
            if (target == null || !target.isEntityAlive()) {
                this.state = EntityAIBarrage.STATE_END;
                return;
            }
            this.theEntity.getLookHelper().setLookPositionWithEntity(target, 100.0F, 100.0F);
            if (this.attackTime <= 0) {
                this.x = target.posX - this.theEntity.posX;
                this.z = target.posZ - this.theEntity.posZ;
                this.y = target.boundingBox.minY + target.height / 3.0F - this.theEntity.posY - this.theEntity.getEyeHeight() + Math.sqrt(this.x * this.x + this.z * this.z) * 0.2;
                this.attackTime = 60;
                this.state = EntityAIBarrage.STATE_SHOOT;
            }
        }
        else if (this.state == EntityAIBarrage.STATE_SHOOT) {
            this.theEntity.getLookHelper().setLookPosition(this.theEntity.posX + this.x, this.theEntity.posY + this.theEntity.getEyeHeight(), this.theEntity.posZ + this.z, 100.0F, 100.0F);
            if (this.attackTime % 5 == 0) {
                EntityArrow arrow = new EntityArrow(this.theEntity.worldObj, this.theEntity.posX, this.theEntity.posY + this.theEntity.getEyeHeight(), this.theEntity.posZ);
                arrow.shootingEntity = this.theEntity;
                arrow.setDamage(this.arrowDamage + this.theEntity.getRNG().nextGaussian() * 0.5 + this.theEntity.worldObj.difficultySetting.getDifficultyId() * 0.22);
                arrow.setThrowableHeading(this.x, this.y, this.z, 1.8F, 20.0F);
                if (this.theEntity.isBurning()) {
                    arrow.setFire(100);
                }

                this.theEntity.worldObj.playSoundAtEntity(this.theEntity, "random.bow", 1.0F, 1.0F / (this.theEntity.getRNG().nextFloat() * 0.4F + 0.8F));
                this.theEntity.worldObj.spawnEntityInWorld(arrow);
            }
            else if (this.attackTime <= 0) {
                this.state = EntityAIBarrage.STATE_END;
            }
        }
    }

    // Resets the task.
    @Override
    public void resetTask() {
        this.attackTime = 60;
    }
}