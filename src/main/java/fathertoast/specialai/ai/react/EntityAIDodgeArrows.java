package fathertoast.specialai.ai.react;

import fathertoast.specialai.ai.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.util.math.Vec3d;

public
class EntityAIDodgeArrows extends EntityAIBase
{
	public static
	void doDodgeCheckForArrow( Entity arrow )
	{
		float  width = arrow.width + 0.3F;
		double vH    = Math.sqrt( arrow.motionX * arrow.motionX + arrow.motionZ * arrow.motionZ );
		double uX    = arrow.motionX / vH;
		double uZ    = arrow.motionZ / vH;
		
		Entity entity;
		int    dY;
		double dX, dZ, dH;
		double cos, sin;
		for( int i = 0; i < arrow.world.loadedEntityList.size( ); i++ ) {
			entity = arrow.world.loadedEntityList.get( i );
			if( entity instanceof EntityCreature ) {
				
				dY = (int) entity.posY - (int) arrow.posY;
				if( dY < 0 )
					dY = -dY;
				if( dY <= 16 ) {
					// Within vertical range
					dX = entity.posX - arrow.posX;
					dZ = entity.posZ - arrow.posZ;
					dH = Math.sqrt( dX * dX + dZ * dZ );
					if( dH < 24.0 ) {
						// Within horizontal range
						cos = (uX * dX + uZ * dZ) / dH;
						sin = Math.sqrt( 1 - cos * cos );
						if( width > dH * sin ) {
							// Within ray width
							EntityAIDodgeArrows.tryDodgeArrow( (EntityCreature) entity, uX, uZ );
						}
					}
				}
			}
		}
	}
	
	private static
	void tryDodgeArrow( EntityCreature entity, double uX, double uZ )
	{
		for( EntityAITasks.EntityAITaskEntry entry : entity.tasks.taskEntries.toArray( new EntityAITasks.EntityAITaskEntry[ 0 ] ) ) {
			if( entry.action instanceof EntityAIDodgeArrows ) {
				((EntityAIDodgeArrows) entry.action).setDodgeTarget( new Vec3d( uX, 0.0, uZ ) );
			}
		}
	}
	
	// The owner of this AI.
	protected final EntityCreature theEntity;
	// The horizontal velocity of the entity being avoided.
	private final   float          dodgeChance;
	
	// The horizontal velocity of the entity being avoided.
	private Vec3d arrowUVec;
	// Ticks until the entity gives up.
	private int   giveUpDelay;
	// Used to prevent mobs from leaping all over the place from multiple arrows.
	private int   dodgeDelay;
	
	public
	EntityAIDodgeArrows( EntityCreature entity, float chance )
	{
		theEntity = entity;
		dodgeChance = chance;
		setMutexBits( AIHandler.BIT_SWIMMING );
	}
	
	// Tells this AI to dodge an entity.
	private
	void setDodgeTarget( Vec3d motionU )
	{
		if( motionU == null ) {
			arrowUVec = null;
			giveUpDelay = 0;
		}
		else if( dodgeDelay <= 0 && theEntity.getRNG( ).nextFloat( ) < dodgeChance ) {
			arrowUVec = motionU;
			giveUpDelay = 10;
		}
	}
	
	// Returns whether the AI should begin execution.
	@Override
	public
	boolean shouldExecute( )
	{
		return dodgeDelay-- <= 0 && arrowUVec != null && giveUpDelay-- > 0 && theEntity.onGround && !theEntity.isRiding( );// && theEntity.getRNG().nextInt(5) == 0;
	}
	
	// Execute a one shot task or start executing a continuous task.
	@Override
	public
	void startExecuting( )
	{
		if( arrowUVec != null ) {
			Vec3d selfUVec  = new Vec3d( 0.0, 1.0, 0.0 );
			Vec3d dodgeUVec = selfUVec.crossProduct( arrowUVec );
			
			double scale = 0.8;
			if( theEntity.getRNG( ).nextBoolean( ) )
				scale = -scale;
			
			theEntity.motionX = dodgeUVec.x * scale;
			theEntity.motionZ = dodgeUVec.z * scale;
			theEntity.motionY = 0.4;
			
			setDodgeTarget( null );
			dodgeDelay = 40;
		}
	}
	
	// Returns whether an in-progress EntityAIBase should continue executing.
	@Override
	public
	boolean shouldContinueExecuting( )
	{
		return false;
	}
}
