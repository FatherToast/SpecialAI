package fathertoast.specialai;

import fathertoast.specialai.ai.*;
import fathertoast.specialai.config.*;
import fathertoast.specialai.village.*;
import net.minecraft.command.ServerCommandManager;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.Logger;

@SuppressWarnings( "WeakerAccess" )
@Mod( modid = SpecialAIMod.MOD_ID, name = SpecialAIMod.NAME, version = SpecialAIMod.VERSION )
public
class SpecialAIMod
{
	public static final String MOD_ID  = "specialai";
	public static final String NAME    = "Special AI";
	public static final String VERSION = "1.1.0_for_mc1.12.2";
	
	/* TODO LIST:
	 *  - Improve ai application configs, such as blacklist for elite ai
	 *  - Bosses: Rare, powerful monsters
	 *      + Name influenced by entity type
	 *      + Uses a "unique" item that is a guaranteed drop
	 *      + Unique item is enchanted and has a special enchantment/modifier
	 *      + The special is more powerful than otherwise obtainable, but may come with a drawback
	 *      + Unique item is named based on its special (prefixed by "<bossname>'s")
	 *
	 * Primary features:
	 *  - Dig: break down walls when unable to path to target
	 *  - Stalk: try to hide behind blocks when target is looking in their general direction
	 *  - Portals: walk into nearby portals (part of idle fiddling?)
	 *  - Counterpotions: reactive potion drinking (similar to witches) from a small "inventory"
	 *      + health (damage for undead) when low on health
	 *	    + fire resistance when on fire or taking fire damage
	 *	    + strength when target is armored/enchanted?
	 *	    + invisibility when shot by arrows?
	 *	? Flock: hunt together
	 *  ? Spread fire: ignite flammable blocks when on fire and chasing a target
	 *
	 * Utility features:
	 *  - More editing for AI, particularly for editing pre-existing AI
	 *  - More effective configs, more per-entity-id options
	 */
	
	// This mod's NBT tag.
	public static final String BASE_TAG = "sAI";
	
	private static Logger logger;
	
	public static
	Logger log( ) { return logger; }
	
	private static SimpleNetworkWrapper networkWrapper;
	
	/** @return The network channel for this mod. */
	public static
	SimpleNetworkWrapper network( ) { return networkWrapper; }
	
	@EventHandler
	public
	void preInit( FMLPreInitializationEvent event )
	{
		logger = event.getModLog( );
		Config.init( logger, "Special_AI", event.getModConfigurationDirectory( ) );
		
		int id = -1;
		networkWrapper = NetworkRegistry.INSTANCE.newSimpleChannel( "SAI|FX" );
		if( event.getSide( ) == Side.CLIENT ) {
			network( ).registerMessage( MessageReputationFX.Handler.class, MessageReputationFX.class, ++id, Side.CLIENT );
		}
	}
	
	@EventHandler
	public
	void init( FMLInitializationEvent event ) { }
	
	@EventHandler
	public
	void postInit( FMLPostInitializationEvent event )
	{
		Config.load( );
		if( Config.get( ).GENERAL.DEBUG ) {
			SpecialAIMod.log( ).info( "Loaded in debug mode!" );
		}
		
		MinecraftForge.EVENT_BUS.register( new AIHandler( ) );
		MinecraftForge.EVENT_BUS.register( new ReputationHandler( ) );
	}
	
	// Called as the server is starting.
	@EventHandler
	public
	void serverStarting( FMLServerStartingEvent event )
	{
		ServerCommandManager commandManager = (ServerCommandManager) event.getServer( ).getCommandManager( );
		commandManager.registerCommand( new CommandVillageInfo( ) );
	}
	
	// Returns this mod's compound tag for the entity.
	public static
	NBTTagCompound getTag( Entity entity )
	{
		NBTTagCompound data = entity.getEntityData( );
		if( !data.hasKey( SpecialAIMod.BASE_TAG ) ) {
			data.setTag( SpecialAIMod.BASE_TAG, new NBTTagCompound( ) );
		}
		return data.getCompoundTag( SpecialAIMod.BASE_TAG );
	}
}
