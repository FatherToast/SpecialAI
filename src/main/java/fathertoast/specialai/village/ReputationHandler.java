package fathertoast.specialai.village;

import fathertoast.specialai.config.*;
import fathertoast.specialai.util.*;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.Village;
import net.minecraft.village.VillageCollection;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.io.File;
import java.io.FileInputStream;

@SuppressWarnings( { "SameParameterValue", "WeakerAccess" } )
public
class ReputationHandler
{
	// Villages consider players with this rep or below to be enemies.
	public static final int REPUTATION_HATED = -15;
	
	static
	void addReputation( EntityPlayer player, BlockPos sourcePos, Village village, int rep )
	{
		if( rep != 0 ) {
			village.modifyPlayerReputation( player.getUniqueID( ), rep );
			playReputationEffects( player.world, sourcePos, rep > 0 );
		}
	}
	
	static
	void playReputationEffects( World world, BlockPos sourcePos, boolean happy )
	{
		if( Config.get( ).VILLAGES.REPUTATION_PARTICLES ) {
			if( happy ) {
				MessageReputationFX.Type.HAPPY.send( world, sourcePos );
			}
			else {
				MessageReputationFX.Type.ANGRY.send( world, sourcePos );
			}
		}
		if( Config.get( ).VILLAGES.REPUTATION_SOUNDS ) {
			world.playSound(
				null, sourcePos, happy ? SoundEvents.ENTITY_VILLAGER_YES : SoundEvents.ENTITY_VILLAGER_NO,
				SoundCategory.PLAYERS, 1.0F, (world.rand.nextFloat( ) - world.rand.nextFloat( )) * 0.2F + 1.0F
			);
		}
	}
	
	static
	void playAggroEffects( World world, BlockPos sourcePos )
	{
		if( Config.get( ).VILLAGES.REPUTATION_PARTICLES ) {
			world.playEvent( BlockHelper.EVENT_SPAWNER_PARTICLES, sourcePos, 0 );
		}
		if( Config.get( ).VILLAGES.REPUTATION_SOUNDS ) {
			world.playSound(
				null, sourcePos, SoundEvents.ENTITY_VILLAGER_HURT,
				SoundCategory.PLAYERS, 1.0F, (world.rand.nextFloat( ) - world.rand.nextFloat( )) * 0.2F + 1.0F
			);
		}
	}
	
	static
	void addReputationToAll( World world, double radius, Village village, int rep )
	{
		// Add 0 to nearby players' rep to ensure they are known to the village
		double   maxDist = village.getVillageRadius( ) + radius;
		BlockPos coords  = village.getCenter( );
		for( EntityPlayer player : world.playerEntities ) {
			if( player.getDistanceSq( coords ) <= maxDist * maxDist ) {
				village.modifyPlayerReputation( player.getUniqueID( ), 0 );
			}
		}
		
		// Add to all known players' rep
		village.setDefaultPlayerReputation( rep );
	}
	
	@SubscribeEvent( priority = EventPriority.NORMAL )
	public
	void onEntitySpawn( EntityJoinWorldEvent event )
	{
		if( event.getWorld( ).isRemote )
			return;
		
		if( event.getEntity( ) instanceof EntityVillager ) {
			EntityVillager villager = (EntityVillager) event.getEntity( );
			
			// Handle random villager names
			if( Config.get( ).VILLAGES.NAME_VILLAGERS && !villager.hasCustomName( ) ) {
				NameHelperVillager.setVillagerName( villager.getRNG( ), villager );
			}
			if( Config.get( ).VILLAGES.NAME_VILLAGERS_ALWAYS_SHOW && villager.hasCustomName( ) ) {
				villager.setAlwaysRenderNameTag( true );
			}
		}
	}
	
	@SubscribeEvent( priority = EventPriority.NORMAL )
	public
	void onWorldLoad( WorldEvent.Load event )
	{
		if( event.getWorld( ).villageCollection != null && !VillageCollectionSafe.class.equals( event.getWorld( ).villageCollection.getClass( ) ) ) {
			// Load our own village collection class and overwrite the old, janky one
			String            dataIdentifier       = VillageCollection.fileNameForProvider( event.getWorld( ).provider );
			VillageCollection newVillageCollection = null;
			try {
				File villagesdat = event.getWorld( ).getSaveHandler( ).getMapFileFromName( dataIdentifier );
				if( villagesdat.exists( ) ) {
					newVillageCollection = new VillageCollectionSafe( dataIdentifier );
					
					// Set world before loading; fixes player reputation data corrupting village objects
					newVillageCollection.setWorldsForAll( event.getWorld( ) );
					
					FileInputStream in  = new FileInputStream( villagesdat );
					NBTTagCompound  tag = CompressedStreamTools.readCompressed( in );
					in.close( );
					newVillageCollection.readFromNBT( tag.getCompoundTag( "data" ) );
				}
			}
			catch( Exception ex ) {
				ex.printStackTrace( );
			}
			
			if( newVillageCollection == null )
				newVillageCollection = new VillageCollectionSafe( event.getWorld( ) );
			
			event.getWorld( ).villageCollection = newVillageCollection;
			event.getWorld( ).getPerWorldStorage( ).setData( dataIdentifier, event.getWorld( ).villageCollection );
		}
	}
	
