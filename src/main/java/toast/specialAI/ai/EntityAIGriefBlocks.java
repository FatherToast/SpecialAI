package toast.specialAI.ai;

import java.util.HashSet;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFire;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.BlockOre;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.init.Blocks;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import toast.specialAI.Properties;
import toast.specialAI.util.BlockHelper;
import toast.specialAI.util.TargetBlock;

public class EntityAIGriefBlocks extends EntityAIBase {
    // Useful properties for this class.
    private static final boolean NEEDS_TOOL = Properties.getBoolean(Properties.GRIEFING, "requires_tools");
    private static final boolean BREAK_LIGHTS = Properties.getBoolean(Properties.GRIEFING, "break_lights");
    private static final boolean BREAK_SOUND = Properties.getBoolean(Properties.GRIEFING, "break_sound");
    private static final HashSet<TargetBlock> TARGET_BLOCKS = BlockHelper.newBlockSet(Properties.getString(Properties.GRIEFING, "target_blocks"));
    private static final HashSet<TargetBlock> BLACKLIST = BlockHelper.newBlockSet(Properties.getString(Properties.GRIEFING, "target_blacklist"));
    private static final boolean LEAVE_DROPS = Properties.getBoolean(Properties.GRIEFING, "leave_drops");
    private static final boolean MAD_CREEPERS = Properties.getBoolean(Properties.GRIEFING, "mad_creepers");
    private static final float REACH = (float) (Properties.getDouble(Properties.GRIEFING, "grief_range") * Properties.getDouble(Properties.GRIEFING, "grief_range"));
    private static final int DELAY = Math.max(1, Properties.getInt(Properties.GRIEFING, "grief_delay"));

    // The owner of this AI.
    protected EntityLiving theEntity;
    // Override for the needs tool config.
    public boolean needsTool;
    // Override for the break lights config.
    public boolean breakLights;
    // Set of blocks that can be targeted by this entity. If null, uses the general config.
    private HashSet<TargetBlock> targetBlocks;
    // Set of blocks that can NOT be targeted by this entity. If null, uses the general config.
    private HashSet<TargetBlock> blacklist;

    // True if this entity can see its target.
    private boolean canSee;
    // Ticks until the entity can check line of sight again.
    private int sightCounter;
    // Ticks until the entity gives up.
    private int giveUpDelay;

    // The coordinates of the block this entity is attacking.
    private int blockX, blockY, blockZ;
    // The block to attack.
    private Block targetBlock;
    // Ticks to count how often to play the "hit" sound.
    private int hitCounter;
    // Current block damage.
    private float blockDamage;

    // The currently running search, if any.
    private GriefSearch search;

    public EntityAIGriefBlocks(EntityLiving entity, byte tool, byte lights, String nbtTargetBlocks, String nbtBlacklist) {
        this.theEntity = entity;
        if (tool >= 0) {
            this.needsTool = tool > 0;
        }
        else {
            this.needsTool = EntityAIGriefBlocks.NEEDS_TOOL;
        }
        if (lights >= 0) {
            this.breakLights = lights > 0;
        }
        else {
            this.breakLights = EntityAIGriefBlocks.BREAK_LIGHTS;
        }
        if (nbtTargetBlocks != null) {
            this.targetBlocks = BlockHelper.newBlockSet(nbtTargetBlocks);
        }
        if (nbtBlacklist != null) {
            this.blacklist = BlockHelper.newBlockSet(nbtBlacklist);
        }
        this.setMutexBits(3);
    }

    // Returns the appropriate target block set.
    public HashSet<TargetBlock> getTargetBlocks() {
        if (this.targetBlocks == null)
            return EntityAIGriefBlocks.TARGET_BLOCKS;
        return this.targetBlocks;
    }

    // Returns the appropriate target block set.
    public HashSet<TargetBlock> getBlacklist() {
        if (this.blacklist == null)
            return EntityAIGriefBlocks.BLACKLIST;
        return this.blacklist;
    }

