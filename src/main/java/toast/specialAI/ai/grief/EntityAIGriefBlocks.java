package toast.specialAI.ai.grief;

import java.util.HashSet;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFire;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.BlockOre;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.WorldServer;
import toast.specialAI.Properties;
import toast.specialAI._SpecialAI;
import toast.specialAI.ai.AIHandler;
import toast.specialAI.util.BlockHelper;
import toast.specialAI.util.TargetBlock;

public class EntityAIGriefBlocks extends EntityAIBase
{
    // Useful properties for this class.
    private static final int SCAN_RANGE = Math.max(0, Properties.getInt(Properties.GRIEFING, "grief_scan_range")) + 1;
    private static final int SCAN_RANGE_VERTICAL = Math.max(0, Properties.getInt(Properties.GRIEFING, "grief_scan_range_vertical")) + 1;

    private static final boolean GRIEF_NEEDS_TOOL = Properties.getBoolean(Properties.GRIEFING, "requires_tools");
    private static final boolean GRIEF_TARGETS_LIGHTS = Properties.getBoolean(Properties.GRIEFING, "break_lights");
    private static final HashSet<TargetBlock> GRIEF_TARGET_BLOCKS = BlockHelper.newBlockSet(Properties.getString(Properties.GRIEFING, "target_blocks"));
    private static final HashSet<TargetBlock> GRIEF_BLACKLIST = BlockHelper.newBlockSet(Properties.getString(Properties.GRIEFING, "target_blacklist"));

    private static final HashSet<TargetBlock> FIDDLE_TARGET_BLOCKS = BlockHelper.newBlockSet(Properties.getString(Properties.GRIEFING, "target_fiddling_blocks"));
    private static final HashSet<TargetBlock> FIDDLE_BLACKLIST = BlockHelper.newBlockSet(Properties.getString(Properties.GRIEFING, "target_fiddling_blacklist"));

    private static final int SCAN_COUNT = Properties.getInt(Properties.GRIEFING, "scan_count");
    private static final boolean BREAK_SOUND = Properties.getBoolean(Properties.GRIEFING, "break_sound");
    private static final boolean LEAVE_DROPS = Properties.getBoolean(Properties.GRIEFING, "leave_drops");
    private static final boolean MAD_CREEPERS = Properties.getBoolean(Properties.GRIEFING, "mad_creepers");
    private static final float REACH = (float) (Properties.getDouble(Properties.GRIEFING, "grief_range") * Properties.getDouble(Properties.GRIEFING, "grief_range"));

    // The owner of this AI.
    protected EntityLiving theEntity;
    // The owner's random number generator.
    protected Random random;

    // Override for the scan range config.
    private int scanRange;
    // Override for the vertical scan range config.
    private int scanRangeY;

    /* Griefing-specific values. These are unused if griefing is false. */
    private final boolean griefing;
    // Override for the needs tool config.
    private boolean griefNeedsTool;
    // Override for the break lights config.
    private boolean griefTargetsLights;
    // Set of blocks that can be griefed by this entity.
    private HashSet<TargetBlock> griefTargetBlocks;
    // Set of blocks that can NOT be griefed by this entity.
    private HashSet<TargetBlock> griefBlacklist;

    /* Fiddling-specific values. These are unused if fiddling is false. */
    private final boolean fiddling;
    // Set of blocks that can be fiddled with by this entity.
    private HashSet<TargetBlock> fiddleTargetBlocks;
    // Set of blocks that can NOT be fiddled with by this entity.
    private HashSet<TargetBlock> fiddleBlacklist;

    private Activity activity = Activity.NONE;

    // True if this entity can see its target.
    private boolean canSee;
    // Ticks until the entity can check line of sight again.
    private int sightCounter;
    // Ticks until the entity gives up.
    private int giveUpDelay;
    // Used to prevent mobs from spamming right click on things.
    private int fiddleDelay;

    // The coordinates of the block this entity is attacking.
    private int blockX, blockY, blockZ;
    // The block to attack.
    private Block targetBlock;
    // Ticks to count how often to play the "hit" sound.
    private int hitCounter;
    // Current block damage.
    private float blockDamage;

