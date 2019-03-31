package fathertoast.specialai.ai.grief;

import fathertoast.specialai.*;
import fathertoast.specialai.ai.*;
import fathertoast.specialai.config.*;
import fathertoast.specialai.util.*;
import net.minecraft.block.*;
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
import net.minecraftforge.event.ForgeEventFactory;

import java.util.Random;

public
class EntityAIGriefBlocks extends EntityAIBase
{
	// The owner of this AI.
	protected final EntityLiving theEntity;
	// The owner's random number generator.
	private final   Random       random;
	
	// Override for the scan range config.
	private int scanRangeXZ;
	// Override for the vertical scan range config.
	private int scanRangeY;
	
	/* Griefing-specific values. These are unused if griefing is disabled. */
	private final boolean               griefingEnabled;
	// Override for the break speed config.
	private       float                 griefBreakSpeed;
	// Override for the needs tool config.
	private       boolean               griefNeedsTool;
	// Override for the break lights config.
	private       boolean               griefTargetsLights;
	// Set of blocks that can be griefed by this entity.
	private       TargetBlock.TargetMap griefTargetBlocks;
	// Set of lootable containers that can be griefed by this entity.
	private       TargetBlock.TargetMap griefTargetLootBlocks;
	// Set of blocks that can NOT be griefed by this entity.
	private       TargetBlock.TargetMap griefBlacklist;
	
	/* Fiddling-specific values. These are unused if fiddling is disabled. */
	private final boolean               fiddlingEnabled;
	// Set of blocks that can be fiddled with by this entity.
	private       TargetBlock.TargetMap fiddleTargetBlocks;
	// Set of blocks that can NOT be fiddled with by this entity.
	private       TargetBlock.TargetMap fiddleBlacklist;
	
	private Activity currentActivity = Activity.NONE;
	
	// True if this entity can see its target.
	private boolean canSee;
	// Ticks until the entity can check line of sight again.
	private int     sightCounter;
	// Ticks until the entity gives up.
	private int     giveUpDelay;
	// Used to prevent mobs from spamming right click on things.
	private int     fiddleDelay;
	
	// The coordinates of the block this entity is attacking.
	private BlockPos    targetPos;
	// The block to attack.
	private IBlockState targetBlock;
	// Ticks to count how often to play the "hit" sound.
	private int         hitCounter;
	// Current block damage.
	private float       blockDamage;
	// Previous block damage int sent to clients.
	private int         lastBlockDamage = -1;
	