    // Returns whether the EntityAIBase should begin execution.
    @Override
    public boolean shouldExecute() {
        if (this.theEntity.ridingEntity == null && this.theEntity.worldObj.getGameRules().getGameRuleBooleanValue("mobGriefing")) {
            if (this.targetBlock != null) {
                if (this.theEntity.worldObj.getBlock(this.blockX, this.blockY, this.blockZ) == this.targetBlock)
                    return true;
                this.targetBlock = null;
            }
            if (this.search == null) {
                if (this.theEntity.getRNG().nextInt(EntityAIGriefBlocks.DELAY) == 0) {
                    this.search = SearchHandler.addScanner(new GriefSearch(this, this.theEntity));
                }
            }
            else if (!this.search.isValid()) {
                SearchHandler.removeScanner(this.search);
                this.cancelSearch();
            }
        }
        return false;
    }

    // Returns whether an in-progress EntityAIBase should continue executing.
    @Override
    public boolean continueExecuting() {
        return this.theEntity.ridingEntity == null && (this.blockDamage > 0.0F || this.giveUpDelay < 400) && this.theEntity.worldObj.getBlock(this.blockX, this.blockY, this.blockZ) == this.targetBlock;
    }

    // Determine if this AI task is interruptible by a higher priority task.
    @Override
    public boolean isInterruptible() {
        return !EntityAIGriefBlocks.MAD_CREEPERS || ! (this.theEntity instanceof EntityCreeper) || this.blockDamage == 0.0F;
    }

    // Execute a one shot task or start executing a continuous task.
    @Override
    public void startExecuting() {
        this.canSee = false;
        this.blockDamage = 0.0F;
        this.hitCounter = 0;
        this.theEntity.getNavigator().tryMoveToXYZ(this.blockX, this.blockY, this.blockZ, 1.0);
    }

    // Updates the task.
    @Override
    public void updateTask() {
        super.updateTask();
        this.theEntity.getLookHelper().setLookPosition(this.blockX, this.blockY, this.blockZ, 30.0F, 30.0F);

        if (this.canSee) {
            if (EntityAIGriefBlocks.MAD_CREEPERS && this.theEntity instanceof EntityCreeper) {
                ((EntityCreeper) this.theEntity).setCreeperState(1);
                this.blockDamage = 1.0F;
            }
            else {
                if (this.hitCounter == 0) {
                    this.theEntity.swingItem();
                    this.theEntity.worldObj.playSoundAtEntity(this.theEntity, this.targetBlock.stepSound.getBreakSound(), this.targetBlock.stepSound.getVolume(), this.targetBlock.stepSound.getPitch() * 0.8F);
                }
                if (++this.hitCounter >= 5) {
                    this.hitCounter = 0;
                }
                this.theEntity.getNavigator().clearPathEntity();

                this.blockDamage += BlockHelper.getDamageAmount(this.targetBlock, this.theEntity, this.theEntity.worldObj, this.blockX, this.blockY, this.blockZ);
                if (this.blockDamage >= 1.0F) { /// Block is broken.
                    if (this.targetBlock == Blocks.farmland) {
                        this.theEntity.worldObj.setBlock(this.blockX, this.blockY, this.blockZ, Blocks.dirt, 0, 3);
                    }
                    else {
                        if (EntityAIGriefBlocks.LEAVE_DROPS) {
                            Block block = this.theEntity.worldObj.getBlock(this.blockX, this.blockY, this.blockZ);
                            int meta = this.theEntity.worldObj.getBlockMetadata(this.blockX, this.blockY, this.blockZ);
                            block.dropBlockAsItem(this.theEntity.worldObj, this.blockX, this.blockY, this.blockZ, meta, 0);
                        }
                        this.theEntity.worldObj.setBlockToAir(this.blockX, this.blockY, this.blockZ);
                        if (EntityAIGriefBlocks.BREAK_SOUND) {
                            this.theEntity.worldObj.playAuxSFX(1012, this.blockX, this.blockY, this.blockZ, 0);
                        }
                        this.theEntity.worldObj.playAuxSFX(2001, this.blockX, this.blockY, this.blockZ, Block.getIdFromBlock(this.targetBlock));
                        this.theEntity.swingItem();
                    }
                    this.blockDamage = 0.0F;
                }
                this.theEntity.worldObj.destroyBlockInWorldPartially(this.theEntity.getEntityId(), this.blockX, this.blockY, this.blockZ, (int) (this.blockDamage * 10.0F) - 1);
            }
        }
        else {
            if (this.sightCounter-- <= 0) {
                if (this.checkSight()) {
                    this.sightCounter = 10;
                }
            }
            if (++this.giveUpDelay > 400) {
                this.theEntity.getNavigator().clearPathEntity();
            }
            else {
                if (this.theEntity.getNavigator().noPath()) {
                    this.theEntity.getNavigator().tryMoveToXYZ(this.blockX, this.blockY, this.blockZ, 1.0);
                }
            }
        }
    }