    public EntityAIGriefBlocks(EntityLiving entity, boolean griefing, boolean fiddling, NBTTagCompound tag) {
        this.theEntity = entity;
        this.random = entity.getRNG();
        this.griefing = griefing;
        this.fiddling = fiddling;
        this.setMutexBits(AIHandler.BIT_MOVEMENT | AIHandler.BIT_FACING);

        // Load common tags
        if (tag.hasKey(AIHandler.GRIEF_RANGE_TAG)) {
            this.scanRange = tag.getByte(AIHandler.GRIEF_RANGE_TAG);
        }
        else {
        	this.scanRange = EntityAIGriefBlocks.SCAN_RANGE;
        }
        if (tag.hasKey(AIHandler.GRIEF_RANGE_VERTICAL_TAG)) {
        	this.scanRangeY = tag.getByte(AIHandler.GRIEF_RANGE_VERTICAL_TAG);
        }
        else {
        	this.scanRangeY = EntityAIGriefBlocks.SCAN_RANGE_VERTICAL;
        }

        // Load griefing-specific tags
        if (griefing) {
	        if (tag.hasKey(AIHandler.GRIEF_TOOL_TAG)) {
	            this.griefNeedsTool = tag.getBoolean(AIHandler.GRIEF_TOOL_TAG);
	        }
	        else {
	        	this.griefNeedsTool = EntityAIGriefBlocks.GRIEF_NEEDS_TOOL;
	        }
	        if (tag.hasKey(AIHandler.GRIEF_LIGHT_TAG)) {
	            this.griefTargetsLights = tag.getBoolean(AIHandler.GRIEF_LIGHT_TAG);
	        }
	        else {
	        	this.griefTargetsLights = EntityAIGriefBlocks.GRIEF_TARGETS_LIGHTS;
	        }
	        if (tag.hasKey(AIHandler.GRIEF_BLOCK_TAG)) {
	            this.griefTargetBlocks = BlockHelper.newBlockSet(tag.getString(AIHandler.GRIEF_BLOCK_TAG));
	        }
	        else {
	        	this.griefTargetBlocks = EntityAIGriefBlocks.GRIEF_TARGET_BLOCKS;
	        }
	        if (tag.hasKey(AIHandler.GRIEF_EXCEPTION_TAG)) {
	            this.griefBlacklist = BlockHelper.newBlockSet(tag.getString(AIHandler.GRIEF_EXCEPTION_TAG));
	        }
	        else {
	        	this.griefBlacklist = EntityAIGriefBlocks.GRIEF_BLACKLIST;
	        }
        }

        // Load fiddling-specific tags
        if (fiddling) {
	        if (tag.hasKey(AIHandler.FIDDLE_BLOCK_TAG)) {
	            this.fiddleTargetBlocks = BlockHelper.newBlockSet(tag.getString(AIHandler.FIDDLE_BLOCK_TAG));
	        }
	        else {
	        	this.fiddleTargetBlocks = EntityAIGriefBlocks.FIDDLE_TARGET_BLOCKS;
	        }
	        if (tag.hasKey(AIHandler.FIDDLE_EXCEPTION_TAG)) {
	            this.fiddleBlacklist = BlockHelper.newBlockSet(tag.getString(AIHandler.FIDDLE_EXCEPTION_TAG));
	        }
	        else {
	        	this.fiddleBlacklist = EntityAIGriefBlocks.FIDDLE_BLACKLIST;
	        }
        }
    }

    // Returns whether the EntityAIBase should begin execution.
    @Override
    public boolean shouldExecute() {
        if (this.theEntity.ridingEntity == null && this.theEntity.worldObj.getGameRules().getGameRuleBooleanValue("mobGriefing")) {
        	// Try picking random blocks
        	this.fiddleDelay--;
            int X = (int) Math.floor(this.theEntity.posX);
            int Y = (int) Math.floor(this.theEntity.posY);
            int Z = (int) Math.floor(this.theEntity.posZ);
            int x, y, z;
        	for (int i = 0; i < EntityAIGriefBlocks.SCAN_COUNT && AIHandler.canScan(); i++) {
        		x = X + this.random.nextInt(this.scanRange) - this.random.nextInt(this.scanRange);
        		y = Y + this.random.nextInt(this.scanRangeY) - this.random.nextInt(this.scanRangeY);
        		z = Z + this.random.nextInt(this.scanRange) - this.random.nextInt(this.scanRange);
        		if (this.tryTargetBlock(x, y, z))
					return true;
        	}
        }
        return false;
    }

