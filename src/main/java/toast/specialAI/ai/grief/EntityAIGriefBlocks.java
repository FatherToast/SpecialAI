package toast.specialAI.ai.grief;

import java.util.HashSet;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFire;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.BlockOre;
import net.minecraft.block.BlockTNT;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.loot.ILootContainer;
import toast.specialAI.Properties;
import toast.specialAI.ModSpecialAI;
import toast.specialAI.ai.AIHandler;
import toast.specialAI.util.BlockHelper;
import toast.specialAI.util.TargetBlock;

public class EntityAIGriefBlocks extends EntityAIBase
{
    // The owner of this AI.
    protected EntityLiving theEntity;
    // The owner's random number generator.
    protected Random random;

    // Override for the scan range config.
    private int scanRangeXZ;
    // Override for the vertical scan range config.
    private int scanRangeY;

    /* Griefing-specific values. These are unused if griefing is false. */
    private final boolean griefing;
    // Override for the break speed config.
    private float griefBreakSpeed;
    // Override for the needs tool config.
    private boolean griefNeedsTool;
    // Override for the break lights config.
    private boolean griefTargetsLights;
    // Set of blocks that can be griefed by this entity.
    private HashSet<TargetBlock> griefTargetBlocks;
    // Set of lootable containers that can be griefed by this entity.
    private HashSet<TargetBlock> griefTargetLootBlocks;
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
    private BlockPos targetPos;
    // The block to attack.
    private IBlockState targetBlock;
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
        if (tag.hasKey(AIHandler.IDLE_RANGE_XZ_TAG)) {
            this.scanRangeXZ = tag.getByte(AIHandler.IDLE_RANGE_XZ_TAG);
        }
        else {
        	this.scanRangeXZ = Properties.get().IDLE_AI.RANGE_XZ;
        }
        if (tag.hasKey(AIHandler.IDLE_RANGE_Y_TAG)) {
        	this.scanRangeY = tag.getByte(AIHandler.IDLE_RANGE_Y_TAG);
        }
        else {
        	this.scanRangeY = Properties.get().IDLE_AI.RANGE_Y;
        }

        // Load griefing-specific tags
        if (griefing) {
	        if (tag.hasKey(AIHandler.GRIEF_BREAK_SPEED)) {
	            this.griefBreakSpeed = tag.getFloat(AIHandler.GRIEF_BREAK_SPEED);
	        }
	        else {
	        	this.griefBreakSpeed = Properties.get().GRIEFING.BREAK_SPEED;
	        }
	        if (tag.hasKey(AIHandler.GRIEF_TOOL_TAG)) {
	            this.griefNeedsTool = tag.getBoolean(AIHandler.GRIEF_TOOL_TAG);
	        }
	        else {
	        	this.griefNeedsTool = Properties.get().GRIEFING.REQUIRES_TOOLS;
	        }
	        if (tag.hasKey(AIHandler.GRIEF_LIGHT_TAG)) {
	            this.griefTargetsLights = tag.getBoolean(AIHandler.GRIEF_LIGHT_TAG);
	        }
	        else {
	        	this.griefTargetsLights = Properties.get().GRIEFING.BREAK_LIGHTS;
	        }
	        if (tag.hasKey(AIHandler.GRIEF_BLOCK_TAG)) {
	            this.griefTargetBlocks = BlockHelper.newBlockSet(tag.getString(AIHandler.GRIEF_BLOCK_TAG));
	        }
	        else {
	        	this.griefTargetBlocks = Properties.get().GRIEFING.TARGETLIST;
	        }
	        if (tag.hasKey(AIHandler.GRIEF_LOOTABLE_TAG)) {
	            this.griefTargetLootBlocks = BlockHelper.newBlockSet(tag.getString(AIHandler.GRIEF_LOOTABLE_TAG));
	        }
	        else {
	        	this.griefTargetLootBlocks = Properties.get().GRIEFING.TARGET_LOOTABLE;
	        }
	        if (tag.hasKey(AIHandler.GRIEF_EXCEPTION_TAG)) {
	            this.griefBlacklist = BlockHelper.newBlockSet(tag.getString(AIHandler.GRIEF_EXCEPTION_TAG));
	        }
	        else {
	        	this.griefBlacklist = Properties.get().GRIEFING.BLACKLIST;
	        }
        }

