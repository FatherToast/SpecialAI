package fathertoast.specialai.ai.elite;

import fathertoast.specialai.ai.*;
import fathertoast.specialai.config.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketEntityVelocity;
import net.minecraft.util.EnumHand;

import java.util.List;
import java.util.UUID;

public
class EntityAIThrowPlayer extends EntityAIBase implements IEliteAI
{
	private static final double SPEED_TO_PLAYER = 1.2;
	private static final double SPEED_TO_TARGET = 1.3;
	
	// Possible states for this AI.
	private static final byte STATE_END   = 0;
	private static final byte STATE_GRAB  = 1;
	private static final byte STATE_CARRY = 2;
	
	// The weight of this AI pattern.
	private int WEIGHT;
	
	// The owner of this AI.
	protected EntityLiving theEntity;
	
	// The state the host is in.
	private byte         state;
	// The mob the host wants to throw its target to.
	private EntityLiving throwTarget;
	// Ticks until next attack.
	private int          attackTime;
	// Ticks until the entity gives up.
	private int          giveUpDelay;
	// Number of times the entity will regrab escaping players before giving up.
	private int          extraGrabAttempts;
	
	EntityAIThrowPlayer( ) { }
	
	private
	EntityAIThrowPlayer( EntityLiving entity )
	{
		this.theEntity = entity;
		this.setMutexBits( AIHandler.BIT_MOVEMENT | AIHandler.BIT_FACING );
	}
	
	// Returns the string name of this AI for use in Properties.
	@Override
	public
	String getName( )
	{
		return "throw_player";
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
		entity.tasks.addTask( 0, new EntityAIThrowPlayer( entity ) );
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
		ItemStack helmet = new ItemStack( Items.LEATHER_HELMET );
		if( Config.get( ).ELITE_AI.THROW_PLAYER_HELMET_DAMAGE > 0.0 ) {
			helmet.setStackDisplayName( "Helmet of Strength" );
			EliteAIHandler.addModifierToItem(
				helmet, SharedMonsterAttributes.ATTACK_DAMAGE,
				Config.get( ).ELITE_AI.THROW_PLAYER_HELMET_DAMAGE,
				EliteAIHandler.AttributeModOperation.ADDITION
			);
		}
		Items.LEATHER_HELMET.setColor( helmet, 0xff0000 );
		entity.setItemStackToSlot( EntityEquipmentSlot.HEAD, helmet );
		
		entity.getEntityAttribute( SharedMonsterAttributes.KNOCKBACK_RESISTANCE ).applyModifier( new AttributeModifier(
			UUID.randomUUID( ), "Playerthrower knockback resistance",
			Config.get( ).ELITE_AI.THROW_PLAYER_KNOCKBACK_RESISTANCE, 0
		) );
		entity.getEntityAttribute( SharedMonsterAttributes.MAX_HEALTH ).applyModifier( new AttributeModifier(
			UUID.randomUUID( ), "Playerthrower health boost",
			Config.get( ).ELITE_AI.THROW_PLAYER_HEALTH_BOOST, 0
		) );
	}
	
	// Returns whether the AI should begin execution.
	@Override
	public
	boolean shouldExecute( )
	{
		return this.attackTime-- <= 0 && this.theEntity.getAttackTarget( ) != null && this.theEntity.getAttackTarget( ).onGround &&
		       !this.theEntity.isRiding( ) && this.theEntity.getRNG( ).nextInt( 20 ) == 0 && this.findThrowTarget( );
	}
	
	// Called once when the AI begins execution.
	@Override
	public
	void startExecuting( )
	{
		this.state = EntityAIThrowPlayer.STATE_GRAB;
		this.extraGrabAttempts = this.theEntity.getRNG( ).nextInt( 4 );
		EntityLivingBase entity = this.theEntity.getAttackTarget( );
		if( entity != null ) {
			this.theEntity.getNavigator( ).tryMoveToEntityLiving( entity, SPEED_TO_PLAYER );
		}
	}
	
