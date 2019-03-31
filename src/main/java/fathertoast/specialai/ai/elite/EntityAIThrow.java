package fathertoast.specialai.ai.elite;

import java.util.List;
import java.util.UUID;

import fathertoast.specialai.ai.*;
import fathertoast.specialai.config.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;

public
class EntityAIThrow extends EntityAIBase implements IEliteAI
{
	private static double SPEED_TO_THROW_TARGET = 1.3;
	private static double SPEED_TO_TARGET       = 1.1;
	
	// The weight of this AI pattern.
	private int WEIGHT;
	
	// The owner of this AI.
	protected EntityLiving theEntity;
	
	// The mob the host wants to throw.
	private EntityLiving throwTarget;
	// Ticks until next attack.
	private int          attackTime;
	// Ticks until the entity gives up.
	private int          giveUpDelay;
	
	EntityAIThrow( ) { }
	
	private
	EntityAIThrow( EntityLiving entity )
	{
		this.theEntity = entity;
		this.setMutexBits( AIHandler.BIT_MOVEMENT | AIHandler.BIT_FACING );
	}
	
	// Returns the string name of this AI for use in Properties.
	@Override
	public
	String getName( )
	{
		return "throw";
	}
	
	// Gets/sets the weight as defined in Properties.
	@Override
	public
	int getWeight( )
	{
		return this.WEIGHT;
	}
	
	@Override
	public
	void setWeight( int weight )
	{
		this.WEIGHT = weight;
	}
	
	// Adds a copy of this AI to the given entity.
	@Override
	public
	void addTo( EntityLiving entity, NBTTagCompound aiTag )
	{
		entity.tasks.addTask( 0, new EntityAIThrow( entity ) );
	}
	
	// Saves this AI to the tag with its default value.
	@Override
	public
	void save( NBTTagCompound aiTag )
	{
		aiTag.setByte( this.getName( ), (byte) 1 );
	}
	
	// Returns true if a copy of this AI is saved to the tag.
	@Override
	public
	boolean isSaved( NBTTagCompound aiTag )
	{
		return aiTag.getByte( this.getName( ) ) > 0;
	}
	
	// Initializes any one-time effects on the entity.
	@Override
	public
	void initialize( EntityLiving entity )
	{
		entity.getEntityAttribute( SharedMonsterAttributes.MOVEMENT_SPEED ).applyModifier( new AttributeModifier(
			UUID.randomUUID( ), "Thrower speed boost",
			Config.get().ELITE_AI.THROW_SPEED_BOOST, 1
		) );
	}
	
	// Returns whether the AI should begin execution.
	@Override
	public
	boolean shouldExecute( )
	{
		EntityLivingBase target = this.theEntity.getAttackTarget( );
		if( this.attackTime-- > 0 || target == null || this.theEntity.isRiding( ) )
			return false;
		if( this.theEntity.isBeingRidden( ) )
			return true;
		if( this.theEntity.getRNG( ).nextInt( 20 ) == 0 )
			return this.findThrowTarget( );
		return false;
	}
	
	// Called once when the AI begins execution.
	@Override
	public
	void startExecuting( )
	{
		if( this.throwTarget != null ) {
			this.theEntity.getNavigator( ).tryMoveToEntityLiving( this.throwTarget, SPEED_TO_THROW_TARGET );
		}
		else {
			EntityLivingBase entity = this.theEntity.getAttackTarget( );
			if( entity != null )
				this.theEntity.getNavigator( ).tryMoveToEntityLiving( entity, SPEED_TO_TARGET );
		}
	}
	
	// Returns whether an in-progress EntityAIBase should continue executing
	@Override
	public
	boolean shouldContinueExecuting( )
	{
		return this.theEntity.getAttackTarget( ) != null && !this.theEntity.isRiding( ) && (this.throwTarget != null && this.throwTarget.isEntityAlive( ) || this.theEntity.isBeingRidden( ));
	}
	
	// Determine if this AI task is interruptible by a higher priority task.
	@Override
	public
	boolean isInterruptible( )
	{
		return false;
	}
	