    // Returns whether an in-progress EntityAIBase should continue executing.
    @Override
    public boolean continueExecuting() {
    	switch (this.activity) {
			case GRIEFING:
				return this.continueExecutingGriefing();
			case FIDDLING:
				return this.continueExecutingFiddling();
			default:
				return false;
    	}
    }
    private boolean continueExecutingGriefing() {
        return this.theEntity.ridingEntity == null && (this.blockDamage > 0.0F || this.giveUpDelay < 400) && this.theEntity.worldObj.getBlock(this.blockX, this.blockY, this.blockZ) == this.targetBlock;
    }
    private boolean continueExecutingFiddling() {
        return this.theEntity.ridingEntity == null && this.giveUpDelay < 400 && this.theEntity.worldObj.getBlock(this.blockX, this.blockY, this.blockZ) == this.targetBlock;
    }

    // Determine if this AI task is interruptible by a higher priority task.
    @Override
    public boolean isInterruptible() {
        return Activity.GRIEFING.equals(this.activity) && (!EntityAIGriefBlocks.MAD_CREEPERS || !(this.theEntity instanceof EntityCreeper) || this.blockDamage == 0.0F);
    }

    // Execute a one shot task or start executing a continuous task.
    @Override
    public void startExecuting() {
        this.canSee = false;
        this.blockDamage = 0.0F;
        this.hitCounter = 0;
        this.theEntity.getNavigator().tryMoveToXYZ(this.blockX, this.blockY, this.blockZ, 1.0);
    }

    // Resets the task.
	@Override
    public void resetTask() {
        this.blockDamage = 0.0F;
        this.giveUpDelay = 0;
        this.targetBlock = null;
    	if (!this.theEntity.getNavigator().noPath()) {
    		this.theEntity.getNavigator().clearPathEntity();
    	}

    	switch (this.activity) {
			case GRIEFING:
				this.resetTaskGriefing();
				break;
			case FIDDLING:
				this.resetTaskFiddling();
				break;
			default:
    	}
        this.activity = Activity.NONE;
    }
    private void resetTaskGriefing() {
        if (EntityAIGriefBlocks.MAD_CREEPERS && this.theEntity instanceof EntityCreeper) {
            ((EntityCreeper) this.theEntity).setCreeperState(-1);
        }
        else {
            this.theEntity.worldObj.destroyBlockInWorldPartially(this.theEntity.getEntityId(), this.blockX, this.blockY, this.blockZ, -1);
        }
    }
    private void resetTaskFiddling() {
    	this.fiddleDelay = 10;
    }