	// Returns whether an in-progress EntityAIBase should continue executing
	@Override
	public
	boolean shouldContinueExecuting( )
	{
		return this.state != EntityAIThrowPlayer.STATE_END && this.theEntity.getAttackTarget( ) != null
		       && !this.theEntity.isRiding( ) && this.throwTarget != null && this.throwTarget.isEntityAlive( );
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
		if( this.state == EntityAIThrowPlayer.STATE_GRAB ) {
			EntityLivingBase target = this.theEntity.getAttackTarget( );
			if( target == null )
				return;
			
			this.theEntity.getLookHelper( ).setLookPositionWithEntity( target, 30.0F, 30.0F );
			
			double range = this.theEntity.width * 2.0F * this.theEntity.width * 2.0F + target.width;
			if( this.theEntity.getDistanceSq( target.posX, target.getEntityBoundingBox( ).minY, target.posZ ) <= range ) {
				target.startRiding( this.theEntity, true );
				this.theEntity.getNavigator( ).tryMoveToEntityLiving( this.throwTarget, SPEED_TO_TARGET );
				this.theEntity.swingArm( EnumHand.MAIN_HAND );
				this.theEntity.swingArm( EnumHand.OFF_HAND );
				this.attackTime = 20;
				this.state = EntityAIThrowPlayer.STATE_CARRY;
			}
			else {
				if( this.theEntity.getNavigator( ).noPath( ) ) {
					this.theEntity.getNavigator( ).tryMoveToEntityLiving( target, SPEED_TO_PLAYER );
				}
			}
		}
		else if( this.state == EntityAIThrowPlayer.STATE_CARRY && this.theEntity.isBeingRidden( ) ) {
			this.theEntity.getLookHelper( ).setLookPositionWithEntity( this.throwTarget, 30.0F, 30.0F );
			
			if( this.attackTime-- <= 0 && this.theEntity.getRNG( ).nextInt( 10 ) == 0 && this.theEntity.getDistanceSq( this.throwTarget ) <= 100.0 ) {
				double dX     = this.throwTarget.posX - this.theEntity.posX;
				double dZ     = this.throwTarget.posZ - this.theEntity.posZ;
				double dH     = Math.sqrt( dX * dX + dZ * dZ );
				Entity entity = this.theEntity.getPassengers( ).get( 0 );
				entity.dismountRidingEntity( );
				entity.motionX = dX / dH + this.theEntity.motionX * 0.2;
				entity.motionZ = dZ / dH + this.theEntity.motionZ * 0.2;
				entity.motionY = 0.4;
				entity.onGround = false;
				entity.fallDistance = 0.0F;
				if( entity instanceof EntityPlayerMP ) {
					try {
						((EntityPlayerMP) entity).connection.sendPacket( new SPacketEntityVelocity( entity ) );
					}
					catch( Exception ex ) {
						ex.printStackTrace( );
					}
				}
				this.theEntity.swingArm( EnumHand.MAIN_HAND );
				this.theEntity.swingArm( EnumHand.OFF_HAND );
				
				this.state = EntityAIThrowPlayer.STATE_END;
			}
			else {
				if( this.theEntity.getNavigator( ).noPath( ) ) {
					this.theEntity.getNavigator( ).tryMoveToEntityLiving( this.throwTarget, SPEED_TO_TARGET );
				}
			}
		}
		else if( this.extraGrabAttempts > 0 ) {
			this.extraGrabAttempts--;
			this.state = EntityAIThrowPlayer.STATE_GRAB;
		}
		else {
			this.state = EntityAIThrowPlayer.STATE_END;
		}
		
		if( ++this.giveUpDelay > 400 ) {
			this.state = EntityAIThrowPlayer.STATE_END;
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
		this.attackTime = 80 + this.theEntity.getRNG( ).nextInt( 41 );
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
		
		List         list       = this.theEntity.world.getEntitiesWithinAABBExcludingEntity( this.theEntity, target.getEntityBoundingBox( ).expand( 16.0, 8.0, 16.0 ) );
		EntityLiving entity;
		int          mostNearby = -1;
		int          nearby;
		for( Object obj : list ) {
			if( obj instanceof EntityLiving ) {
				entity = (EntityLiving) obj;
				if( target == entity.getAttackTarget( ) ) {
					if( entity.getDistanceSq( target ) <= 9.0 ) {
						this.throwTarget = null;
						return false;
					}
					nearby = entity.world.getEntitiesWithinAABBExcludingEntity( entity, entity.getEntityBoundingBox( ).expand( 4.0, 1.0, 4.0 ) ).size( );
					if( nearby > mostNearby ) {
						mostNearby = nearby;
						this.throwTarget = entity;
					}
				}
			}
		}
		return this.throwTarget != null;
	}
}