	// Called every tick while this AI is executing.
	@Override
	public
	void updateTask( )
	{
		EntityLivingBase target = this.theEntity.getAttackTarget( );
		if( target == null )
			return;
		
		if( this.throwTarget != null ) {
			this.theEntity.getLookHelper( ).setLookPositionWithEntity( this.throwTarget, 30.0F, 30.0F );
			
			double range = this.theEntity.width * 2.0F * this.theEntity.width * 2.0F + this.throwTarget.width;
			if( this.theEntity.getDistanceSq( this.throwTarget.posX, this.throwTarget.getEntityBoundingBox( ).minY, this.throwTarget.posZ ) <= range ) {
				this.throwTarget.startRiding( this.theEntity, true );
				this.throwTarget = null;
				this.theEntity.getNavigator( ).tryMoveToEntityLiving( target, SPEED_TO_TARGET );
				this.theEntity.swingArm( EnumHand.MAIN_HAND );
				this.theEntity.swingArm( EnumHand.OFF_HAND );
				this.attackTime = 20;
			}
			else {
				if( this.theEntity.getNavigator( ).noPath( ) ) {
					this.theEntity.getNavigator( ).tryMoveToEntityLiving( this.throwTarget, SPEED_TO_THROW_TARGET );
				}
			}
		}
		if( this.theEntity.isBeingRidden( ) ) {
			this.theEntity.getLookHelper( ).setLookPositionWithEntity( target, 30.0F, 30.0F );
			
			if( this.attackTime-- <= 0 && this.theEntity.getRNG( ).nextInt( 10 ) == 0 && this.theEntity.getDistanceSq( target ) <= 100.0 ) {
				double dX     = target.posX - this.theEntity.posX;
				double dZ     = target.posZ - this.theEntity.posZ;
				double dH     = Math.sqrt( dX * dX + dZ * dZ );
				Entity entity = this.theEntity.getPassengers( ).get( 0 );
				entity.dismountRidingEntity( );
				entity.motionX = dX / dH + this.theEntity.motionX * 0.2;
				entity.motionZ = dZ / dH + this.theEntity.motionZ * 0.2;
				entity.motionY = 0.4;
				entity.onGround = false;
				entity.fallDistance = 0.0F;
				this.theEntity.getNavigator( ).clearPath( );
				this.theEntity.swingArm( EnumHand.MAIN_HAND );
				this.theEntity.swingArm( EnumHand.OFF_HAND );
				this.attackTime = 40 + this.theEntity.getRNG( ).nextInt( 41 );
			}
			else {
				if( this.theEntity.getNavigator( ).noPath( ) ) {
					this.theEntity.getNavigator( ).tryMoveToEntityLiving( target, SPEED_TO_TARGET );
				}
			}
		}
		if( ++this.giveUpDelay > 400 ) {
			this.theEntity.getNavigator( ).clearPath( );
			this.throwTarget = null;
		}
	}
	
	// Resets the task.
	@Override
	public
	void resetTask( )
	{
		if( this.theEntity.isBeingRidden( ) ) {
			this.theEntity.getPassengers( ).get( 0 ).dismountRidingEntity( );
		}
		this.theEntity.getNavigator( ).clearPath( );
		this.giveUpDelay = 0;
		this.throwTarget = null;
	}
	
	// Searches for a nearby mount and targets it. Returns true if one is found.
	private
	boolean findThrowTarget( )
	{
		EntityLivingBase target = this.theEntity.getAttackTarget( );
		if( target == null )
			return false;
		
		double distance = this.theEntity.getDistanceSq( target );
		if( distance < 9.0 )
			return false;
		
		List         list = this.theEntity.world.getEntitiesWithinAABBExcludingEntity( this.theEntity, this.theEntity.getEntityBoundingBox( ).expand( 16.0, 8.0, 16.0 ) );
		EntityLiving entity;
		double       dist;
		for( Object obj : list ) {
			if( obj instanceof EntityLiving ) {
				entity = (EntityLiving) obj;
				if( !entity.onGround || entity.isRiding( ) || target != entity.getAttackTarget( ) || entity.getDistanceSq( target ) <= 36.0 ) {
					continue;
				}
				dist = this.theEntity.getDistanceSq( entity );
				if( dist < distance ) {
					distance = dist;
					this.throwTarget = entity;
				}
			}
		}
		return this.throwTarget != null;
	}
}