	public
	EntityAIGriefBlocks( EntityLiving entity, boolean griefing, boolean fiddling, NBTTagCompound tag )
	{
		theEntity = entity;
		random = entity.getRNG( );
		griefingEnabled = griefing;
		fiddlingEnabled = fiddling;
		setMutexBits( AIHandler.BIT_MOVEMENT | AIHandler.BIT_FACING );
		
		// Load common tags
		if( tag.hasKey( AIHandler.IDLE_RANGE_XZ_TAG ) ) {
			scanRangeXZ = tag.getByte( AIHandler.IDLE_RANGE_XZ_TAG );
		}
		else {
			scanRangeXZ = Config.get( ).IDLE_AI.RANGE_XZ;
		}
		if( tag.hasKey( AIHandler.IDLE_RANGE_Y_TAG ) ) {
			scanRangeY = tag.getByte( AIHandler.IDLE_RANGE_Y_TAG );
		}
		else {
			scanRangeY = Config.get( ).IDLE_AI.RANGE_Y;
		}
		
		// Load griefing-specific tags
		if( griefingEnabled ) {
			if( tag.hasKey( AIHandler.GRIEF_BREAK_SPEED ) ) {
				griefBreakSpeed = tag.getFloat( AIHandler.GRIEF_BREAK_SPEED );
			}
			else {
				griefBreakSpeed = Config.get( ).GRIEFING.BREAK_SPEED;
			}
			if( tag.hasKey( AIHandler.GRIEF_TOOL_TAG ) ) {
				griefNeedsTool = tag.getBoolean( AIHandler.GRIEF_TOOL_TAG );
			}
			else {
				griefNeedsTool = Config.get( ).GRIEFING.REQUIRES_TOOLS;
			}
			if( tag.hasKey( AIHandler.GRIEF_LIGHT_TAG ) ) {
				griefTargetsLights = tag.getBoolean( AIHandler.GRIEF_LIGHT_TAG );
			}
			else {
				griefTargetsLights = Config.get( ).GRIEFING.BREAK_LIGHTS;
			}
			if( tag.hasKey( AIHandler.GRIEF_BLOCK_TAG ) ) {
				griefTargetBlocks = TargetBlock.newTargetDefinition( tag.getString( AIHandler.GRIEF_BLOCK_TAG ) );
			}
			else {
				griefTargetBlocks = Config.get( ).GRIEFING.TARGET_LIST;
			}
			if( tag.hasKey( AIHandler.GRIEF_LOOTABLE_TAG ) ) {
				griefTargetLootBlocks = TargetBlock.newTargetDefinition( tag.getString( AIHandler.GRIEF_LOOTABLE_TAG ) );
			}
			else {
				griefTargetLootBlocks = Config.get( ).GRIEFING.TARGET_LOOTABLE;
			}
			if( tag.hasKey( AIHandler.GRIEF_EXCEPTION_TAG ) ) {
				griefBlacklist = TargetBlock.newTargetDefinition( tag.getString( AIHandler.GRIEF_EXCEPTION_TAG ) );
			}
			else {
				griefBlacklist = Config.get( ).GRIEFING.BLACK_LIST;
			}
		}
		
		// Load fiddling-specific tags
		if( fiddlingEnabled ) {
			if( tag.hasKey( AIHandler.FIDDLE_BLOCK_TAG ) ) {
				fiddleTargetBlocks = TargetBlock.newTargetDefinition( tag.getString( AIHandler.FIDDLE_BLOCK_TAG ) );
			}
			else {
				fiddleTargetBlocks = Config.get( ).FIDDLING.TARGET_LIST;
			}
			if( tag.hasKey( AIHandler.FIDDLE_EXCEPTION_TAG ) ) {
				fiddleBlacklist = TargetBlock.newTargetDefinition( tag.getString( AIHandler.FIDDLE_EXCEPTION_TAG ) );
			}
			else {
				fiddleBlacklist = Config.get( ).FIDDLING.BLACK_LIST;
			}
		}
	}
	
	// Returns whether the EntityAIBase should begin execution.
	@Override
	public
	boolean shouldExecute( )
	{
		if( !theEntity.isRiding( ) && ForgeEventFactory.getMobGriefingEvent( theEntity.world, theEntity ) ) {
			fiddleDelay--;
			sightCounter--;
			
			if( sightCounter <= 0 ) {
				sightCounter = Config.get( ).IDLE_AI.SCAN_DELAY;
				
				// Try picking random blocks
				final int x = (int) Math.floor( theEntity.posX );
				final int y = (int) Math.floor( theEntity.posY );
				final int z = (int) Math.floor( theEntity.posZ );
				
				MutableBlockPos pos = new MutableBlockPos( );
				for( int i = 0; i < Config.get( ).IDLE_AI.SCAN_COUNT && AIHandler.canScan( ); i++ ) {
					pos.setPos(
						x + random.nextInt( scanRangeXZ ) - random.nextInt( scanRangeXZ ),
						y + random.nextInt( scanRangeY ) - random.nextInt( scanRangeY ),
						z + random.nextInt( scanRangeXZ ) - random.nextInt( scanRangeXZ )
					);
					if( tryTargetBlock( pos ) )
						return true;
				}
			}
		}
		return false;
	}
	
	// Returns whether an in-progress EntityAIBase should continue executing.
	@Override
	public
	boolean shouldContinueExecuting( )
	{
		switch( currentActivity ) {
			case GRIEFING:
				return continueExecutingGriefing( );
			case FIDDLING:
				return continueExecutingFiddling( );
			default:
				return false;
		}
	}
	
