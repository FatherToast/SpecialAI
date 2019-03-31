package fathertoast.specialai.ai.elite;

import fathertoast.specialai.*;
import fathertoast.specialai.config.*;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public
class EliteAIHandler
{
	// Array of special AI patterns.
	public static final IEliteAI[] ELITE_AI_LIST = {
		new EntityAICharge( ), new EntityAISprint( ), new EntityAIJump( ), new EntityAILeap( ),
		new EntityAIBarrage( ), new EntityAISpawner( ), new EntityAIShaman( ), new EntityAIThrow( ),
		new EntityAIThrowPlayer( ), new EntityAIThief( )
	};
	
	// Applies a random special AI to the mob.
	public static
	void saveSpecialAI( EntityLiving entity, NBTTagCompound aiTag )
	{
		if( Config.get( ).ELITE_AI.AI_WEIGHT_TOTAL > 0 ) {
			int choice = entity.getRNG( ).nextInt( Config.get( ).ELITE_AI.AI_WEIGHT_TOTAL );
			for( int index = EliteAIHandler.ELITE_AI_LIST.length; index-- > 0; ) {
				choice -= EliteAIHandler.ELITE_AI_LIST[ index ].getWeight( );
				if( choice < 0 ) {
					EliteAIHandler.ELITE_AI_LIST[ index ].save( aiTag );
					return;
				}
			}
		}
	}
	
	// Adds any special AI contained within the tag.
	public static
	void addSpecialAI( EntityLiving entity, NBTTagCompound tag, boolean init )
	{
		float healthDiff = init ? entity.getMaxHealth( ) - entity.getHealth( ) : Float.NaN;
		
		for( IEliteAI ai : EliteAIHandler.ELITE_AI_LIST ) {
			if( ai.isSaved( tag ) ) {
				ai.addTo( entity, tag );
				if( init )
					ai.initialize( entity );
			}
		}
		
		if( init )
			entity.setHealth( entity.getMaxHealth( ) - healthDiff );
	}
	
	// Adds a custom attribute modifier to the item stack.
	static
	void addModifierToItem( ItemStack stack, IAttribute attribute, double value, AttributeModOperation operation )
	{
		String name = attribute.getName( );
		stack.addAttributeModifier(
			name,
			new AttributeModifier( SpecialAIMod.MOD_ID + ":" + name, value, operation.id ),
			EntityLiving.getSlotForItemStack( stack )
		);
	}
	
	public
	enum AttributeModOperation
	{
		ADDITION( 0 ), MULTIPLY_BASE( 1 ), MULTIPLY_TOTAL( 2 );
		
		public final int id;
		
		AttributeModOperation( int key ) { id = key; }
	}
}
