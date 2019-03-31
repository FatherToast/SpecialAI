package fathertoast.specialai.ai;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAttackMelee;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.MathHelper;

public
class EntityAIAttackMeleePassive extends EntityAIAttackMelee
{
	public
	EntityAIAttackMeleePassive( EntityCreature entity, double speed, boolean mem )
	{
		super( entity, speed, mem );
	}
	
	@Override
	protected
	void checkAndPerformAttack( EntityLivingBase target, double range )
	{
		if( attacker instanceof EntityMob ) {
			super.checkAndPerformAttack( target, range );
		}
		else if( range <= getAttackReachSqr( target ) && attackTick <= 0 ) {
			attackTick = 20;
			attacker.swingArm( EnumHand.MAIN_HAND );
			attackEntityAsMob( target );
		}
	}
	
	// External implementation of EntityMob.attackEntityAsMob(Entity) for normally passive mobs.
	private
	void attackEntityAsMob( EntityLivingBase target )
	{
		float damage;
		try {
			damage = (float) attacker.getEntityAttribute( SharedMonsterAttributes.ATTACK_DAMAGE ).getAttributeValue( );
		}
		catch( Exception ex ) {
			if( attacker instanceof EntitySheep || attacker instanceof EntityRabbit ) {
				damage = 2.0F;
			}
			else if( attacker instanceof EntityChicken ) {
				damage = 1.0F;
			}
			else if( attacker instanceof EntityCow || attacker instanceof EntityHorse ) {
				damage = 4.0F;
			}
			else {
				damage = 3.0F;
			}
		}
		damage += EnchantmentHelper.getModifierForCreature( attacker.getHeldItemMainhand( ), target.getCreatureAttribute( ) );
		int knockback = EnchantmentHelper.getKnockbackModifier( attacker );
		
		if( target.attackEntityFrom( DamageSource.causeMobDamage( attacker ), damage ) ) {
			if( knockback > 0 ) {
				target.knockBack( attacker, knockback * 0.5F, (double) MathHelper.sin( attacker.rotationYaw * 0.0175F ), (double) -MathHelper.cos( attacker.rotationYaw * 0.0175F ) );
				attacker.motionX *= 0.6;
				attacker.motionZ *= 0.6;
			}
			
			int fire = EnchantmentHelper.getFireAspectModifier( attacker ) * 4;
			if( attacker.isBurning( ) ) {
				fire += 2;
			}
			if( fire > 0 ) {
				target.setFire( fire );
			}
			
			if( target instanceof EntityPlayer ) {
				EntityPlayer player = (EntityPlayer) target;
				ItemStack    weapon       = attacker.getHeldItemMainhand( );
				ItemStack    shield       = player.isHandActive( ) ? player.getActiveItemStack( ) : ItemStack.EMPTY;
				
				if( !weapon.isEmpty( ) && !shield.isEmpty( ) && weapon.getItem( ).canDisableShield( weapon, shield, player, attacker ) && shield.getItem( ).isShield( shield, player ) ) {
					float shieldDisableChance = 0.25F + (float) EnchantmentHelper.getEfficiencyModifier( attacker ) * 0.05F;
					
					if( attacker.getRNG( ).nextFloat( ) < shieldDisableChance ) {
						player.getCooldownTracker( ).setCooldown( shield.getItem( ), 100 );
						attacker.world.setEntityState( player, (byte) 30 );
					}
				}
			}
			
			EnchantmentHelper.applyThornEnchantments( target, attacker );
			EnchantmentHelper.applyArthropodEnchantments( attacker, target );
		}
	}
}
