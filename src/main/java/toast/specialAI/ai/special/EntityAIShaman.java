package toast.specialAI.ai.special;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.math.BlockPos;
import toast.specialAI.ai.AIHandler;

public class EntityAIShaman extends EntityAIBase implements ISpecialAI {
    // The weight of this AI pattern.
    private int WEIGHT;

    // The owner of this AI.
    protected EntityLiving theEntity;
    // The amount healed per pulse.
    private float healAmount;
    // List of custom potion effects, null if none.
    private NBTTagList potionEffects;

    // The entity this mob is following.
    private EntityLiving followTarget;
    // Ticks until next attack.
    private int healTime;

    public EntityAIShaman() { }

    private EntityAIShaman(EntityLiving entity, float healAmount, NBTTagList effects) {
        this.theEntity = entity;
        this.healAmount = healAmount;
        this.potionEffects = effects;
        this.setMutexBits(AIHandler.BIT_MOVEMENT | AIHandler.BIT_FACING);
    }

    // Returns the string name of this AI for use in Properties.
    @Override
    public String getName() {
        return "shaman";
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
        NBTTagList effects;
        if (aiTag.hasKey(this.getName() + "FX")) {
            effects = aiTag.getTagList(this.getName() + "FX", new NBTTagCompound().getId());
        }
        else {
            effects = null;
        }
        entity.tasks.addTask(0, new EntityAIShaman(entity, aiTag.getFloat(this.getName()), effects));
    }

    // Saves this AI to the tag with its default value.
    @Override
    public void save(NBTTagCompound aiTag) {
        aiTag.setFloat(this.getName(), 1.0F);
    }

    // Returns true if a copy of this AI is saved to the tag.
    @Override
    public boolean isSaved(NBTTagCompound aiTag) {
        return aiTag.getFloat(this.getName()) > 0.0F;
    }

    // Initializes any one-time effects on the entity.
    @Override
    public void initialize(EntityLiving entity) {
        entity.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, new ItemStack(Items.BONE));
        entity.setItemStackToSlot(EntityEquipmentSlot.HEAD, new ItemStack(Blocks.LIT_PUMPKIN));

        entity.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).applyModifier(new AttributeModifier(UUID.randomUUID(), "Shaman health boost", 10.0, 0));
    }

    // Returns whether the AI should begin execution.
    @Override
    public boolean shouldExecute() {
        return this.findFollowTarget();
    }

    // Returns whether an in-progress EntityAIBase should continue executing
    @Override
    public boolean continueExecuting() {
        EntityLivingBase target = this.theEntity.getAttackTarget();
        return target != null && this.followTarget != null && this.followTarget.isEntityAlive() && (target == this.followTarget.getAttackTarget() || this.findFollowTarget());
    }

    // Called every tick while this AI is executing.
    @Override
    public void updateTask() {
        EntityLivingBase target = this.theEntity.getAttackTarget();
        double distance = this.theEntity.getDistanceSqToEntity(this.followTarget);
        if (distance > 36.0) {
            this.theEntity.getNavigator().tryMoveToEntityLiving(this.followTarget, 1.3);
        }
        else if (distance < 4.0) {
            this.theEntity.getNavigator().clearPathEntity();
        }

        if (this.theEntity.getNavigator().noPath()) {
            this.theEntity.getLookHelper().setLookPositionWithEntity(target, 30.0F, 30.0F);
        }
        else {
            this.theEntity.getLookHelper().setLookPositionWithEntity(this.followTarget, 30.0F, 30.0F);
        }

        this.healTime--;
        if (this.healTime <= 0) {
            this.healTime = 20;
            List list = this.theEntity.worldObj.getEntitiesWithinAABBExcludingEntity(this.theEntity, this.theEntity.getEntityBoundingBox().expand(16.0, 8.0, 16.0));
            Collections.shuffle(list);
            EntityLiving healTarget;
            for (Object entity : list) {
                if (entity instanceof EntityLiving) {
                    healTarget = (EntityLiving) entity;
                    if (target == healTarget.getAttackTarget() && this.theEntity.getEntitySenses().canSee(healTarget)) {
                        healTarget.heal(this.healAmount);
                        healTarget.extinguish();
                        if (this.potionEffects == null) {
                            healTarget.addPotionEffect(new PotionEffect(MobEffects.STRENGTH, 31, 0));
                            healTarget.addPotionEffect(new PotionEffect(MobEffects.RESISTANCE, 31, 1));
                            healTarget.addPotionEffect(new PotionEffect(MobEffects.SPEED, 31, 1));
                        }
                        else {
                            int length = this.potionEffects.tagCount();
                            PotionEffect effect;
                            for (int i = 0; i < length; i++) {
                                effect = PotionEffect.readCustomPotionEffectFromNBT(this.potionEffects.getCompoundTagAt(i));
                                if (effect != null) {
                                    healTarget.addPotionEffect(effect);
                                }
                            }
                        }
                        this.theEntity.worldObj.playEvent(2005, new BlockPos(healTarget), 0);
                    }
                }
            }
        }
    }

    // Searches for a nearby ally (targeting the same entity) to follow. Returns true if one is found.
    private boolean findFollowTarget() {
        EntityLivingBase target = this.theEntity.getAttackTarget();
        if (target != null) {
            List list = this.theEntity.worldObj.getEntitiesWithinAABBExcludingEntity(this.theEntity, this.theEntity.getEntityBoundingBox().expand(16.0, 8.0, 16.0));
            Collections.shuffle(list);
            for (Object entity : list) {
                if (entity instanceof EntityLiving && target == ((EntityLiving) entity).getAttackTarget()) {
                    this.followTarget = (EntityLiving) entity;
                    return true;
                }
            }
        }
        return false;
    }
}