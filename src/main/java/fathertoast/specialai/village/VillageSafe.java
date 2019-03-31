package fathertoast.specialai.village;

import net.minecraft.village.Village;
import net.minecraft.world.World;

public
class VillageSafe extends Village
{
	public
	VillageSafe( )
	{
		super( );
	}
	
	public
	VillageSafe( World world )
	{
		super( world );
	}
	
	@Override
	public
	void tick( int tickCounter )
	{
		// This is the update method called by the vanilla village collection;
		// We want to cancel this so that the village does not get updated while it is unloaded from the world
	}
	
	public
	void tickLoaded( int tickCounter )
	{
		super.tick( tickCounter );
	}
}