    // Checks line of sight to the target block. Returns true if a ray trace was made.
    private boolean checkSight() {
        double x = this.blockX + 0.5;
        double y = this.blockY + 0.5;
        double z = this.blockZ + 0.5;
        if (this.theEntity.getDistanceSq(x, y - this.theEntity.getEyeHeight(), z) <= EntityAIGriefBlocks.REACH) {
            Vec3 posVec = Vec3.createVectorHelper(this.theEntity.posX, this.theEntity.posY + this.theEntity.getEyeHeight(), this.theEntity.posZ);

            if (this.checkSight(posVec, x, y + (this.theEntity.posY > y ? 0.5 : -0.5), z) || this.checkSight(posVec, x + (this.theEntity.posX > x ? 0.5 : -0.5), y, z) || this.checkSight(posVec, x, y, z + (this.theEntity.posZ > z ? 0.5 : -0.5))) {
                this.canSee = true;
            }
            return true;
        }
        return false;
    }

    // Ray traces to check sight. Returns true if there is an unobstructed view of the coords.
    private boolean checkSight(Vec3 posVec, double x, double y, double z) {
        Vec3 targetVec = Vec3.createVectorHelper(x, y, z);
        MovingObjectPosition target = this.theEntity.worldObj.rayTraceBlocks(posVec, targetVec);
        return target == null || this.blockY == target.blockY && this.blockX == target.blockX && this.blockZ == target.blockZ || this.checkCollided(target.blockX, target.blockY, target.blockZ);
    }

    // Returns true if the obstructing block can be targeted.
    private boolean checkCollided(int x, int y, int z) {
        Block block = this.theEntity.worldObj.getBlock(x, y, z);
        if (this.isValidTarget(block, x, y, z)) {
            this.targetBlock(block, x, y, z);
            return true;
        }
        return false;
    }

    private boolean isValidTarget(Block block, int x, int y, int z) {
        TargetBlock targetBlock = new TargetBlock(block, this.theEntity.worldObj.getBlockMetadata(x, y, z));
        if (block != null && block != Blocks.air && !this.getBlacklist().contains(targetBlock) && (this.breakLights && block.getLightValue() > 1 && ! (block instanceof BlockLiquid) && ! (block instanceof BlockFire) && block != Blocks.lit_redstone_ore && ! (block instanceof BlockOre) || this.getTargetBlocks().contains(targetBlock)))
            return BlockHelper.shouldDamage(block, this.theEntity, this.needsTool, this.theEntity.worldObj, x, y, z);
        return false;
    }

    // Resets the task.
    @Override
    public void resetTask() {
        this.blockDamage = 0.0F;
        this.giveUpDelay = 0;
        this.targetBlock = null;
        this.theEntity.getNavigator().clearPathEntity();
        if (EntityAIGriefBlocks.MAD_CREEPERS && this.theEntity instanceof EntityCreeper) {
            ((EntityCreeper) this.theEntity).setCreeperState(-1);
        }
        else {
            this.theEntity.worldObj.destroyBlockInWorldPartially(this.theEntity.getEntityId(), this.blockX, this.blockY, this.blockZ, -1);
        }
    }

    // Cancels the current search.
    public void cancelSearch() {
        this.search = null;
    }

    // Targets the block at the given position.
    public void targetBlock(Block block, int x, int y, int z) {
        this.blockX = x;
        this.blockY = y;
        this.blockZ = z;
        this.targetBlock = block;
    }
}