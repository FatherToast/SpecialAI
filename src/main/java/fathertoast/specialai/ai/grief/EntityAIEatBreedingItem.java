package fathertoast.specialai.ai.grief;

import fathertoast.specialai.ai.*;
import fathertoast.specialai.config.*;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.ForgeEventFactory;

import java.util.Collections;
import java.util.List;

public
class EntityAIEatBreedingItem extends EntityAIBase
{
	
	// The owner of this AI.
	protected EntityAnimal theEntity;
	
	// The item this entity wants to eat.
	private EntityItem target;
	// Ticks until the entity will search for more food.
	private int        checkTime;
	// Ticks until the entity gives up.
	private int        giveUpDelay;
	
	public
	EntityAIEatBreedingItem( EntityAnimal entity )
	{
		theEntity = entity;
		setMutexBits( AIHandler.BIT_MOVEMENT | AIHandler.BIT_FACING );
	}
	
	// Returns whether the EntityAIBase should begin execution.
	@Override
	public
	boolean shouldExecute( )
	{
		if( !theEntity.isRiding( ) && ForgeEventFactory.getMobGriefingEvent( theEntity.world, theEntity ) && ++checkTime > 30 ) {
			checkTime = 0;
			return findNearbyFood( );
		}
		return false;
	}
	
	// Returns whether an in-progress EntityAIBase should continue executing.
	@Override
	public
	boolean shouldContinueExecuting( )
	{
		return !theEntity.isRiding( ) && giveUpDelay < 400 && target != null && target.isEntityAlive( ) && theEntity.isBreedingItem( target.getItem( ) ) && !theEntity.getNavigator( ).noPath( );
	}
	
	// Execute a one shot task or start executing a continuous task.
	@Override
	public
	void startExecuting( )
	{
		theEntity.getNavigator( ).tryMoveToXYZ( target.posX, target.posY, target.posZ, 1.25 );
	}
	
	// Updates the task.
	@Override
	public
	void updateTask( )
	{
		theEntity.getLookHelper( ).setLookPositionWithEntity( target, 30.0F, 30.0F );
		
		List list = theEntity.world.getEntitiesWithinAABBExcludingEntity( theEntity, theEntity.getEntityBoundingBox( ).expand( 0.2, 0.0, 0.2 ) );
		if( list.contains( target ) ) {
			if( Config.get( ).GENERAL.EATING_HEALS ) {
				ItemStack itemStack  = target.getItem( );
				float     healAmount = itemStack.getCount( );
				if( !itemStack.isEmpty( ) && itemStack.getItem( ) instanceof ItemFood ) {
					healAmount *= ((ItemFood) itemStack.getItem( )).getHealAmount( itemStack );
				}
				theEntity.heal( healAmount );
			}
			theEntity.world.playSound( null, new BlockPos( theEntity ), SoundEvents.ENTITY_PLAYER_BURP, theEntity.getSoundCategory( ), 0.5F, theEntity.getRNG( ).nextFloat( ) * 0.1F + 0.9F );
			theEntity.getNavigator( ).clearPath( );
			target.setDead( );
		}
		else if( ++giveUpDelay > 400 ) {
			theEntity.getNavigator( ).clearPath( );
		}
		else {
			if( theEntity.getNavigator( ).noPath( ) ) {
				theEntity.getNavigator( ).tryMoveToXYZ( target.posX, target.posY, target.posZ, 1.25 );
			}
		}
	}
	
	// Resets the task.
	@Override
	public
	void resetTask( )
	{
		giveUpDelay = 0;
		theEntity.getNavigator( ).clearPath( );
		target = null;
	}
	
	// Searches for a nearby food item and targets it. Returns true if one is found.
	private
	boolean findNearbyFood( )
	{
		List list = theEntity.world.getEntitiesWithinAABBExcludingEntity( theEntity, theEntity.getEntityBoundingBox( ).expand( 16.0, 8.0, 16.0 ) );
		Collections.shuffle( list );
		for( Object entity : list ) {
			if( entity instanceof EntityItem && theEntity.isBreedingItem( ((EntityItem) entity).getItem( ) ) ) {
				target = (EntityItem) entity;
				return true;
			}
		}
		return false;
	}
}