	@SubscribeEvent( priority = EventPriority.NORMAL )
	public
	void onLivingDeath( LivingDeathEvent event )
	{
		if( event.getEntity( ).world.isRemote )
			return;
		
		// Give good reputation for defending village
		if( Config.get( ).VILLAGES.HELP_REP_CHANCE > 0.0 && event.getEntityLiving( ).isCreatureType( EnumCreatureType.MONSTER, false ) &&
		    event.getSource( ).getTrueSource( ) instanceof EntityPlayer && event.getEntityLiving( ).getRNG( ).nextDouble( ) < Config.get( ).VILLAGES.HELP_REP_CHANCE ) {
			Village village = event.getEntityLiving( ).world.villageCollection.getNearestVillage( new BlockPos( event.getEntityLiving( ) ), 32 );
			if( village != null ) {
				addReputation( (EntityPlayer) event.getSource( ).getTrueSource( ), new BlockPos( event.getEntityLiving( ) ), village, 1 );
			}
		}
	}
	
	@SubscribeEvent( priority = EventPriority.LOWEST )
	public
	void onFarmlandTrample( BlockEvent.FarmlandTrampleEvent event )
	{
		if( event.getWorld( ).isRemote )
			return;
		
		// Treat trampling as breaking the block
		if( event.getEntity( ) instanceof EntityPlayer ) {
			onBlockBreak( new BlockEvent.BreakEvent( event.getWorld( ), event.getPos( ), event.getState( ), (EntityPlayer) event.getEntity( ) ) );
		}
	}
	
	@SubscribeEvent( priority = EventPriority.NORMAL )
	public
	void onBlockBreak( BlockEvent.BreakEvent event )
	{
		if( event.getWorld( ).isRemote || event.getPlayer( ) == null )
			return;
		Village village = event.getWorld( ).villageCollection.getNearestVillage( event.getPos( ), 8 );
		if( village == null )
			return;
		
		boolean isTreasure = Config.get( ).VILLAGES.TREASURED_LIST.matches( event.getState( ) );
		if( !isTreasure ) {
			if( !Config.get( ).VILLAGES.BLOCK_WHITELIST.isEmpty( ) && !Config.get( ).VILLAGES.BLOCK_WHITELIST.matches( event.getState( ) ) ||
			    Config.get( ).VILLAGES.BLOCK_BLACKLIST.matches( event.getState( ) ) )
				return;
		}
		
		int     playerRep = village.getPlayerReputation( event.getPlayer( ).getUniqueID( ) );
		boolean trigger;
		
		// Reduce reputation for players that destroy the village
		if( playerRep <= Config.get( ).VILLAGES.BLOCK_REP_LIMIT ) {
			if( isTreasure ) {
				trigger = event.getWorld( ).rand.nextDouble( ) < Config.get( ).VILLAGES.TREASURED_REP_CHANCE;
			}
			else {
				trigger = event.getWorld( ).rand.nextDouble( ) < Config.get( ).VILLAGES.BLOCK_REP_CHANCE;
			}
			if( trigger ) {
				addReputation( event.getPlayer( ), event.getPos( ), village, -1 );
			}
		}
		
		// Attack players that destroy the village
		if( playerRep <= ReputationHandler.REPUTATION_HATED ) {
			village.addOrRenewAgressor( event.getPlayer( ) );
			playAggroEffects( event.getWorld( ), event.getPos( ) );
		}
		else if( playerRep <= Config.get( ).VILLAGES.BLOCK_ATTACK_LIMIT ) {
			if( isTreasure ) {
				trigger = event.getWorld( ).rand.nextDouble( ) < Config.get( ).VILLAGES.TREASURED_ATTACK_CHANCE;
			}
			else {
				trigger = event.getWorld( ).rand.nextDouble( ) < Config.get( ).VILLAGES.BLOCK_ATTACK_CHANCE;
			}
			if( trigger ) {
				village.addOrRenewAgressor( event.getPlayer( ) );
				playAggroEffects( event.getWorld( ), event.getPos( ) );
			}
		}
	}
}
