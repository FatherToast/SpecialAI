package fathertoast.specialai.ai.elite;

import fathertoast.specialai.ai.*;
import fathertoast.specialai.config.*;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityTippedArrow;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;

public
class EntityAIBarrage extends EntityAIBase implements IEliteAI
{
	// Possible states for this AI.
	private static final byte STATE_END   = 0;
	private static final byte STATE_START = 1;
	private static final byte STATE_SHOOT = 2;
	
	// The weight of this AI pattern.
	private int WEIGHT;
	
	// The owner of this AI.
	protected EntityLiving theEntity;
	// The arrow damage dealt by this AI.
	private   float        arrowDamage;
	
	// The state the host is in.
	private byte  state;
	// Ticks until next attack.
	private byte  attackTime;
	// The vector of this mob's current attack.
	private Vec3d attackDir;
	
	EntityAIBarrage( ) { }
	
	private
	EntityAIBarrage( EntityLiving entity, float arrowDamage )
	{
		this.theEntity = entity;
		this.arrowDamage = arrowDamage;
		this.setMutexBits( AIHandler.BIT_MOVEMENT | AIHandler.BIT_FACING | AIHandler.BIT_SWIMMING );
	}
	
	// Returns the string name of this AI for use in Properties.
	@Override
	public
	String getName( )
	{
		return "barrage";
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
		entity.tasks.addTask( 0, new EntityAIBarrage( entity, aiTag.getFloat( this.getName( ) ) ) );
	}
	
	// Saves this AI to the tag with its default value.
	@Override
	public
	void save( NBTTagCompound aiTag )
	{
		aiTag.setFloat( this.getName( ), 3.0F );
	}
	
	// Returns true if a copy of this AI is saved to the tag.
	@Override
	public
	boolean isSaved( NBTTagCompound aiTag )
	{
		return aiTag.getFloat( this.getName( ) ) > 0.0F;
	}
	
	// Initializes any one-time effects on the entity.
	@Override
	public
	void initialize( EntityLiving entity )
	{
		entity.setItemStackToSlot( EntityEquipmentSlot.HEAD, new ItemStack( Blocks.DISPENSER ) );
		
		entity.getEntityAttribute( SharedMonsterAttributes.MAX_HEALTH ).applyModifier( new AttributeModifier(
			UUID.randomUUID( ), "Barrager health boost",
			Config.get( ).ELITE_AI.BARRAGE_HEALTH_BOOST, 0
		) );
	}
	
	// Returns whether the AI should begin execution.
	@Override
	public
	boolean shouldExecute( )
	{
		if( !this.theEntity.onGround || this.theEntity.isRiding( ) || this.attackTime-- > 0 || this.theEntity.getRNG( ).nextInt( 10 ) != 0 )
			return false;
		EntityLivingBase target = this.theEntity.getAttackTarget( );
		if( target != null ) {
			double distance = this.theEntity.getDistanceSq( target );
			return distance <= 256.0 && distance >= 25.0 && this.theEntity.getEntitySenses( ).canSee( target );
		}
		return false;
	}
	
	// Returns whether an in-progress EntityAIBase should continue executing
	@Override
	public
	boolean shouldContinueExecuting( )
	{
		return this.state != EntityAIBarrage.STATE_END;
	}
	
	// Determine if this AI task is interruptible by a higher priority task.
	@Override
	public
	boolean isInterruptible( )
	{
		return false;
	}
	
	// Called once when the AI begins execution.
	@Override
	public
	void startExecuting( )
	{
		this.theEntity.getNavigator( ).clearPath( );
		this.attackTime = 30;
		this.state = EntityAIBarrage.STATE_START;
		this.theEntity.swingArm( EnumHand.MAIN_HAND );
		this.theEntity.swingArm( EnumHand.OFF_HAND );
		this.theEntity.world.playSound( null, new BlockPos( this.theEntity ), SoundEvents.BLOCK_STONE_BUTTON_CLICK_ON, this.theEntity.getSoundCategory( ), 1.0F, 1.0F / (this.theEntity.getRNG( ).nextFloat( ) * 0.4F + 0.8F) );
	}
	
	// Called every tick while this AI is executing.
	@Override
	public
	void updateTask( )
	{
		EntityLivingBase target = this.theEntity.getAttackTarget( );
		this.attackTime--;
		if( this.state == EntityAIBarrage.STATE_START ) {
			if( target == null || !target.isEntityAlive( ) ) {
				this.state = EntityAIBarrage.STATE_END;
				return;
			}
			this.theEntity.getLookHelper( ).setLookPositionWithEntity( target, 100.0F, 100.0F );
			if( this.attackTime <= 0 ) {
				this.attackDir = new Vec3d(
					target.posX - this.theEntity.posX,
					target.getEntityBoundingBox( ).minY + target.height / 3.0F - this.theEntity.posY - this.theEntity.getEyeHeight( ),
					target.posZ - this.theEntity.posZ
				);
				this.attackDir = this.attackDir.addVector( 0.0, Math.sqrt( this.attackDir.x * this.attackDir.x + this.attackDir.z * this.attackDir.z ) * 0.2, 0.0 );
				this.attackTime = 60;
				this.state = EntityAIBarrage.STATE_SHOOT;
			}
		}
		else if( this.state == EntityAIBarrage.STATE_SHOOT ) {
			this.theEntity.getLookHelper( ).setLookPosition( this.theEntity.posX + this.attackDir.x, this.theEntity.posY + this.theEntity.getEyeHeight( ), this.theEntity.posZ + this.attackDir.z, 100.0F, 100.0F );
			if( this.attackTime % 5 == 0 ) {
				EntityArrow arrow = new EntityTippedArrow( this.theEntity.world, this.theEntity.posX, this.theEntity.posY + this.theEntity.getEyeHeight( ), this.theEntity.posZ );
				arrow.shootingEntity = this.theEntity;
				arrow.setDamage( this.arrowDamage + this.theEntity.getRNG( ).nextGaussian( ) * 0.5 + this.theEntity.world.getDifficulty( ).getDifficultyId( ) * 0.22 );
				arrow.shoot( this.attackDir.x, this.attackDir.y, this.attackDir.z, 1.8F, 20.0F );
				if( this.theEntity.isBurning( ) ) {
					arrow.setFire( 100 );
				}
				
				this.theEntity.world.playSound( null, new BlockPos( this.theEntity ), SoundEvents.ENTITY_ARROW_SHOOT, this.theEntity.getSoundCategory( ), 1.0F, 1.0F / (this.theEntity.getRNG( ).nextFloat( ) * 0.4F + 0.8F) );
				this.theEntity.world.spawnEntity( arrow );
			}
			else if( this.attackTime <= 0 ) {
				this.state = EntityAIBarrage.STATE_END;
			}
		}
	}
	
	// Resets the task.
	@Override
	public
	void resetTask( )
	{
		this.attackTime = 60;
	}
}
