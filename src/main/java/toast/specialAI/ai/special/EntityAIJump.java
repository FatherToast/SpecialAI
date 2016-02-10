package toast.specialAI.ai.special;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import toast.specialAI.ai.AIHandler;

public class EntityAIJump extends EntityAIBase implements ISpecialAI {
    // The weight of this AI pattern.
    private int WEIGHT;

    // The owner of this AI.
    protected EntityLiving theEntity;

    public EntityAIJump() {}

    private EntityAIJump(EntityLiving entity) {
        this.theEntity = entity;
        this.setMutexBits(AIHandler.BIT_MOVEMENT | AIHandler.BIT_SWIMMING);
    }

    // Returns the string name of this AI for use in Properties.
    @Override
    public String getName() {
        return "jump";
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
        entity.tasks.addTask(0, new EntityAIJump(entity));
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
        ItemStack boots = new ItemStack(Items.leather_boots);
        boots.addEnchantment(Enchantment.featherFalling, Enchantment.featherFalling.getMaxLevel());
        boots.setStackDisplayName("Feather Boots");
        Items.leather_boots.func_82813_b(boots, 0x9664b4); /// Dyes the armor if it is leather.
        entity.setCurrentItemOrArmor(1, boots);
    }

    // Returns whether the AI should begin execution.
    @Override
    public boolean shouldExecute() {
        if (!this.theEntity.onGround || this.theEntity.ridingEntity != null || this.theEntity.getRNG().nextInt(10) != 0)
            return false;
        EntityLivingBase target = this.theEntity.getAttackTarget();
        if (target != null) {
            double distance = this.theEntity.getDistanceSqToEntity(target);
            return distance <= 144.0 && distance >= 36.0;
        }
        return false;
    }

    // Called once when the AI begins execution.
    @Override
    public void startExecuting() {
        EntityLivingBase target = this.theEntity.getAttackTarget();
        double dX = target.posX - this.theEntity.posX;
        double dZ = target.posZ - this.theEntity.posZ;
        double dH = Math.sqrt(dX * dX + dZ * dZ);
        this.theEntity.motionX = dX / dH * 1.4 + this.theEntity.motionX * 0.2;
        this.theEntity.motionZ = dZ / dH * 1.4 + this.theEntity.motionZ * 0.2;
        this.theEntity.motionY = 1.0;
    }

    // Called every tick while this AI is executing.
    @Override
    public void updateTask() {
        this.theEntity.fallDistance = 0.0F;
    }

    // Returns whether an in-progress EntityAIBase should continue executing
    @Override
    public boolean continueExecuting() {
        return !this.theEntity.onGround && !this.theEntity.handleWaterMovement() && !this.theEntity.handleLavaMovement();
    }
}