        // Load fiddling-specific tags
        if (fiddling) {
	        if (tag.hasKey(AIHandler.FIDDLE_BLOCK_TAG)) {
	            this.fiddleTargetBlocks = BlockHelper.newBlockSet(tag.getString(AIHandler.FIDDLE_BLOCK_TAG));
	        }
	        else {
	        	this.fiddleTargetBlocks = Properties.get().FIDDLING.TARGETLIST;
	        }
	        if (tag.hasKey(AIHandler.FIDDLE_EXCEPTION_TAG)) {
	            this.fiddleBlacklist = BlockHelper.newBlockSet(tag.getString(AIHandler.FIDDLE_EXCEPTION_TAG));
	        }
	        else {
	        	this.fiddleBlacklist = Properties.get().FIDDLING.BLACKLIST;
	        }
        }
    }

    // Returns whether the EntityAIBase should begin execution.
    @Override
    public boolean shouldExecute() {
        if (!this.theEntity.isRiding() && this.theEntity.worldObj.getGameRules().getBoolean("mobGriefing")) {
        	// Try picking random blocks
        	this.fiddleDelay--;
            int X = (int) Math.floor(this.theEntity.posX);
            int Y = (int) Math.floor(this.theEntity.posY);
            int Z = (int) Math.floor(this.theEntity.posZ);
            MutableBlockPos pos = new MutableBlockPos();
        	for (int i = 0; i < Properties.get().IDLE_AI.SCAN_COUNT && AIHandler.canScan(); i++) {
        		pos.setPos(
	        		X + this.random.nextInt(this.scanRangeXZ) - this.random.nextInt(this.scanRangeXZ),
	        		Y + this.random.nextInt(this.scanRangeY) - this.random.nextInt(this.scanRangeY),
	        		Z + this.random.nextInt(this.scanRangeXZ) - this.random.nextInt(this.scanRangeXZ)
        		);
        		if (this.tryTargetBlock(pos))
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
        return !this.theEntity.isRiding() && (this.blockDamage > 0.0F || this.giveUpDelay < 400) && this.theEntity.worldObj.getBlockState(this.targetPos).getBlock() == this.targetBlock.getBlock();
    }
    private boolean continueExecutingFiddling() {
        return !this.theEntity.isRiding() && this.giveUpDelay < 400 && this.theEntity.worldObj.getBlockState(this.targetPos).getBlock() == this.targetBlock.getBlock();
    }

    // Determine if this AI task is interruptible by a higher priority task.
    @Override
    public boolean isInterruptible() {
        return Activity.GRIEFING.equals(this.activity) && (!Properties.get().GRIEFING.MAD_CREEPERS || !(this.theEntity instanceof EntityCreeper) || this.blockDamage == 0.0F);
    }

    // Execute a one shot task or start executing a continuous task.
    @Override
    public void startExecuting() {
        this.canSee = false;
        this.blockDamage = 0.0F;
        this.hitCounter = 0;
        this.theEntity.getNavigator().setPath(this.theEntity.getNavigator().getPathToPos(this.targetPos), 1.0);
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
        if (Properties.get().GRIEFING.MAD_CREEPERS && this.theEntity instanceof EntityCreeper) {
            ((EntityCreeper) this.theEntity).setCreeperState(-1);
        }
        else {
            this.theEntity.worldObj.sendBlockBreakProgress(this.theEntity.getEntityId(), this.targetPos, -1);
        }
    }
    private void resetTaskFiddling() {
    	this.fiddleDelay = 80 + this.theEntity.getRNG().nextInt(81);
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
        this.theEntity.getLookHelper().setLookPosition(this.targetPos.getX() + 0.5, this.targetPos.getY() + 0.5, this.targetPos.getZ() + 0.5, 30.0F, 30.0F);

        if (this.canSee) {
        	if (!this.theEntity.getNavigator().noPath()) {
                this.theEntity.getNavigator().clearPathEntity();
            }

            if (Properties.get().GRIEFING.MAD_CREEPERS && this.theEntity instanceof EntityCreeper) {
                ((EntityCreeper) this.theEntity).setCreeperState(1);
                this.blockDamage = 1.0F;
            }
            else {
                if (this.hitCounter == 0) {
                    this.theEntity.swingArm(EnumHand.MAIN_HAND);
                    SoundType sound = this.targetBlock.getBlock().getSoundType(this.targetBlock, this.theEntity.worldObj, this.targetPos, this.theEntity);
                    this.theEntity.worldObj.playSound(null, this.targetPos, sound.getBreakSound(), this.theEntity.getSoundCategory(), sound.getVolume(), sound.getPitch() * 0.8F);
                }
                if (++this.hitCounter >= 5) {
                    this.hitCounter = 0;
                }

                this.blockDamage += BlockHelper.getDamageAmount(this.targetBlock, this.theEntity, this.theEntity.worldObj, this.targetPos) * this.griefBreakSpeed;
                if (this.blockDamage >= 1.0F) { // Block is broken.
                    if (this.targetBlock == Blocks.FARMLAND) {
                        this.theEntity.worldObj.setBlockState(this.targetPos, Blocks.DIRT.getDefaultState(), 3);
                    }
                    else {
                    	this.theEntity.worldObj.destroyBlock(this.targetPos, Properties.get().GRIEFING.LEAVE_DROPS);
                        if (Properties.get().GRIEFING.BREAK_SOUND) {
                            this.theEntity.worldObj.playEvent(1021, this.targetPos, 0);
                        }
                        this.theEntity.swingArm(EnumHand.MAIN_HAND);
                    }
                    this.blockDamage = 0.0F;

                    this.resetTaskGriefing();
                    this.activity = Activity.NONE;
                }
                this.theEntity.worldObj.sendBlockBreakProgress(this.theEntity.getEntityId(), this.targetPos, (int) (this.blockDamage * 10.0F) - 1);
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
                this.theEntity.getNavigator().setPath(this.theEntity.getNavigator().getPathToPos(this.targetPos), 1.0);
            }
        }
    }
    private void updateTaskFiddling() {
        this.theEntity.getLookHelper().setLookPosition(this.targetPos.getX() + 0.5, this.targetPos.getY() + 0.5, this.targetPos.getZ() + 0.5, 30.0F, 30.0F);

        if (this.canSee) {
        	try {
        		if (this.theEntity.worldObj instanceof WorldServer) {
        			if (this.targetBlock.getBlock() instanceof BlockTNT) {
        				((BlockTNT) this.targetBlock.getBlock()).explode(this.theEntity.worldObj, this.targetPos, this.targetBlock.withProperty(BlockTNT.EXPLODE, true), this.theEntity);
        				this.theEntity.worldObj.setBlockState(this.targetPos, Blocks.AIR.getDefaultState(), 11);
        			}
        			else {
	        			EntityFakePlayer fakePlayer = new EntityFakePlayer(this.theEntity);
						this.targetBlock.getBlock().onBlockActivated(this.theEntity.worldObj, this.targetPos, this.targetBlock, fakePlayer,
							EnumHand.MAIN_HAND, this.theEntity.getHeldItem(EnumHand.MAIN_HAND), EnumFacing.DOWN, 0.5F, 0.5F, 0.5F);
						fakePlayer.updateWrappedEntityState();
        			}
				}
        	}
        	catch (Exception ex) {
        		ModSpecialAI.logWarning("Failed to fiddle with block! (" + Block.REGISTRY.getNameForObject(this.targetBlock.getBlock()) + ")");
        		ex.printStackTrace();
        	}

            this.theEntity.swingArm(EnumHand.MAIN_HAND);
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
	            this.theEntity.getNavigator().setPath(this.theEntity.getNavigator().getPathToPos(this.targetPos), 1.0);
	        }
        }
    }

    // Checks line of sight to the target block. Returns true if a ray trace was made.
    private boolean checkSight() {
        double x = this.targetPos.getX() + 0.5;
        double y = this.targetPos.getY() + 0.5;
        double z = this.targetPos.getZ() + 0.5;
        if (this.theEntity.getDistanceSq(x, y - this.theEntity.getEyeHeight(), z) <= Properties.get().IDLE_AI.REACH * Properties.get().IDLE_AI.REACH) {
            Vec3d posVec = new Vec3d(this.theEntity.posX, this.theEntity.posY + this.theEntity.getEyeHeight(), this.theEntity.posZ);

            if (this.checkSight(posVec, x, y + (this.theEntity.posY > y ? 0.5 : -0.5), z) || this.checkSight(posVec, x + (this.theEntity.posX > x ? 0.5 : -0.5), y, z) || this.checkSight(posVec, x, y, z + (this.theEntity.posZ > z ? 0.5 : -0.5))) {
                this.canSee = true;
            }
            return true;
        }
        return false;
    }

    // Ray traces to check sight. Returns true if there is an unobstructed view of the coords.
    private boolean checkSight(Vec3d posVec, double x, double y, double z) {
        Vec3d targetVec = new Vec3d(x, y, z);
        RayTraceResult target = this.theEntity.worldObj.rayTraceBlocks(posVec, targetVec);
        return target == null || RayTraceResult.Type.BLOCK.equals(target.typeOfHit) && this.targetPos.equals(target.getBlockPos()) || this.tryTargetObstructingBlock(target.getBlockPos());
    }
    private boolean tryTargetObstructingBlock(BlockPos pos) {
        IBlockState block = this.theEntity.worldObj.getBlockState(pos);
    	switch (this.activity) {
			case GRIEFING:
				return this.tryTargetBlockGriefing(block, pos);
			case FIDDLING:
				return this.tryTargetBlockFiddling(block, pos);
			default:
				return false;
    	}
    }

    // Tries to target a block. Returns true if successful.
    private boolean tryTargetBlock(BlockPos pos) {
        IBlockState block = this.theEntity.worldObj.getBlockState(pos);
        return this.tryTargetBlock(block, pos);
    }
    private boolean tryTargetBlock(IBlockState block, BlockPos pos) {
    	if (this.griefing && this.tryTargetBlockGriefing(block, pos))
			return true;
    	if (this.fiddling && this.fiddleDelay <= 0 && this.tryTargetBlockFiddling(block, pos))
    		return true;
		return false;
    }
    private boolean tryTargetBlockGriefing(IBlockState block, BlockPos pos) {
        if (this.isValidTargetForGriefing(block, pos)) {
            this.activity = Activity.GRIEFING;
            this.targetPos = pos.toImmutable();
            this.targetBlock = block;
            return true;
        }
        return false;
    }
    private boolean tryTargetBlockFiddling(IBlockState block, BlockPos pos) {
        if (this.isValidTargetForFiddling(block, pos)) {
            this.activity = Activity.FIDDLING;
            this.targetPos = pos.toImmutable();
            this.targetBlock = block;
            return true;
        }
        return false;
    }

    // Returns true if the specified block at some coordinates can be a target of this AI.
    private boolean isValidTargetForGriefing(IBlockState block, BlockPos pos) {
        TargetBlock testTargetBlock = new TargetBlock(block);
        if (block != null && block != Blocks.AIR && !(block instanceof BlockLiquid) && !this.griefBlacklist.contains(testTargetBlock)
        	&& (
        		this.griefTargetsLights && block.getLightValue(this.theEntity.worldObj, pos) > 1 && !(block instanceof BlockFire) && block != Blocks.LIT_REDSTONE_ORE && !(block instanceof BlockOre)
	        	|| this.griefTargetBlocks.contains(testTargetBlock)
	        	|| this.griefTargetLootBlocks.contains(testTargetBlock) && this.isLootContainerTargetable(block, pos)
        	))
            return BlockHelper.shouldDamage(block, this.theEntity, this.griefNeedsTool, this.theEntity.worldObj, pos);
        return false;
    }
    private boolean isValidTargetForFiddling(IBlockState block, BlockPos pos) {
        TargetBlock testTargetBlock = new TargetBlock(block);
        if (block != null && block != Blocks.AIR && !this.fiddleBlacklist.contains(testTargetBlock) && this.fiddleTargetBlocks.contains(testTargetBlock))
            return true;
        return false;
    }

    private boolean isLootContainerTargetable(IBlockState block, BlockPos pos) {
    	TileEntity container = this.theEntity.worldObj.getTileEntity(pos);
    	return container instanceof ILootContainer && ((ILootContainer) container).getLootTable() == null;
    }

    private static enum Activity {
    	NONE, GRIEFING, FIDDLING;
    }
}