	private
	boolean continueExecutingGriefing( )
	{
		return !theEntity.isRiding( ) && (blockDamage > 0.0F || giveUpDelay < 400) && theEntity.world.getBlockState( targetPos ).getBlock( ) == targetBlock.getBlock( );
	}
	
	private
	boolean continueExecutingFiddling( )
	{
		return !theEntity.isRiding( ) && giveUpDelay < 400 && theEntity.world.getBlockState( targetPos ).getBlock( ) == targetBlock.getBlock( );
	}
	
	// Determine if this AI task is interruptible by a higher priority task.
	@Override
	public
	boolean isInterruptible( )
	{
		return currentActivity == Activity.GRIEFING && !madCreeper( ) || blockDamage == 0.0F;
	}
	
	// Execute a one shot task or start executing a continuous task.
	@Override
	public
	void startExecuting( )
	{
		sightCounter = 0;
		canSee = false;
		
		hitCounter = 0;
		blockDamage = 0.0F;
		lastBlockDamage = -1;
		
		theEntity.getNavigator( ).setPath( theEntity.getNavigator( ).getPathToPos( targetPos ), 1.0 );
	}
	
	// Resets the task.
	@Override
	public
	void resetTask( )
	{
		blockDamage = 0.0F;
		giveUpDelay = 0;
		targetBlock = null;
		if( !theEntity.getNavigator( ).noPath( ) ) {
			theEntity.getNavigator( ).clearPath( );
		}
		
		switch( currentActivity ) {
			case GRIEFING:
				resetTaskGriefing( );
				break;
			case FIDDLING:
				resetTaskFiddling( );
				break;
			default:
		}
		currentActivity = Activity.NONE;
	}
	
	private
	void resetTaskGriefing( )
	{
		if( madCreeper( ) ) {
			((EntityCreeper) theEntity).setCreeperState( -1 );
		}
		else {
			theEntity.world.sendBlockBreakProgress( theEntity.getEntityId( ), targetPos, -1 );
		}
	}
	
	private
	void resetTaskFiddling( )
	{
		fiddleDelay = 80 + theEntity.getRNG( ).nextInt( 81 );
	}
	
	// Updates the task.
	@Override
	public
	void updateTask( )
	{
		switch( currentActivity ) {
			case GRIEFING:
				updateTaskGriefing( );
				break;
			case FIDDLING:
				updateTaskFiddling( );
				break;
			default:
		}
	}
	
	private
	void updateTaskGriefing( )
	{
		theEntity.getLookHelper( ).setLookPosition( targetPos.getX( ) + 0.5, targetPos.getY( ) + 0.5, targetPos.getZ( ) + 0.5, 30.0F, 30.0F );
		
		if( canSee ) {
			if( !theEntity.getNavigator( ).noPath( ) ) {
				theEntity.getNavigator( ).clearPath( );
			}
			
			if( madCreeper( ) ) {
				((EntityCreeper) theEntity).setCreeperState( 1 );
				blockDamage = 1.0F;
			}
			else {
				if( hitCounter == 0 ) {
					theEntity.swingArm( EnumHand.MAIN_HAND );
					SoundType sound = targetBlock.getBlock( ).getSoundType( targetBlock, theEntity.world, targetPos, theEntity );
					theEntity.world.playSound( null, targetPos, sound.getBreakSound( ), theEntity.getSoundCategory( ), sound.getVolume( ), sound.getPitch( ) * 0.8F );
				}
				if( ++hitCounter >= 5 ) {
					hitCounter = 0;
				}
				
				blockDamage += BlockHelper.getDamageAmount( targetBlock, theEntity, theEntity.world, targetPos ) * griefBreakSpeed;
				if( blockDamage >= 1.0F ) {
					// Block is broken
					if( targetBlock == Blocks.FARMLAND ) {
						theEntity.world.setBlockState( targetPos, Blocks.DIRT.getDefaultState( ), 3 );
					}
					else {
						theEntity.world.destroyBlock( targetPos, Config.get( ).GRIEFING.LEAVE_DROPS );
						if( Config.get( ).GRIEFING.BREAK_SOUND ) {
							theEntity.world.playEvent( BlockHelper.EVENT_BREAK_DOOR_WOOD, targetPos, 0 );
						}
						theEntity.swingArm( EnumHand.MAIN_HAND );
					}
					blockDamage = 0.0F;
					
					resetTaskGriefing( );
					currentActivity = Activity.NONE;
				}
				int damage = (int) Math.ceil( blockDamage * 10.0F ) - 1;
				if( damage != lastBlockDamage ) {
					theEntity.world.sendBlockBreakProgress( theEntity.getEntityId( ), targetPos, damage );
					lastBlockDamage = damage;
				}
			}
		}
		else {
			if( sightCounter-- <= 0 ) {
				sightCounter = 10 + random.nextInt( 5 );
				if( checkSight( ) ) {
					sightCounter += 3;
				}
			}
			
			if( ++giveUpDelay > 400 ) {
				theEntity.getNavigator( ).clearPath( );
				resetTaskGriefing( );
				currentActivity = Activity.NONE;
			}
			else if( theEntity.getNavigator( ).noPath( ) && giveUpDelay % 12 == 0 ) {
				theEntity.getNavigator( ).setPath( theEntity.getNavigator( ).getPathToPos( targetPos ), 1.0 );
			}
		}
	}
	
