package toast.specialAI.ai.special;

import java.util.UUID;

import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAvoidEntity;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import toast.specialAI.EffectHelper;

public class EntityAIThief extends EntityAIBase implements ISpecialAI {
    // The weight of this AI pattern.
    private int WEIGHT;

    // The owner of this AI.
    protected EntityLiving theEntity;

    // The avoidance AI to be used after an item was stolen.
    private EntityAIAvoidEntity aiAvoid;

    public EntityAIThief() {}

    private EntityAIThief(EntityLiving entity, float avoidRange) {
        this.theEntity = entity;
        if (entity instanceof EntityCreature) {
            this.aiAvoid = new EntityAIAvoidEntity((EntityCreature) entity, EntityPlayer.class, avoidRange, 1.0, 1.2);
        }
        this.setMutexBits(3);
    }

    // Returns the string name of this AI for use in Properties.
    @Override
    public String getName() {
        return "thief";
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
        entity.tasks.addTask(0, new EntityAIThief(entity, aiTag.getFloat(this.getName())));
    }

    // Saves this AI to the tag with its default value.
    @Override
    public void save(NBTTagCompound aiTag) {
        aiTag.setFloat(this.getName(), 16.0F);
    }

    // Returns true if a copy of this AI is saved to the tag.
    @Override
    public boolean isSaved(NBTTagCompound aiTag) {
        return aiTag.getFloat(this.getName()) > 0.0F;
    }

    // Initializes any one-time effects on the entity.
    @Override
    public void initialize(EntityLiving entity) {
        entity.setCurrentItemOrArmor(0, (ItemStack) null);
        ItemStack helmet = new ItemStack(Items.leather_helmet);
        helmet.setStackDisplayName("Thief's Cap");
        EffectHelper.addModifier(helmet, SharedMonsterAttributes.movementSpeed, 0.1, 1);
        Items.leather_helmet.func_82813_b(helmet, 0x102024); // Dyes the armor if it is leather.
        entity.setCurrentItemOrArmor(4, helmet);

        entity.getEntityAttribute(SharedMonsterAttributes.movementSpeed).applyModifier(new AttributeModifier(UUID.randomUUID(), "Thief speed boost", 0.2, 1));
        entity.setCanPickUpLoot(false);
    }

    // Returns whether the AI should begin execution.
    @Override
    public boolean shouldExecute() {
        EntityLivingBase target = this.theEntity.getAttackTarget();
        if (target == null || target instanceof EntityPlayer && target.getHealth() > target.getMaxHealth() / 2.0F) {
            if (this.theEntity.getEquipmentInSlot(0) == null)
                return target != null && this.hasItems((EntityPlayer) target);
            if (this.aiAvoid != null) {
                try {
                    return this.aiAvoid.shouldExecute();
                }
                catch (Exception ex) {
                    return false;
                }
            }
        }
        return false;
    }

    // Called once when the AI begins execution.
    @Override
    public void startExecuting() {
        if (this.theEntity.getEquipmentInSlot(0) == null) {
            this.theEntity.getNavigator().tryMoveToEntityLiving(this.theEntity.getAttackTarget(), 1.2);
        }
        else if (this.aiAvoid != null) {
            try {
                this.aiAvoid.startExecuting();
            }
            catch (Exception ex) {
                this.theEntity.getNavigator().clearPathEntity();
            }
        }
    }

    // Returns whether an in-progress EntityAIBase should continue executing
    @Override
    public boolean continueExecuting() {
        return !this.theEntity.getNavigator().noPath();
    }

    // Called every tick while this AI is executing.
    @Override
    public void updateTask() {
        if (this.theEntity.getEquipmentInSlot(0) == null) {
            EntityLivingBase target = this.theEntity.getAttackTarget();
            if (target == null)
                return;
            this.theEntity.getLookHelper().setLookPositionWithEntity(target, 30.0F, 30.0F);

            double range = this.theEntity.width * 2.0F * this.theEntity.width * 2.0F + target.width;
            if (this.theEntity.getDistanceSq(target.posX, target.boundingBox.minY, target.posZ) <= range) {
                target.attackEntityFrom(DamageSource.causeMobDamage(this.theEntity), 1.0F);
				if (target instanceof EntityPlayer) {
					this.theEntity.setCurrentItemOrArmor(0, this.removeRandomItem((EntityPlayer) target));
				}
                this.theEntity.setEquipmentDropChance(0, 2.0F);
                this.theEntity.func_110163_bv(); // Marks the entity to never despawn.
                this.theEntity.addPotionEffect(new PotionEffect(Potion.invisibility.id, 60, 0));
                this.theEntity.getNavigator().clearPathEntity();
            }
            else {
                if (this.theEntity.getNavigator().noPath()) {
                    this.theEntity.getNavigator().tryMoveToEntityLiving(target, 1.2);
                }
            }
        }
        else if (this.aiAvoid != null) {
            try {
                this.aiAvoid.updateTask();
            }
            catch (Exception ex) {
                this.theEntity.getNavigator().clearPathEntity();
            }
        }
    }

    // Resets the task
    @Override
    public void resetTask() {
        if (this.aiAvoid != null) {
            try {
                this.aiAvoid.resetTask();
            }
            catch (Exception ex) {
                // Do nothing
            }
        }
    }

    // Returns true if the player has any items in their inventory.
    private boolean hasItems(EntityPlayer player) {
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            if (player.inventory.getStackInSlot(i) != null)
                return true;
        }
        return false;
    }

    // Removes a random item stack from the player's inventory and returns it.
    private ItemStack removeRandomItem(EntityPlayer player) {
        int count = 0;
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            if (player.inventory.getStackInSlot(i) != null) {
                count++;
            }
        }
        if (count > 0) {
            count = this.theEntity.getRNG().nextInt(count);
            ItemStack item;
            for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
                item = player.inventory.getStackInSlot(i);
                if (item != null && --count < 0) {
                    player.inventory.setInventorySlotContents(i, (ItemStack) null);
                    return item;
                }
            }
        }
        return null;
    }
}