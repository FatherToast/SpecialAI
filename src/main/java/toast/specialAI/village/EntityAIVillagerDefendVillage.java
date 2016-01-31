package toast.specialAI.village;

import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAITarget;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.village.Village;

public class EntityAIVillagerDefendVillage extends EntityAITarget {
    // The saved village object, occasionally updated.
    private Village village;
    // Ticks until the village object should be checked again.
    private int refreshDelay;
    // The currently targeted village aggressor.
    private EntityLivingBase villageAggressor;
    // The username of the player being targeted. Null if target is not a player.
    private String aggressorName;

    public EntityAIVillagerDefendVillage(EntityCreature entity) {
        super(entity, false, true);
        this.setMutexBits(1);
    }

    // Returns whether the EntityAIBase should begin execution.
    @Override
    public boolean shouldExecute() {
        // Call implementation and play sound if this should execute
        if (this.shouldExecuteImpl()) {
            if (this.taskOwner instanceof EntityVillager) {
                this.taskOwner.worldObj.playSoundAtEntity(this.taskOwner, "mob.villager.no", 1.0F, 1.0F / (this.taskOwner.getRNG().nextFloat() * 0.4F + 0.8F));
            }
            if (this.villageAggressor instanceof EntityPlayer) {
                this.aggressorName = ((EntityPlayer) this.villageAggressor).getCommandSenderName();
            }
            else {
                this.aggressorName = null;
            }
            return true;
        }
        return false;
    }

    // The actual implementation of shouldExecute.
    private boolean shouldExecuteImpl() {
        // Update the saved village object if needed
        if (--this.refreshDelay <= 0) {
            this.refreshDelay = 70 + this.taskOwner.getRNG().nextInt(50);
            this.village = this.taskOwner.worldObj.villageCollectionObj.findNearestVillage(MathHelper.floor_double(this.taskOwner.posX), MathHelper.floor_double(this.taskOwner.posY), MathHelper.floor_double(this.taskOwner.posZ), 32);
        }

        // Check for aggressors in the village
        if (this.village == null)
            return false;
        this.villageAggressor = this.village.findNearestVillageAggressor(this.taskOwner);
        if (!this.isSuitableTarget(this.villageAggressor, false)) {
            if (this.taskOwner.getRNG().nextInt(20) == 0) {
                this.villageAggressor = this.village.func_82685_c(this.taskOwner);
                return this.isSuitableTarget(this.villageAggressor, false);
            }
            return false;
        }
        return true;
    }

    // Execute a one shot task or start executing a continuous task.
    @Override
    public void startExecuting() {
        this.taskOwner.setAttackTarget(this.villageAggressor);
        super.startExecuting();
    }

    // Returns whether an in-progress EntityAIBase should continue executing
    @Override
    public boolean continueExecuting() {
        if (super.continueExecuting()) {
            if (this.aggressorName != null) {
                int reputation = this.village.getReputationForPlayer(this.aggressorName);
                if (reputation > ReputationHandler.getDefendLimit() || reputation > -15 && this.taskOwner.getRNG().nextFloat() < (reputation - ReputationHandler.getDefendLimit() + 10.0F) / 150.0F)
                    return false;
            }
            return true;
        }
        return false;
    }
}