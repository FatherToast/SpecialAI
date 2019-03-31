package fathertoast.specialai.village;

import com.google.common.collect.Lists;
import fathertoast.specialai.config.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.Village;
import net.minecraft.village.VillageCollection;
import net.minecraft.village.VillageDoorInfo;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;

public
class VillageCollectionSafe extends VillageCollection
{
	public World theWorld;
	
	boolean firstTick = true;
	protected int tickCounter;
	
	// Villages that were at least partly loaded in the last check.
	private final List< VillageSafe > loadedVillageList = Lists.newArrayList( );
	
	// Last saved door count for each village.
	private final HashMap< Village, Integer > doorCountCache = new HashMap<>( );
	
	// Constructor used to load a pre-existing collection.
	public
	VillageCollectionSafe( String dataIdentifier )
	{
		super( dataIdentifier );
	}
	
	// Constructor used to make a new, empty collection.
	public
	VillageCollectionSafe( World world )
	{
		super( world );
		this.theWorld = world;
	}
	
	// Constructor used to make a new collection from a parent.
	public
	VillageCollectionSafe( World world, VillageCollection parent )
	{
		super( world );
		this.theWorld = world;
		
		this.readFromNBT( parent.writeToNBT( new NBTTagCompound( ) ) );
	}
	
	@Override
	public
	void tick( )
	{
		this.tickCounter++;
		
		// Update loaded villages list
		if( this.firstTick || this.tickCounter % 40 == 0 ) {
			Village villageTesterVar;
			VillageSafe village;
			
			for( ListIterator< Village > itr = this.getVillageList( ).listIterator( ); itr.hasNext( ); ) {
				villageTesterVar = itr.next( );
				if( !VillageSafe.class.equals( villageTesterVar.getClass( ) ) ) {
					villageTesterVar = this.convertVillage( villageTesterVar );
					itr.set( villageTesterVar );
				}
				village = (VillageSafe) villageTesterVar;
				
				boolean loadedLast = this.isLoaded( village );
				boolean loaded     = this.isVillageLoadedInWorld( village );
				if( loaded != loadedLast ) {
					if( loaded )
						this.loadVillage( village );
					else
						this.unloadVillage( village );
				}
			}
		}
		
		// Update the villages and the underlying collection
		for( VillageSafe village : this.loadedVillageList ) {
			village.tickLoaded( this.tickCounter );
		}
		super.tick( );
		
		// Add/remove reputation for adding/removing houses.
		if( Config.get( ).VILLAGES.HOUSE_REP && this.tickCounter % 50 == 0 ) {
			for( Village village : this.getVillageList( ) ) {
				if( !this.doorCountCache.containsKey( village ) ) {
					this.doorCountCache.put( village, village.getNumVillageDoors( ) );
				}
				else {
					int difference = village.getNumVillageDoors( ) - this.doorCountCache.get( village );
					if( difference != 0 ) {
						this.doorCountCache.put( village, village.getNumVillageDoors( ) );
						ReputationHandler.addReputationToAll( this.theWorld, 32.0, village, difference );
					}
				}
			}
		}
		
		// Keep doors saved in the village until destroyed.
		if( Config.get( ).VILLAGES.REFRESH_HOUSES && this.tickCounter % 810 == 0 ) {
			for( VillageSafe village : this.loadedVillageList ) {
				this.refreshAllDoors( village );
			}
		}
		
		// Clean up caches
		if( this.tickCounter % 3030 == 0 ) {
			this.doorCountCache.keySet( ).retainAll( this.getVillageList( ) );
		}
		
		this.firstTick = false;
	}
	
	public
	VillageSafe convertVillage( Village village )
	{
		VillageSafe    newVillage = new VillageSafe( this.theWorld );
		NBTTagCompound tag        = new NBTTagCompound( );
		village.writeVillageDataToNBT( tag );
		newVillage.readVillageDataFromNBT( tag );
		return newVillage;
	}
	
	public
	boolean isVillageLoadedInWorld( Village village )
	{
		int      chunkR = 2; // Test using minimum village radius (32) // village.getVillageRadius() >> 4;
		BlockPos center = village.getCenter( );
		for( int x = -chunkR; x <= chunkR; x++ ) {
			for( int z = -chunkR; z <= chunkR; z++ ) {
				if( Math.abs( x ) + Math.abs( z ) <= chunkR && !this.theWorld.isBlockLoaded( center.add( x << 4, 0, z << 4 ) ) )
					return false;
			}
		}
		return true;
	}
	
	public
	boolean isLoaded( VillageSafe village )
	{
		return this.loadedVillageList.contains( village );
	}
	
	public
	void loadVillage( VillageSafe village )
	{
		this.loadedVillageList.add( village );
		this.refreshAllDoors( village );
	}
	
	public
	void unloadVillage( VillageSafe village )
	{
		this.loadedVillageList.remove( village );
	}
	
	public
	void refreshAllDoors( VillageSafe village )
	{
		for( VillageDoorInfo doorInfo : village.getVillageDoorInfoList( ) ) {
			doorInfo.setLastActivityTimestamp( this.tickCounter );
		}
	}
	
	@Override
	public
	void readFromNBT( NBTTagCompound tag )
	{
		NBTTagList villageList = tag.getTagList( "Villages", 10 );
		tag.removeTag( "Villages" ); // No need to load villages multiple times
		super.readFromNBT( tag );
		tag.setTag( "Villages", villageList );
		
		this.tickCounter = tag.getInteger( "Tick" );
		
		// Load the villages correctly
		for( int i = 0; i < villageList.tagCount( ); i++ ) {
			NBTTagCompound villageTag = villageList.getCompoundTagAt( i );
			
			// Village needs a world object to load properly
			Village village = new VillageSafe( this.theWorld );
			
			village.readVillageDataFromNBT( villageTag );
			this.getVillageList( ).add( village );
		}
	}
	
	@Override
	public
	void setWorldsForAll( World world )
	{
		this.theWorld = world;
		super.setWorldsForAll( world );
	}
}
