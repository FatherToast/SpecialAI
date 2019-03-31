package fathertoast.specialai.ai.elite;

import net.minecraft.entity.EntityLiving;
import net.minecraft.nbt.NBTTagCompound;

public
interface IEliteAI
{
	// Returns the string name of this AI for use in configs.
	String getName( );
	
	// Gets/sets the weight as defined in configs.
	int getWeight( );
	
	void setWeight( int weight );
	
	// Adds a copy of this AI to the given entity.
	void addTo( EntityLiving entity, NBTTagCompound aiTag );
	
	// Saves this AI to the tag with its default value.
	void save( NBTTagCompound aiTag );
	
	// Returns true if a copy of this AI is saved to the tag.
	boolean isSaved( NBTTagCompound aiTag );
	
	// Initializes any one-time effects on the entity.
	void initialize( EntityLiving entity );
}
