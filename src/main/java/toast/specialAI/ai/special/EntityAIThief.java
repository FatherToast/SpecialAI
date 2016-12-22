package toast.specialAI.ai.special;

import java.util.UUID;

import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAvoidEntity;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import toast.specialAI.EffectHelper;
import toast.specialAI.ai.AIHandler;

public class EntityAIThief extends EntityAIBase implements ISpecialAI {
    // The weight of this AI pattern.
    private int WEIGHT;

    // The owner of this AI.
    protected EntityLiving theEntity;

    // The avoidance AI to be used after an item was stolen.
    private EntityAIAvoidEntity aiAvoid;

    public EntityAIThief() { }

    private EntityAIThief(EntityLiving entity, float avoidRange) {
        this.theEntity = entity;
        if (entity instanceof EntityCreature) {
            this.aiAvoid = new EntityAIAvoidEntity((EntityCreature) entity, EntityPlayer.class, avoidRange, 1.0, 1.2);
        }
        this.setMutexBits(AIHandler.BIT_MOVEMENT | AIHandler.BIT_FACING);
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
        entity.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, null);
        ItemStack helmet = new ItemStack(Items.LEATHER_HELMET);
        helmet.setStackDisplayName("Thief's Cap");
        EffectHelper.addModifier(helmet, SharedMonsterAttributes.MOVEMENT_SPEED, 0.1, 1);
        Items.LEATHER_HELMET.setColor(helmet, 0x102024);
        entity.setItemStackToSlot(EntityEquipmentSlot.HEAD, helmet);

        entity.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).applyModifier(new AttributeModifier(UUID.randomUUID(), "Thief speed boost", 0.2, 1));
        entity.setCanPickUpLoot(false);
    }

    // Returns whether the AI should begin execution.
    @Override
    public boolean shouldExecute() {
        EntityLivingBase target = this.theEntity.getAttackTarget();
        if (target instanceof EntityPlayer && this.theEntity.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND) == null)
            return this.hasItems((EntityPlayer) target);
        if (this.aiAvoid != null && (target == null || target.getHealth() > target.getMaxHealth() / 3.0F)) {
            try {
                return this.aiAvoid.shouldExecute();
            }
            catch (Exception ex) {
                return false;
            }
        }
        return false;
    }

    // Called once when the AI begins execution.
    @Override
    public void startExecuting() {
        EntityLivingBase target = this.theEntity.getAttackTarget();
        if (target != null && this.theEntity.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND) == null) {
            this.theEntity.getNavigator().tryMoveToEntityLiving(target, 1.2);
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
        if (this.theEntity.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND) == null) {
            EntityLivingBase target = this.theEntity.getAttackTarget();
            if (target == null)
                return;
            this.theEntity.getLookHelper().setLookPositionWithEntity(target, 30.0F, 30.0F);

            double range = this.theEntity.width * 2.0F * this.theEntity.width * 2.0F + target.width;
            if (this.theEntity.getDistanceSq(target.posX, target.getEntityBoundingBox().minY, target.posZ) <= range) {
                target.attackEntityFrom(DamageSource.causeMobDamage(this.theEntity), 1.0F);
				if (target instanceof EntityPlayer) {
					ItemStack stolen = this.removeRandomItem((EntityPlayer) target);
					if (stolen != null) {
						EntityItem drop = new EntityItem(this.theEntity.worldObj, this.theEntity.posX, this.theEntity.posY + 0.5, this.theEntity.posZ, stolen);
						drop.setPickupDelay(20);
						drop.getEntityData().setUniqueId("ThiefUUID", this.theEntity.getUniqueID());
						this.theEntity.worldObj.spawnEntityInWorld(drop);
					}
				}
                this.theEntity.addPotionEffect(new PotionEffect(MobEffects.INVISIBILITY, 60, 0));
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
                    player.inventory.setInventorySlotContents(i, null);
                    return item;
                }
            }
        }
        return null;
    }
}