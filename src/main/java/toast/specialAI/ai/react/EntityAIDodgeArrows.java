package toast.specialAI.ai.react;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAITasks.EntityAITaskEntry;
import net.minecraft.util.math.Vec3d;
import toast.specialAI.ai.AIHandler;

public class EntityAIDodgeArrows extends EntityAIBase
{
	public static void doDodgeCheckForArrow(Entity arrow) {
		float width = arrow.width + 0.3F;
		double vH = Math.sqrt(arrow.motionX * arrow.motionX + arrow.motionZ * arrow.motionZ);
		double uX = arrow.motionX / vH;
		double uZ = arrow.motionZ / vH;

		Entity entity;
		int dY;
		double dX, dZ, dH;
		double cos, sin;
		for (int i = 0; i < arrow.worldObj.loadedEntityList.size(); i++) {
			entity = arrow.worldObj.loadedEntityList.get(i);
			if (entity instanceof EntityCreature) {

				dY = (int) entity.posY - (int) arrow.posY;
				if (dY < 0) dY = -dY;
				if (dY <= 16) {
					// Within vertical range
					dX = entity.posX - arrow.posX;
					dZ = entity.posZ - arrow.posZ;
					dH = Math.sqrt(dX * dX + dZ * dZ);
					if (dH < 24.0) {
						// Within horizontal range
						cos = (uX * dX + uZ * dZ) / dH;
						sin = Math.sqrt(1 - cos * cos);
						if (width > dH * sin) {
							// Within ray width
							EntityAIDodgeArrows.tryDodgeArrow((EntityCreature) entity, uX, uZ);
						}
					}
				}
			}
		}
	}
	private static void tryDodgeArrow(EntityCreature entity, double uX, double uZ) {
        for (EntityAITaskEntry entry : entity.tasks.taskEntries.toArray(new EntityAITaskEntry[0])) {
            if (entry.action instanceof EntityAIDodgeArrows) {
            	((EntityAIDodgeArrows) entry.action).setDodgeTarget(new Vec3d(uX, 0.0, uZ));
            }
        }
	}

    // The owner of this AI.
    protected EntityCreature theEntity;
    // The horizontal velocity of the entity being avoided.
    protected float dodgeChance;

    // The horizontal velocity of the entity being avoided.
    private Vec3d arrowUVec;
    // Ticks until the entity gives up.
    private int giveUpDelay;
    // Used to prevent mobs from leaping all over the place from multiple arrows.
    private int dodgeDelay;

    public EntityAIDodgeArrows(EntityCreature entity, float chance) {
    	this.dodgeChance = chance;
        this.theEntity = entity;
        this.setMutexBits(AIHandler.BIT_SWIMMING);
    }

    // Tells this AI to dodge an entity.
    private void setDodgeTarget(Vec3d motionU) {
    	if (motionU == null) {
    		this.arrowUVec = null;
    		this.giveUpDelay = 0;
    	}
    	else if (this.dodgeDelay <= 0 && this.theEntity.getRNG().nextFloat() < this.dodgeChance) {
    		this.arrowUVec = motionU;
    		this.giveUpDelay = 10;
    	}
    }

    // Returns whether the AI should begin execution.
    @Override
    public boolean shouldExecute() {
        return this.dodgeDelay-- <= 0 && this.arrowUVec != null && this.giveUpDelay-- > 0 && this.theEntity.onGround && !this.theEntity.isRiding();// && this.theEntity.getRNG().nextInt(5) == 0;
    }

    // Execute a one shot task or start executing a continuous task.
    @Override
    public void startExecuting() {
    	if (this.arrowUVec != null) {
	        Vec3d selfUVec = new Vec3d(0.0, 1.0, 0.0);
	        Vec3d dodgeUVec = selfUVec.crossProduct(this.arrowUVec);

	        double scale = 0.8;
	        if (this.theEntity.getRNG().nextBoolean())
	        	scale = -scale;

	        this.theEntity.motionX = dodgeUVec.xCoord * scale;
	        this.theEntity.motionZ = dodgeUVec.zCoord * scale;
	        this.theEntity.motionY = 0.4;

	        this.setDodgeTarget(null);
	        this.dodgeDelay = 40;
    	}
    }

    // Returns whether an in-progress EntityAIBase should continue executing
    @Override
    public boolean continueExecuting() {
        return false;
    }
}