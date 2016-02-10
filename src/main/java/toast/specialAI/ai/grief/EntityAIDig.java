package toast.specialAI.ai.grief;

import java.util.ArrayList;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.pathfinding.PathEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import toast.specialAI._SpecialAI;
import toast.specialAI.util.BlockHelper;

public class EntityAIDig extends EntityAIBase {
    // Useful properties for this class.
    private static final boolean PLAYER_ONLY = false;
    private static final boolean LEAVE_DROPS = false;
    private static final boolean NEEDS_TOOL = false;

    // The entity that owns this AI.
    protected EntityLiving theEntity;
    // The entity's starting position.
    private double startX, startY, startZ;
    // The coordinates of the block this entity is attacking.
    private int blockX, blockY, blockZ;
    // The id of the block to attack.
    private Block targetBlock;
    // Current block damage.
    private float blockDamage;

    public EntityAIDig(EntityLiving entity) {
        this.theEntity = entity;
    }

    // Returns the target of the mob.
    public EntityLivingBase target() {
        return this.theEntity.getAttackTarget();
    }

    // Returns the target of the mob.
    public Random random() {
        return this.theEntity.getRNG();
    }

    // Returns the target of the mob.
    public double distance() {
        EntityLivingBase target = this.target();
        return target == null ? Double.POSITIVE_INFINITY : this.theEntity.getDistanceSqToEntity(target);
    }

    // Returns whether the EntityAIBase should begin execution.
    @Override
    public boolean shouldExecute() {
        EntityLivingBase target = this.target();
        if (this.theEntity.worldObj.getGameRules().getGameRuleBooleanValue("mobGriefing") && target != null && (!EntityAIDig.PLAYER_ONLY || target instanceof EntityPlayer))
            return this.targetHighestObstruction(target);
        return false;
    }

    // Returns whether an in-progress EntityAIBase should continue executing.
    @Override
    public boolean continueExecuting() {
        return this.theEntity.worldObj.getBlock(this.blockX, this.blockY, this.blockZ) == this.targetBlock && this.theEntity.getDistanceSq(this.startX, this.startY, this.startZ) <= 4.0;
    }

    // Execute a one shot task or start executing a continuous task.
    @Override
    public void startExecuting() {
        _SpecialAI.console("Starting!");
        this.startX = this.theEntity.posX;
        this.startY = this.theEntity.posY;
        this.startZ = this.theEntity.posZ;
        this.blockDamage = 0.0F;
    }

    // Updates the task.
    @Override
    public void updateTask() {
        this.theEntity.getLookHelper().setLookPosition(this.blockX, this.blockY, this.blockZ, 30.0F, 30.0F);
        if (this.random().nextInt(20) == 0) {
            this.theEntity.worldObj.playAuxSFX(1010, this.blockX, this.blockY, this.blockZ, 0);
            this.theEntity.swingItem();
        }

        this.blockDamage += BlockHelper.getDamageAmount(this.targetBlock, this.theEntity, this.theEntity.worldObj, this.blockX, this.blockY, this.blockZ);
        if (this.blockDamage >= 1.0F) {
            this.theEntity.worldObj.setBlockToAir(this.blockX, this.blockY, this.blockZ);
            this.theEntity.worldObj.playAuxSFX(2001, this.blockX, this.blockY, this.blockZ, Block.getIdFromBlock(this.targetBlock));
            this.theEntity.swingItem();
            this.blockDamage = 0.0F;
        }
        this.theEntity.worldObj.destroyBlockInWorldPartially(this.theEntity.getEntityId(), this.blockX, this.blockY, this.blockZ, (int) (this.blockDamage * 10.0F) - 1);
    }

    // Resets the task.
    @Override
    public void resetTask() {
        _SpecialAI.console("Stopping!");
        this.blockDamage = 0.0F;
        this.theEntity.worldObj.destroyBlockInWorldPartially(this.theEntity.getEntityId(), this.blockX, this.blockY, this.blockZ, -1);
    }

    // Returns the first colliding block with the entity. Searches from top to bottom (kinda).
    private boolean targetHighestObstruction(EntityLivingBase target) {
        PathEntity path = this.theEntity.getNavigator().getPath();
        int x, y, z;

        if (path == null || !path.isFinished())
            return false;
        _SpecialAI.console("Blocked!");

        AxisAlignedBB boundingBox = this.theEntity.boundingBox;

        double dX = target.posX - this.theEntity.posX;
        double dY = target.posY - this.theEntity.posY;
        double dZ = target.posZ - this.theEntity.posZ;
        double v = Math.sqrt(dX * dX + dY * dY + dZ * dZ);
        boundingBox = boundingBox.addCoord(dX / v, dY / v, dZ / v);

        ArrayList<AxisAlignedBB> bbList = new ArrayList<AxisAlignedBB>();
        int minX = MathHelper.floor_double(boundingBox.minX);
        int maxX = MathHelper.floor_double(boundingBox.maxX);
        int minY = MathHelper.floor_double(boundingBox.minY);
        int maxY = MathHelper.floor_double(boundingBox.maxY);
        int minZ = MathHelper.floor_double(boundingBox.minZ);
        int maxZ = MathHelper.floor_double(boundingBox.maxZ);

        if (this.theEntity.worldObj.checkChunksExist(minX, minY, minZ, maxX, maxY, maxZ)) {
            for (y = maxY; y >= minY; y--) {
                for (x = minX; x <= maxX; x++) {
                    for (z = minZ; z <= maxZ; z++) {
                        Block block = this.theEntity.worldObj.getBlock(x, y, z);
                        if (block != null) {
                            block.addCollisionBoxesToList(this.theEntity.worldObj, x, y, z, boundingBox, bbList, this.theEntity);
                            if (!bbList.isEmpty()) {
                                if (this.tryTargetBlock(block, x, y, z))
                                    return true;
                                bbList.clear();
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean tryTargetBlock(Block block, int x, int y, int z) {
        if (block != Blocks.air && BlockHelper.shouldDamage(block, this.theEntity, EntityAIDig.NEEDS_TOOL, this.theEntity.worldObj, x, y, z)) {
            this.blockX = x;
            this.blockY = y;
            this.blockZ = z;
            this.targetBlock = block;
            return true;
        }
        return false;
    }
}