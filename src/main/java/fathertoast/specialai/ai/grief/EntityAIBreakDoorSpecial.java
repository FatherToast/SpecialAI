package fathertoast.specialai.ai.grief;

import fathertoast.specialai.ai.*;
import fathertoast.specialai.config.*;
import fathertoast.specialai.util.*;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.EntityAIBreakDoor;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathNavigateGround;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.ForgeEventFactory;

public
class EntityAIBreakDoorSpecial extends EntityAIBreakDoor
{
	// Override for the break speed config.
	private float                 doorBreakSpeed;
	// Override for the needs target config.
	private boolean               doorNeedsTarget;
	// Override for the needs tool config.
	private boolean               doorNeedsTool;
	// Set of door blocks that can be broken by this entity.
	private TargetBlock.TargetMap doorTargetBlocks;
	
	// Flagged true to end the task.
	private boolean completed;
	// Vector pointing from the entity to the door in the XZ plane. Used to determine when the entity moves past the door.
	private float   initialDoorVecX;
	private float   initialDoorVecZ;
	
	// The block to attack.
	private IBlockState targetBlock;
	// Ticks to count how often to play the "hit" sound.
	private int         hitCounter;
	// Current block damage.
	private float       blockDamage;
	// Previous block damage int sent to clients.
	private int         lastBlockDamage = -1;
	
	public
	EntityAIBreakDoorSpecial( EntityLiving entity, NBTTagCompound tag )
	{
		super( entity );
		setMutexBits( AIHandler.BIT_NONE );
		
		if( tag.hasKey( AIHandler.DOOR_BREAK_SPEED ) ) {
			doorBreakSpeed = tag.getFloat( AIHandler.DOOR_BREAK_SPEED );
		}
		else {
			doorBreakSpeed = Config.get( ).DOOR_BREAKING.BREAK_SPEED;
		}
		if( tag.hasKey( AIHandler.DOOR_BREAK_TARGET_TAG ) ) {
			doorNeedsTarget = tag.getBoolean( AIHandler.DOOR_BREAK_TARGET_TAG );
		}
		else {
			doorNeedsTarget = Config.get( ).DOOR_BREAKING.REQUIRES_TARGET;
		}
		if( tag.hasKey( AIHandler.DOOR_BREAK_TOOL_TAG ) ) {
			doorNeedsTool = tag.getBoolean( AIHandler.DOOR_BREAK_TOOL_TAG );
		}
		else {
			doorNeedsTool = Config.get( ).DOOR_BREAKING.REQUIRES_TOOLS;
		}
		if( tag.hasKey( AIHandler.DOOR_BREAK_BLOCK_TAG ) ) {
			doorTargetBlocks = TargetBlock.newTargetDefinition( tag.getString( AIHandler.DOOR_BREAK_BLOCK_TAG ) );
		}
		else {
			doorTargetBlocks = Config.get( ).DOOR_BREAKING.TARGET_LIST;
		}
	}
	
	@Override
	public
	boolean shouldExecute( )
	{
		if( !entity.collidedHorizontally || !ForgeEventFactory.getMobGriefingEvent( entity.world, entity ) ||
		    doorNeedsTarget && entity.getAttackTarget( ) == null && entity.getRevengeTarget( ) == null ||
		    // Try to find a door to break
		    !findObstructingDoor( ) ) {
			return false;
		}
		// Return true if we are allowed to destroy the door
		IBlockState doorState = entity.world.getBlockState( doorPosition );
		return doorState.getBlock( ).canEntityDestroy( doorState, entity.world, doorPosition, entity ) &&
		       ForgeEventFactory.onEntityDestroyBlock( entity, doorPosition, doorState );
	}
	
