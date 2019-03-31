package fathertoast.specialai.ai.react;

import com.google.common.base.Predicate;
import fathertoast.specialai.ai.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.projectile.EntityDragonFireball;
import net.minecraft.entity.projectile.EntityLargeFireball;
import net.minecraft.entity.projectile.EntityWitherSkull;
import net.minecraft.pathfinding.Path;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public
class EntityAIAvoidExplosions extends EntityAIBase
{
	private static final double SPEED_SLOW = 1.1;
	private static final double SPEED_FAST = 1.3;
	
	// Selects things that are likely to explode.
	private static final Predicate< Entity > IS_EXPLODING_SELECTOR =
		entity ->
			entity instanceof EntityCreeper && ((EntityCreeper) entity).getCreeperState( ) > 0 ||
			entity instanceof EntityTNTPrimed ||
			entity instanceof EntityLargeFireball ||
			entity instanceof EntityDragonFireball ||
			entity instanceof EntityWitherSkull;
	
	// The owner of this AI.
	protected final EntityCreature theEntity;
	
	// The entity currently being avoided.
	private Entity runFromEntity;
	// The path this AI is attempting.
	private Path   pathEntity;
	
	public
	EntityAIAvoidExplosions( EntityCreature entity )
	{
		theEntity = entity;
		setMutexBits( AIHandler.BIT_MOVEMENT | AIHandler.BIT_FACING );
	}
	
	// Returns whether the AI should begin execution.
	@Override
	public
	boolean shouldExecute( )
	{
		List< Entity > nearby = theEntity.world.getEntitiesInAABBexcluding( theEntity, theEntity.getEntityBoundingBox( ).expand( 9.0, 3.0, 9.0 ), EntityAIAvoidExplosions.IS_EXPLODING_SELECTOR );
		if( nearby.isEmpty( ) )
			return false;
		runFromEntity = nearby.get( 0 );
		
		Vec3d target = RandomPositionGenerator.findRandomTargetBlockAwayFrom( theEntity, 16, 7, new Vec3d( runFromEntity.posX, runFromEntity.posY, runFromEntity.posZ ) );
		if( target == null )
			return false;
		if( runFromEntity.getDistanceSq( target.x, target.y, target.z ) < runFromEntity.getDistanceSq( theEntity ) )
			return false;
		
		pathEntity = theEntity.getNavigator( ).getPathToPos( new BlockPos( target ) );
		return pathEntity != null;
	}
	
	// Returns whether an in-progress EntityAIBase should continue executing.
	@Override
	public
	boolean shouldContinueExecuting( )
	{
		return !theEntity.getNavigator( ).noPath( ) && !runFromEntity.isDead;
	}
	
	// Determine if this AI task is interruptible by a higher priority task.
	@Override
	public
	boolean isInterruptible( )
	{
		return false;
	}
	
	// Execute a one shot task or start executing a continuous task.
	@Override
	public
	void startExecuting( )
	{
		theEntity.getNavigator( ).setPath( pathEntity, EntityAIAvoidExplosions.SPEED_SLOW );
	}
	
	// Called every tick while this AI is executing.
	@Override
	public
	void updateTask( )
	{
		theEntity.getNavigator( ).setSpeed(
			theEntity.getDistanceSq( runFromEntity ) < 64.0
			? EntityAIAvoidExplosions.SPEED_FAST
			: EntityAIAvoidExplosions.SPEED_SLOW
		);
	}
	
	// Resets the task.
	@Override
	public
	void resetTask( )
	{
		runFromEntity = null;
	}
}
