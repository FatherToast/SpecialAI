package fathertoast.specialai.util;

import com.mojang.authlib.GameProfile;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.ai.attributes.AbstractAttributeMap;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.FoodStats;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;

import javax.annotation.Nullable;
import java.util.Collection;

public
class EntityFakePlayer extends FakePlayer
{
	// The fake profile used for all fake players used by this mod.
	private static final GameProfile FAKE_PLAYER_PROFILE = new GameProfile( null, "[SpecialAIFakePlayer]" );
	
	// The entity posing as this fake player.
	private final EntityLiving wrappedEntity;
	
	public
	EntityFakePlayer( EntityLiving entity )
	{
		super( (WorldServer) entity.world, EntityFakePlayer.FAKE_PLAYER_PROFILE );
		wrappedEntity = entity;
		foodStats = new FakeFoodStats( this );
		
		copyLocationAndAnglesFrom( entity );
		motionX = entity.motionX;
		motionY = entity.motionY;
		motionZ = entity.motionZ;
	}
	
	// Call this method when you are done using this fake player. After this is called, you should probably throw away the reference to this player.
	public
	void updateWrappedEntityState( )
	{
		if( wrappedEntity != null ) {
			wrappedEntity.copyLocationAndAnglesFrom( this );
			wrappedEntity.motionX = motionX;
			wrappedEntity.motionY = motionY;
			wrappedEntity.motionZ = motionZ;
		}
	}
	
	@Override
	public
	void heal( float amount )
	{
		if( wrappedEntity != null ) {
			wrappedEntity.heal( amount );
		}
	}
	
	@Override
	public
	void setHealth( float amount )
	{
		if( wrappedEntity != null ) {
			wrappedEntity.setHealth( amount );
		}
	}
	
	@Override
	public
	boolean attackEntityFrom( DamageSource source, float damage )
	{
		return wrappedEntity != null && wrappedEntity.attackEntityFrom( source, damage );
	}
	
	@Override
	public
	void knockBack( Entity attacker, float force, double yaw, double pitch )
	{
		if( wrappedEntity != null ) {
			wrappedEntity.knockBack( attacker, force, yaw, pitch );
		}
	}
	
	@Override
	public
	boolean isEntityAlive( )
	{
		return wrappedEntity == null || wrappedEntity.isEntityAlive( );
	}
	
	@Override
	public
	Collection< PotionEffect > getActivePotionEffects( )
	{
		return wrappedEntity != null ? wrappedEntity.getActivePotionEffects( ) : super.getActivePotionEffects( );
	}
	
	@Override
	public
	boolean isPotionActive( Potion potion )
	{
		return wrappedEntity != null && wrappedEntity.isPotionActive( potion );
	}
	
	@Override
	public
	PotionEffect getActivePotionEffect( Potion potion )
	{
		return wrappedEntity != null ? wrappedEntity.getActivePotionEffect( potion ) : null;
	}
	
	@Override
	public
	void addPotionEffect( PotionEffect potionEffect )
	{
		if( wrappedEntity != null ) {
			wrappedEntity.addPotionEffect( potionEffect );
		}
	}
	
	@Override
	public
	boolean isPotionApplicable( PotionEffect potionEffect )
	{
		return wrappedEntity != null && wrappedEntity.isPotionApplicable( potionEffect );
	}
	
	@Override
	public
	void removePotionEffect( Potion potion )
	{
		if( wrappedEntity != null ) {
			wrappedEntity.removePotionEffect( potion );
		}
	}
	
	@Override
	public
	void curePotionEffects( ItemStack curativeItem )
	{
		if( wrappedEntity != null ) {
			wrappedEntity.curePotionEffects( curativeItem );
		}
	}
	
	@Override
	public
	boolean isEntityUndead( )
	{
		return wrappedEntity != null && wrappedEntity.isEntityUndead( );
	}
	
	@Override
	public
	ItemStack getHeldItem( EnumHand hand )
	{
		return wrappedEntity != null ? wrappedEntity.getHeldItem( hand ) : ItemStack.EMPTY;
	}
	
	@Override
	public
	Iterable< ItemStack > getHeldEquipment( )
	{
		return wrappedEntity != null ? wrappedEntity.getHeldEquipment( ) : super.getHeldEquipment( );
	}
	
	@Override
	public
	Iterable< ItemStack > getArmorInventoryList( )
	{
		return wrappedEntity != null ? wrappedEntity.getArmorInventoryList( ) : super.getArmorInventoryList( );
	}
	
	@Override
	public
	void setItemStackToSlot( EntityEquipmentSlot slot, @Nullable ItemStack itemStack )
	{
		if( wrappedEntity != null ) {
			wrappedEntity.setItemStackToSlot( slot, itemStack );
		}
	}
	
	@Override
	@Nullable
	public
	ItemStack getItemStackFromSlot( EntityEquipmentSlot slot )
	{
		return wrappedEntity != null ? wrappedEntity.getItemStackFromSlot( slot ) : ItemStack.EMPTY;
	}
	
	@Override
	public
	IAttributeInstance getEntityAttribute( IAttribute attribute )
	{
		return wrappedEntity != null ? wrappedEntity.getEntityAttribute( attribute ) : super.getEntityAttribute( attribute );
	}
	
	@Override
	public
	AbstractAttributeMap getAttributeMap( )
	{
		return wrappedEntity != null ? wrappedEntity.getAttributeMap( ) : super.getAttributeMap( );
	}
	
	@Override
	public
	boolean isEntityInvulnerable( DamageSource source )
	{
		return wrappedEntity == null || wrappedEntity.isEntityInvulnerable( source );
	}
	
	@Override
	public
	Entity changeDimension( int dim )
	{
		return wrappedEntity != null ? wrappedEntity.changeDimension( dim ) : super.changeDimension( dim );
	}
	
	@Override
	public
	EnumCreatureAttribute getCreatureAttribute( )
	{
		return wrappedEntity != null ? wrappedEntity.getCreatureAttribute( ) : super.getCreatureAttribute( );
	}
	
	private static
	class FakeFoodStats extends FoodStats
	{
		final EntityFakePlayer fakePlayer;
		
		FakeFoodStats( EntityFakePlayer player ) { fakePlayer = player; }
		
		@Override
		public
		void addStats( int food, float saturationModifier )
		{
			if( fakePlayer.wrappedEntity != null ) {
				fakePlayer.wrappedEntity.heal( Math.max( food, 1.0F ) );
			}
		}
		
		@Override
		public
		int getFoodLevel( ) { return 10; }
		
		@Override
		public
		boolean needFood( ) { return true; }
		
		@Override
		public
		float getSaturationLevel( ) { return 10.0F; }
	}
}