	private
	void updateTaskFiddling( )
	{
		theEntity.getLookHelper( ).setLookPosition( targetPos.getX( ) + 0.5, targetPos.getY( ) + 0.5, targetPos.getZ( ) + 0.5, 30.0F, 30.0F );
		
		if( canSee ) {
			try {
				if( theEntity.world instanceof WorldServer ) {
					if( targetBlock.getBlock( ) instanceof BlockTNT ) {
						((BlockTNT) targetBlock.getBlock( )).explode( theEntity.world, targetPos, targetBlock.withProperty( BlockTNT.EXPLODE, true ), theEntity );
						theEntity.world.setBlockState( targetPos, Blocks.AIR.getDefaultState( ), 11 );
					}
					else {
						EntityFakePlayer fakePlayer = new EntityFakePlayer( theEntity );
						targetBlock.getBlock( ).onBlockActivated( theEntity.world, targetPos, targetBlock, fakePlayer,
						                                          EnumHand.MAIN_HAND, EnumFacing.DOWN, 0.5F, 0.5F, 0.5F );
						fakePlayer.updateWrappedEntityState( );
					}
				}
			}
			catch( Exception ex ) {
				SpecialAIMod.log( ).warn( "Failed to fiddle with block '{}'", Block.REGISTRY.getNameForObject( targetBlock.getBlock( ) ), ex );
			}
			
			theEntity.swingArm( EnumHand.MAIN_HAND );
			theEntity.getNavigator( ).clearPath( );
			resetTaskFiddling( );
			currentActivity = Activity.NONE;
		}
		else {
			if( sightCounter-- <= 0 ) {
				sightCounter = 10 + random.nextInt( 5 );
				if( checkSight( ) ) {
					sightCounter += 3;
				}
			}
			
			if( ++giveUpDelay > 400 ) {
				theEntity.getNavigator( ).clearPath( );
				resetTaskFiddling( );
				currentActivity = Activity.NONE;
			}
			else if( theEntity.getNavigator( ).noPath( ) && giveUpDelay % 13 == 0 ) {
				theEntity.getNavigator( ).setPath( theEntity.getNavigator( ).getPathToPos( targetPos ), 1.0 );
			}
		}
	}
	
	// Checks line of sight to the target block. Returns true if a ray trace was made.
	private
	boolean checkSight( )
	{
		double x = targetPos.getX( ) + 0.5;
		double y = targetPos.getY( ) + 0.5;
		double z = targetPos.getZ( ) + 0.5;
		if( theEntity.getDistanceSq( x, y - theEntity.getEyeHeight( ), z ) <= Config.get( ).IDLE_AI.REACH * Config.get( ).IDLE_AI.REACH ) {
			Vec3d posVec = new Vec3d( theEntity.posX, theEntity.posY + theEntity.getEyeHeight( ), theEntity.posZ );
			
			if( checkSight( posVec, x, y + (theEntity.posY > y ? 0.5 : -0.5), z ) || checkSight( posVec, x + (theEntity.posX > x ? 0.5 : -0.5), y, z ) || checkSight( posVec, x, y, z + (theEntity.posZ > z ? 0.5 : -0.5) ) ) {
				canSee = true;
			}
			return true;
		}
		return false;
	}
	
