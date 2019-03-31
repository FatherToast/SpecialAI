package fathertoast.specialai.util;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLiving;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.ForgeEventFactory;

@SuppressWarnings( "WeakerAccess" )
public
class BlockHelper
{
	// Event types for the effects fired in "net.minecraft.client.renderer.RenderGlobal".
	public static final int EVENT_ATTACK_DOOR_WOOD   = 1019;
	public static final int EVENT_ATTACK_DOOR_IRON   = 1020;
	public static final int EVENT_BREAK_DOOR_WOOD    = 1021;
	public static final int EVENT_SPAWNER_PARTICLES  = 2004;
	public static final int EVENT_BONEMEAL_PARTICLES = 2005;
	
	// Returns true if the mob should detroy the block.
	public static
	boolean shouldDamage( IBlockState state, EntityLiving entity, boolean needsTool, World world, BlockPos pos )
	{
		return state.getBlockHardness( world, pos ) >= 0.0F && !state.getMaterial( ).isLiquid( ) &&
		       (!needsTool || BlockHelper.canHarvestBlock( entity.getHeldItemMainhand( ), state )) &&
		       state.getBlock( ).canEntityDestroy( state, world, pos, entity ) &&
		       ForgeEventFactory.onEntityDestroyBlock( entity, pos, state );
	}
	
	// Returns the amount of damage to deal to a block.
	public static
	float getDamageAmount( IBlockState state, EntityLiving entity, World world, BlockPos pos )
	{
		float hardness = state.getBlockHardness( world, pos );
		if( hardness < 0.0F )
			return 0.0F;
		
		if( !BlockHelper.canHarvestBlock( entity.getHeldItemMainhand( ), state ) )
			return 1.0F / (hardness * 100.0F);
		return BlockHelper.getCurrentStrengthVsBlock( entity, state ) / (hardness * 30.0F);
	}
	
	// Returns whether the item can harvest the specified block.
	public static
	boolean canHarvestBlock( ItemStack stack, IBlockState state )
	{
		return !state.getMaterial( ).isLiquid( ) && (
			state.getMaterial( ).isToolNotRequired( ) ||
			!stack.isEmpty( ) && stack.canHarvestBlock( state )
		);
	}
	
	// Returns the mob's strength vs. the given block.
	public static
	float getCurrentStrengthVsBlock( EntityLiving entity, IBlockState state )
	{
		ItemStack held     = entity.getHeldItemMainhand( );
		float     strength = held.isEmpty( ) ? 1.0F : held.getDestroySpeed( state );
		
		if( strength > 1.0F ) {
			int efficiency = EnchantmentHelper.getEfficiencyModifier( entity );
			if( efficiency > 0 ) {
				strength += efficiency * efficiency + 1;
			}
		}
		
		if( entity.isPotionActive( MobEffects.HASTE ) ) {
			strength *= 1.0F + (entity.getActivePotionEffect( MobEffects.HASTE ).getAmplifier( ) + 1) * 0.2F;
		}
		if( entity.isPotionActive( MobEffects.MINING_FATIGUE ) ) {
			strength *= 1.0F - (entity.getActivePotionEffect( MobEffects.MINING_FATIGUE ).getAmplifier( ) + 1) * 0.2F;
		}
		
		if( entity.isInsideOfMaterial( Material.WATER ) && !EnchantmentHelper.getAquaAffinityModifier( entity ) ) {
			strength /= 5.0F;
		}
		if( !entity.onGround ) {
			strength /= 5.0F;
		}
		
		return strength < 0.0F ? 0.0F : strength;
	}
}