    // Updates the task.
	@Override
    public void updateTask() {
    	switch (this.activity) {
			case GRIEFING:
				this.updateTaskGriefing();
				break;
			case FIDDLING:
				this.updateTaskFiddling();
				break;
			default:
    	}
    }
    private void updateTaskGriefing() {
        this.theEntity.getLookHelper().setLookPosition(this.blockX, this.blockY, this.blockZ, 30.0F, 30.0F);

        if (this.canSee) {
        	if (!this.theEntity.getNavigator().noPath()) {
                this.theEntity.getNavigator().clearPathEntity();
            }

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

                this.blockDamage += BlockHelper.getDamageAmount(this.targetBlock, this.theEntity, this.theEntity.worldObj, this.blockX, this.blockY, this.blockZ);
                if (this.blockDamage >= 1.0F) { // Block is broken.
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

                    this.resetTaskGriefing();
                    this.activity = Activity.NONE;
                }
                this.theEntity.worldObj.destroyBlockInWorldPartially(this.theEntity.getEntityId(), this.blockX, this.blockY, this.blockZ, (int) (this.blockDamage * 10.0F) - 1);
            }
        }
        else {
            if (this.sightCounter-- <= 0) {
                this.sightCounter = 10 + this.random.nextInt(5);
                if (this.checkSight()) {
                	this.sightCounter += 3;
                }
            }

            if (++this.giveUpDelay > 400) {
                this.theEntity.getNavigator().clearPathEntity();
                this.resetTaskGriefing();
                this.activity = Activity.NONE;
            }
            else if (this.theEntity.getNavigator().noPath() && this.giveUpDelay % 12 == 0) {
                this.theEntity.getNavigator().tryMoveToXYZ(this.blockX, this.blockY, this.blockZ, 1.0);
            }
        }
    }
    private void updateTaskFiddling() {
        this.theEntity.getLookHelper().setLookPosition(this.blockX, this.blockY, this.blockZ, 30.0F, 30.0F);

        if (this.canSee) {
        	try {
        		if (this.theEntity.worldObj instanceof WorldServer) {
        			EntityFakePlayer fakePlayer = new EntityFakePlayer(this.theEntity);
					this.targetBlock.onBlockActivated(this.theEntity.worldObj, this.blockX, this.blockY, this.blockZ, fakePlayer, 0, 0.5F, 0.5F, 0.5F);
					fakePlayer.updateWrappedEntityState();
				}
        	}
        	catch (Exception ex) {
        		_SpecialAI.console("Failed to fiddle with block! (" + Block.blockRegistry.getNameForObject(this.targetBlock) + ")");
        		ex.printStackTrace();
        	}

            this.theEntity.swingItem();
            this.theEntity.getNavigator().clearPathEntity();
            this.resetTaskFiddling();
            this.activity = Activity.NONE;
        }
        else {
	        if (this.sightCounter-- <= 0) {
	            this.sightCounter = 10 + this.random.nextInt(5);
	            if (this.checkSight()) {
	            	this.sightCounter += 3;
	            }
	        }

	        if (++this.giveUpDelay > 400) {
	            this.theEntity.getNavigator().clearPathEntity();
	            this.resetTaskFiddling();
	            this.activity = Activity.NONE;
	        }
	        else if (this.theEntity.getNavigator().noPath() && this.giveUpDelay % 13 == 0) {
	            this.theEntity.getNavigator().tryMoveToXYZ(this.blockX, this.blockY, this.blockZ, 1.0);
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
        return target == null || this.blockY == target.blockY && this.blockX == target.blockX && this.blockZ == target.blockZ || this.tryTargetObstructingBlock(target.blockX, target.blockY, target.blockZ);
    }
    private boolean tryTargetObstructingBlock(int x, int y, int z) {
        Block block = this.theEntity.worldObj.getBlock(x, y, z);
    	switch (this.activity) {
			case GRIEFING:
				return this.tryTargetBlockGriefing(block, x, y, z);
			case FIDDLING:
				return this.tryTargetBlockFiddling(block, x, y, z);
			default:
				return false;
    	}
    }

    // Tries to target a block. Returns true if successful.
    private boolean tryTargetBlock(int x, int y, int z) {
        Block block = this.theEntity.worldObj.getBlock(x, y, z);
        return this.tryTargetBlock(block, x, y, z);
    }
    private boolean tryTargetBlock(Block block, int x, int y, int z) {
    	if (this.griefing && this.tryTargetBlockGriefing(block, x, y, z))
			return true;
    	if (this.fiddling && this.fiddleDelay <= 0 && this.tryTargetBlockFiddling(block, x, y, z))
    		return true;
		return false;
    }
    private boolean tryTargetBlockGriefing(Block block, int x, int y, int z) {
        if (this.isValidTargetForGriefing(block, x, y, z)) {
            this.activity = Activity.GRIEFING;
            this.blockX = x;
            this.blockY = y;
            this.blockZ = z;
            this.targetBlock = block;
            return true;
        }
        return false;
    }
    private boolean tryTargetBlockFiddling(Block block, int x, int y, int z) {
        if (this.isValidTargetForFiddling(block, x, y, z)) {
            this.activity = Activity.FIDDLING;
            this.blockX = x;
            this.blockY = y;
            this.blockZ = z;
            this.targetBlock = block;
            return true;
        }
        return false;
    }

    // Returns true if the specified block at some coordinates can be a target of this AI.
    private boolean isValidTargetForGriefing(Block block, int x, int y, int z) {
        TargetBlock testTargetBlock = new TargetBlock(block, this.theEntity.worldObj.getBlockMetadata(x, y, z));
        if (block != null && block != Blocks.air && !(block instanceof BlockLiquid) && !this.griefBlacklist.contains(testTargetBlock) && (this.griefTargetsLights && block.getLightValue() > 1 && !(block instanceof BlockFire) && block != Blocks.lit_redstone_ore && !(block instanceof BlockOre) || this.griefTargetBlocks.contains(testTargetBlock)))
            return BlockHelper.shouldDamage(block, this.theEntity, this.griefNeedsTool, this.theEntity.worldObj, x, y, z);
        return false;
    }
    private boolean isValidTargetForFiddling(Block block, int x, int y, int z) {
        TargetBlock testTargetBlock = new TargetBlock(block, this.theEntity.worldObj.getBlockMetadata(x, y, z));
        if (block != null && block != Blocks.air && !this.fiddleBlacklist.contains(testTargetBlock) && this.fiddleTargetBlocks.contains(testTargetBlock))
            return true;
        return false;
    }

    private static enum Activity {
    	NONE, GRIEFING, FIDDLING;
    }
}