	// Ray traces to check sight. Returns true if there is an unobstructed view of the coords.
	private
	boolean checkSight( Vec3d posVec, double x, double y, double z )
	{
		Vec3d          targetVec = new Vec3d( x, y, z );
		RayTraceResult target    = theEntity.world.rayTraceBlocks( posVec, targetVec );
		return target == null || RayTraceResult.Type.BLOCK.equals( target.typeOfHit ) && targetPos.equals( target.getBlockPos( ) ) || tryTargetObstructingBlock( target.getBlockPos( ) );
	}
	
	private
	boolean tryTargetObstructingBlock( BlockPos pos )
	{
		IBlockState block = theEntity.world.getBlockState( pos );
		switch( currentActivity ) {
			case GRIEFING:
				return tryTargetBlockGriefing( block, pos );
			case FIDDLING:
				return tryTargetBlockFiddling( block, pos );
			default:
				return false;
		}
	}
	
	// Tries to target a block. Returns true if successful.
	private
	boolean tryTargetBlock( BlockPos pos )
	{
		IBlockState block = theEntity.world.getBlockState( pos );
		return griefingEnabled && tryTargetBlockGriefing( block, pos ) ||
		       fiddlingEnabled && fiddleDelay <= 0 && tryTargetBlockFiddling( block, pos );
	}
	
	private
	boolean tryTargetBlockGriefing( IBlockState block, BlockPos pos )
	{
		if( isValidTargetForGriefing( block, pos ) ) {
			currentActivity = Activity.GRIEFING;
			targetPos = pos.toImmutable( );
			targetBlock = block;
			return true;
		}
		return false;
	}
	
	private
	boolean tryTargetBlockFiddling( IBlockState block, BlockPos pos )
	{
		if( isValidTargetForFiddling( block, pos ) ) {
			currentActivity = Activity.FIDDLING;
			targetPos = pos.toImmutable( );
			targetBlock = block;
			return true;
		}
		return false;
	}
	
	// Returns true if the specified block at some coordinates can be a target of this AI.
	private
	boolean isValidTargetForGriefing( IBlockState block, BlockPos pos )
	{
		if( block.getBlock( ) != Blocks.AIR && !griefBlacklist.matches( block ) && (
			// Is a targetable light block
			griefTargetsLights &&
			block.getLightValue( theEntity.world, pos ) > 1 && !(block.getBlock( ) instanceof BlockFire) &&
			block.getBlock( ) != Blocks.LIT_REDSTONE_ORE && !(block.getBlock( ) instanceof BlockOre) &&
			block.getBlock( ) != Blocks.MAGMA
			// On grief target list
			|| griefTargetBlocks.matches( block )
			// Is a targetable loot container
			|| griefTargetLootBlocks.matches( block ) && isLootContainerTargetable( block, pos )
		) ) {
			return BlockHelper.shouldDamage( block, theEntity, griefNeedsTool && !madCreeper( ), theEntity.world, pos );
		}
		return false;
	}
	
	private
	boolean isValidTargetForFiddling( IBlockState block, @SuppressWarnings( "unused" ) BlockPos pos )
	{
		return block != null && block != Blocks.AIR && !fiddleBlacklist.matches( block ) && fiddleTargetBlocks.matches( block );
	}
	
	private
	boolean isLootContainerTargetable( @SuppressWarnings( "unused" ) IBlockState block, BlockPos pos )
	{
		TileEntity container = theEntity.world.getTileEntity( pos );
		return container instanceof ILootContainer && ((ILootContainer) container).getLootTable( ) == null;
	}
	
	private
	boolean madCreeper( ) { return Config.get( ).GRIEFING.MAD_CREEPERS && theEntity instanceof EntityCreeper; }
	
	private
	enum Activity
	{
		NONE, GRIEFING, FIDDLING
	}
}
