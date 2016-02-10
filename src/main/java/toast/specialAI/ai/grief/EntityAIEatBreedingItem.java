package toast.specialAI.ai.grief;

import java.util.Collections;
import java.util.List;

import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import toast.specialAI.Properties;
import toast.specialAI.ai.AIHandler;

public class EntityAIEatBreedingItem extends EntityAIBase {
    // Useful properties for this class.
    public static final boolean EATING_HEALS = Properties.getBoolean(Properties.GENERAL, "eating_heals");

    // The owner of this AI.
    protected EntityAnimal theEntity;

    // The item this entity wants to eat.
    private EntityItem target;
    // Ticks until the entity will search for more food.
    private int checkTime;
    // Ticks until the entity gives up.
    private int giveUpDelay;

    public EntityAIEatBreedingItem(EntityAnimal entity) {
        this.theEntity = entity;
        this.setMutexBits(AIHandler.BIT_MOVEMENT | AIHandler.BIT_FACING);
    }

    // Returns whether the EntityAIBase should begin execution.
    @Override
    public boolean shouldExecute() {
        if (this.theEntity.ridingEntity == null && this.theEntity.worldObj.getGameRules().getGameRuleBooleanValue("mobGriefing") && ++this.checkTime > 30) {
            this.checkTime = 0;
            return this.findNearbyFood();
        }
        return false;
    }

    // Returns whether an in-progress EntityAIBase should continue executing.
    @Override
    public boolean continueExecuting() {
        return this.theEntity.ridingEntity == null && this.giveUpDelay < 400 && this.target != null && this.target.isEntityAlive() && this.theEntity.isBreedingItem(this.target.getEntityItem()) && !this.theEntity.getNavigator().noPath();
    }

    // Execute a one shot task or start executing a continuous task.
    @Override
    public void startExecuting() {
        this.theEntity.getNavigator().tryMoveToXYZ(this.target.posX, this.target.posY, this.target.posZ, 1.25);
    }

    // Updates the task.
    @Override
    public void updateTask() {
        this.theEntity.getLookHelper().setLookPositionWithEntity(this.target, 30.0F, 30.0F);

        List list = this.theEntity.worldObj.getEntitiesWithinAABBExcludingEntity(this.theEntity, this.theEntity.boundingBox.expand(0.2, 0.0, 0.2));
        if (list.contains(this.target)) {
            if (EntityAIEatBreedingItem.EATING_HEALS) {
                ItemStack itemStack = this.target.getEntityItem();
                float healAmount = itemStack.stackSize;
                if (itemStack.getItem() instanceof ItemFood) {
                    healAmount *= ((ItemFood) itemStack.getItem()).func_150905_g(itemStack);
                }
                this.theEntity.heal(healAmount);
            }
            this.theEntity.worldObj.playSoundAtEntity(this.theEntity, "random.burp", 0.5F, this.theEntity.getRNG().nextFloat() * 0.1F + 0.9F);
            this.theEntity.getNavigator().clearPathEntity();
            this.target.setDead();
        }
        else if (++this.giveUpDelay > 400) {
            this.theEntity.getNavigator().clearPathEntity();
        }
        else {
            if (this.theEntity.getNavigator().noPath()) {
                this.theEntity.getNavigator().tryMoveToXYZ(this.target.posX, this.target.posY, this.target.posZ, 1.25);
            }
        }
    }

    // Resets the task.
    @Override
    public void resetTask() {
        this.giveUpDelay = 0;
        this.theEntity.getNavigator().clearPathEntity();
        this.target = null;
    }

    // Searches for a nearby food item and targets it. Returns true if one is found.
    private boolean findNearbyFood() {
        List list = this.theEntity.worldObj.getEntitiesWithinAABBExcludingEntity(this.theEntity, this.theEntity.boundingBox.expand(16.0, 8.0, 16.0));
        Collections.shuffle(list);
        for (Object entity : list) {
            if (entity instanceof EntityItem && this.theEntity.isBreedingItem( ((EntityItem) entity).getEntityItem())) {
                this.target = (EntityItem) entity;
                return true;
            }
        }
        return false;
    }
}