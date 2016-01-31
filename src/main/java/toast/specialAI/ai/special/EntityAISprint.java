package toast.specialAI.ai.special;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import toast.specialAI.EffectHelper;

public class EntityAISprint extends EntityAIBase implements ISpecialAI {
    // The weight of this AI pattern.
    private int WEIGHT;

    // The owner of this AI.
    protected EntityLiving theEntity;
    // The sprinting path speed.
    private float speedMult;

    // Ticks until the entity gives up.
    private int giveUpDelay;

    public EntityAISprint() {}

    private EntityAISprint(EntityLiving entity, float speedMult) {
        this.theEntity = entity;
        this.speedMult = speedMult + 1.0F;
        this.setMutexBits(3);
    }

    // Returns the string name of this AI for use in Properties.
    @Override
    public String getName() {
        return "sprint";
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
        entity.tasks.addTask(0, new EntityAISprint(entity, aiTag.getFloat(this.getName())));
    }

    // Saves this AI to the tag with its default value.
    @Override
    public void save(NBTTagCompound aiTag) {
        aiTag.setFloat(this.getName(), 0.7F);
    }

    // Returns true if a copy of this AI is saved to the tag.
    @Override
    public boolean isSaved(NBTTagCompound aiTag) {
        return aiTag.getFloat(this.getName()) > 0.0F;
    }

    // Initializes any one-time effects on the entity.
    @Override
    public void initialize(EntityLiving entity) {
        ItemStack boots = new ItemStack(Items.leather_boots);
        boots.setStackDisplayName("Running Boots");
        EffectHelper.addModifier(boots, SharedMonsterAttributes.movementSpeed, 0.1, 1);
        Items.leather_boots.func_82813_b(boots, 0xff0000); /// Dyes the armor if it is leather.
        entity.setCurrentItemOrArmor(1, boots);
    }

    // Returns whether the AI should begin execution.
    @Override
    public boolean shouldExecute() {
        EntityLivingBase target = this.theEntity.getAttackTarget();
        if (target == null || this.theEntity.ridingEntity != null || !target.isSprinting() && this.theEntity.getRNG().nextInt(20) != 0)
            return false;
        return this.theEntity.getDistanceSqToEntity(target) > 144.0;
    }

    // Returns whether an in-progress EntityAIBase should continue executing
    @Override
    public boolean continueExecuting() {
        EntityLivingBase target = this.theEntity.getAttackTarget();
        if (!this.theEntity.getNavigator().noPath() && target != null && this.theEntity.ridingEntity == null) {
            double distance = this.theEntity.getDistanceSqToEntity(target);
            return distance > 36.0 || distance > 9.0 && this.theEntity.getRNG().nextInt(10) != 0;
        }
        return false;
    }

    // Determine if this AI task is interruptible by a higher priority task.
    @Override
    public boolean isInterruptible() {
        return false;
    }

    // Called once when the AI begins execution.
    @Override
    public void startExecuting() {
        this.theEntity.setSprinting(true);
    }

    // Called every tick while this AI is executing.
    @Override
    public void updateTask() {
        EntityLivingBase target = this.theEntity.getAttackTarget();
        this.theEntity.getLookHelper().setLookPositionWithEntity(target, 30.0F, 30.0F);
        if (++this.giveUpDelay > 400) {
            this.theEntity.getNavigator().clearPathEntity();
        }
        else {
            if (this.theEntity.getNavigator().noPath()) {
                this.theEntity.getNavigator().tryMoveToEntityLiving(target, this.speedMult);
            }
        }
    }

    // Resets the task.
    @Override
    public void resetTask() {
        this.theEntity.setSprinting(false);
        this.giveUpDelay = 0;
    }
}