	private
	boolean findObstructingDoor( )
	{
		PathNavigateGround navigator = (PathNavigateGround) entity.getNavigator( );
		Path               path      = navigator.getPath( );
		
		if( path != null && !path.isFinished( ) && navigator.getEnterDoors( ) ) {
			// Search along the entity's path
			int maxPoint = Math.min( path.getCurrentPathIndex( ) + 2, path.getCurrentPathLength( ) );
			for( int i = 0; i < maxPoint; i++ ) {
				PathPoint pathpoint = path.getPathPointFromIndex( i );
				BlockPos  footPos   = new BlockPos( pathpoint.x, pathpoint.y, pathpoint.z );
				
				double dX = footPos.getX( ) - entity.posX;
				double dZ = footPos.getZ( ) - entity.posZ;
				if( dX * dX + dZ * dZ <= 2.25 && tryTargetDoor( footPos ) ) {
					return true;
				}
			}
			// Check the space the entity is in
			return tryTargetDoor( new BlockPos( entity ) );
		}
		// Entity has no need to break doors
		return false;
	}
	
	private
	boolean tryTargetDoor( BlockPos footPos )
	{
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(
			footPos.getX( ), footPos.getY( ) - 1 + (int) Math.ceil( entity.height ), footPos.getZ( )
		);
		
		// Target the highest valid block
		while( pos.getY( ) >= footPos.getY( ) ) {
			IBlockState target = entity.world.getBlockState( pos );
			if( doorTargetBlocks.matches( target ) && BlockHelper.shouldDamage( target, entity, doorNeedsTool && !madCreeper( ), entity.world, pos ) ) {
				targetBlock = target;
				doorPosition = pos.toImmutable( );
				return true;
			}
			pos.setY( pos.getY( ) - 1 );
		}
		return false;
	}
	
	@Override
	public
	void startExecuting( )
	{
		if( madCreeper( ) ) {
			((EntityCreeper) entity).ignite( );
			completed = true;
		}
		else {
			completed = false;
			initialDoorVecX = (float) (doorPosition.getX( ) + 0.5F - entity.posX);
			initialDoorVecZ = (float) (doorPosition.getZ( ) + 0.5F - entity.posZ);
			
			hitCounter = 0;
			blockDamage = 0.0F;
			lastBlockDamage = -1;
		}
	}
	
	@Override
	public
	boolean shouldContinueExecuting( )
	{
		return !completed && entity.getDistanceSq( doorPosition ) < 4.0;
	}
	
	@Override
	public
	void resetTask( )
	{
		blockDamage = 0.0F;
		targetBlock = null;
		
		entity.world.sendBlockBreakProgress( entity.getEntityId( ), doorPosition, -1 );
	}
	
	@Override
	public
	void updateTask( )
	{
		float doorVecX   = (float) (doorPosition.getX( ) + 0.5F - entity.posX);
		float doorVecZ   = (float) (doorPosition.getZ( ) + 0.5F - entity.posZ);
		float projection = initialDoorVecX * doorVecX + initialDoorVecZ * doorVecZ;
		
		// This projection becomes negative roughly when the entity passes the door
		if( projection < 0.0F ) {
			completed = true;
		}
		
		if( hitCounter == 0 ) {
			entity.swingArm( EnumHand.MAIN_HAND );
			entity.world.playEvent(
				targetBlock.getMaterial( ) == Material.IRON || targetBlock.getMaterial( ) == Material.ANVIL ?
				BlockHelper.EVENT_ATTACK_DOOR_IRON :
				BlockHelper.EVENT_ATTACK_DOOR_WOOD,
				doorPosition, 0
			);
		}
		if( ++hitCounter >= 16 ) {
			hitCounter = 0;
		}
		
		blockDamage += BlockHelper.getDamageAmount( targetBlock, entity, entity.world, doorPosition ) * doorBreakSpeed;
		if( blockDamage >= 1.0F ) {
			// Block is broken
			entity.world.destroyBlock( doorPosition, Config.get( ).DOOR_BREAKING.LEAVE_DROPS );
			entity.world.playEvent( BlockHelper.EVENT_BREAK_DOOR_WOOD, doorPosition, 0 );
			entity.swingArm( EnumHand.MAIN_HAND );
			blockDamage = 0.0F;
			
			completed = true;
		}
		int damage = (int) Math.ceil( blockDamage * 10.0F ) - 1;
		if( damage != lastBlockDamage ) {
			entity.world.sendBlockBreakProgress( entity.getEntityId( ), doorPosition, damage );
			lastBlockDamage = damage;
		}
	}
	
	private
	boolean madCreeper( ) { return Config.get( ).DOOR_BREAKING.MAD_CREEPERS && entity instanceof EntityCreeper; }
}
