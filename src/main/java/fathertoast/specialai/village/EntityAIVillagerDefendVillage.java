package fathertoast.specialai.village;

import fathertoast.specialai.ai.*;
import fathertoast.specialai.config.*;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAITarget;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.Village;

import java.util.UUID;

public
class EntityAIVillagerDefendVillage extends EntityAITarget
{
	// The saved village object, occasionally updated.
	private Village          village;
	// Ticks until the village object should be checked again.
	private int              refreshDelay;
	// The currently targeted village aggressor.
	private EntityLivingBase villageAggressor;
	// The username of the player being targeted. Null if target is not a player.
	private UUID             aggressorUUID;
	
	public
	EntityAIVillagerDefendVillage( EntityCreature entity )
	{
		super( entity, false, true );
		this.setMutexBits( AIHandler.TARGET_BIT );
	}
	
	// Returns whether the EntityAIBase should begin execution.
	@Override
	public
	boolean shouldExecute( )
	{
		// Call implementation and play sound if this should execute
		if( this.shouldExecuteImpl( ) ) {
			if( this.taskOwner instanceof EntityVillager ) {
				this.taskOwner.world.playSound(
					null, new BlockPos( this.taskOwner ), SoundEvents.ENTITY_VILLAGER_NO, this.taskOwner.getSoundCategory( ),
					1.0F, 1.0F / (this.taskOwner.getRNG( ).nextFloat( ) * 0.4F + 0.8F)
				);
			}
			if( this.villageAggressor instanceof EntityPlayer ) {
				this.aggressorUUID = this.villageAggressor.getUniqueID( );
			}
			else {
				this.aggressorUUID = null;
			}
			return true;
		}
		return false;
	}
	
	// The actual implementation of shouldExecute.
	private
	boolean shouldExecuteImpl( )
	{
		// Update the saved village object if needed
		if( --this.refreshDelay <= 0 ) {
			this.refreshDelay = 70 + this.taskOwner.getRNG( ).nextInt( 50 );
			this.village = this.taskOwner.world.villageCollection.getNearestVillage( new BlockPos( this.taskOwner ), 32 );
		}
		
		// Check for aggressors in the village
		if( this.village == null )
			return false;
		this.villageAggressor = this.village.findNearestVillageAggressor( this.taskOwner );
		if( !this.isSuitableTarget( this.villageAggressor, false ) ) {
			if( this.taskOwner.getRNG( ).nextInt( 20 ) == 0 ) {
				this.villageAggressor = this.village.getNearestTargetPlayer( this.taskOwner );
				return this.isSuitableTarget( this.villageAggressor, false );
			}
			return false;
		}
		return true;
	}
	
	// Execute a one shot task or start executing a continuous task.
	@Override
	public
	void startExecuting( )
	{
		this.taskOwner.setAttackTarget( this.villageAggressor );
		super.startExecuting( );
	}
	
	// Returns whether an in-progress EntityAIBase should continue executing
	@Override
	public
	boolean shouldContinueExecuting( )
	{
		if( super.shouldContinueExecuting( ) ) {
			if( this.aggressorUUID != null ) {
				int reputation = this.village.getPlayerReputation( this.aggressorUUID );
				return reputation <= Config.get( ).VILLAGES.BLOCK_ATTACK_LIMIT &&
				       (reputation <= -15 || this.taskOwner.getRNG( ).nextFloat( ) >= (reputation - Config.get( ).VILLAGES.BLOCK_ATTACK_LIMIT + 10.0F) / 150.0F);
			}
			return true;
		}
		return false;
	}
}
