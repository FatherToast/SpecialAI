package fathertoast.specialai.config;

import fathertoast.specialai.ai.elite.*;
import fathertoast.specialai.village.*;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.monster.*;
import net.minecraft.entity.passive.*;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.IShearable;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


/**
 * This helper class manages and stores references to user-defined configurations.
 */
public
class Config
{
	// Returns the main config.
	public static
	Config get( ) { return Config.INSTANCE; }
	
	public static
	void init( Logger logger, String fileName, File configDir )
	{
		if( Config.DIRECTORY != null ) {
			throw new IllegalStateException( "Config has already been initialized" );
		}
		Config.log = logger;
		Config.NAME = fileName;
		Config.DIRECTORY = configDir;
	}
	
	public static
	void load( )
	{
		if( DIRECTORY == null ) {
			throw new IllegalStateException( "Config folder must be set before loading configs" );
		}
		
		Config.log.info( "Loading configs..." );
		long startTime = System.nanoTime( );
		
		// Global mod config
		Config.configLoading = new Configuration( new File( Config.DIRECTORY, Config.NAME + ".cfg" ) );
		Config.configLoading.load( );
		Config.INSTANCE = new Config( );
		Config.configLoading.save( );
		Config.configLoading = null;
		
		long estimatedTime = System.nanoTime( ) - startTime;
		Config.log.info( "Loaded configs in {} ms", estimatedTime / 1.0E6 );
	}
	
	// Logger used by the configuration classes.
	static         Logger        log;
	// Config file currently being loaded. Null when not loading any file.
	private static Configuration configLoading;
	
	// The path to the config folder.
	private static File   DIRECTORY;
	// The name to use for this config's files.
	private static String NAME;
	// The instance of the mod's main config.
	private static Config INSTANCE;
	
	private
	Config( ) { }
	
	// General category is specific to the main config (dimension 0).
	public final GENERAL GENERAL = new GENERAL( );
	
	public
	class GENERAL extends PropertyCategory
	{
		@Override
		String name( ) { return "_general"; }
		
		@Override
		String comment( )
		{
			return "General and/or miscellaneous options.";
		}
		
		public final boolean DEBUG = prop(
			"_debug_mode", false,
			"If true, the mod will start up in debug mode."
		);
		
		public final double AGGRESSIVE_CHANCE = prop(
			"depacify_aggressive_chance", 0.0,
			"Chance for an entity on the depacify list to spawn aggressive, instead of just neutral.",
			PropertyCategory.R_DBL_ONE
		);
		
		public final EntityListConfig DEPACIFY_LIST = prop(
			"depacify_list", new EntryEntity[] {
				new EntryEntity( EntityChicken.class, 1.0F ), new EntryEntity( EntityCow.class, 1.0F ),
				new EntryEntity( EntityPig.class, 1.0F ), new EntryEntity( EntitySheep.class, 1.0F ),
				new EntryEntity( EntityRabbit.class, 1.0F ), new EntryEntity( EntitySquid.class, 1.0F )
			},
			"List of passive mobs (by entity id) that are made \"neutral\" like wolves.\n" +
			"You may put a tilde (~) in front of any entity id to make it specific; specific entity ids\n" +
			"will not count any entities extending (i.e., based on) the specified entity.\n" +
			"Additional number after the entity id is the chance (0.0 to 1.0) for entities of that type to spawn with the AI.\n" +
			"May or may not work on mod mobs."
		);
		
		public final boolean EAT_BREEDING_ITEMS = prop(
			"eat_breeding_items", true,
			"If true, passive mobs will seek out and eat the items used to breed them laying on the floor."
		);
		
		public final boolean EATING_HEALS = prop(
			"eating_heals", true,
			"If true, when mobs eat breeding items off the floor, they will regain health like wolves."
		);
		
	}
	
	public final DOOR_BREAKING DOOR_BREAKING = new DOOR_BREAKING( );
	
	public
	class DOOR_BREAKING extends PropertyCategory
	{
		@Override
		public
		String name( ) { return "door_breaking"; }
		
		@Override
		protected
		String comment( )
		{
			return "Options to customize monsters\' door-breaking behavior.";
		}
		
		public final boolean ENABLED = prop(
			"_enabled", true,
			"If true, mobs will destroy doors that are blocking their path."
		);
		
		public final float BREAK_SPEED = prop(
			"break_speed", 0.33F,
			"The block breaking speed multiplier for mobs, relative to the player's block breaking speed."
		);
		
		public final boolean LEAVE_DROPS = prop(
			"leave_drops", true,
			"If true, broken blocks will leave item drops."
		);
		
		public final boolean MAD_CREEPERS = prop(
			"mad_creepers", true,
			"If true, creepers will be very mad about not having arms to break things with, and resort to what they know best..."
		);
		
		public final EntityListConfig MOB_LIST = prop(
			"mob_list", new EntryEntity[] {
				new EntryEntity( EntityZombie.class, 1.0F ), new EntryEntity( EntityCreeper.class, 1.0F ),
				new EntryEntity( EntityPigZombie.class, 1.0F )
			},
			"List of mobs that can gain door-breaking AI (note that the entity must have task-based AI enabled).\n" +
			"The number after each mob is the chance for that mob type to get the AI, from 0 to 1."
		);
		
		public final boolean REQUIRES_TARGET = prop(
			"requires_target", true,
			"If true, mobs will only break doors while they are chasing an attack target.\n" +
			"Setting this to false typically leads to mobs smashing into your house to get to blocks they are targeting\n" +
			"as part of an idle griefing or fiddling behavior, such as torches or chests."
		);
		
		public final boolean REQUIRES_TOOLS = prop(
			"requires_tools", true,
			"If true, mobs will only target blocks they have the tools to harvest.\n" +
			"For example, they will only break iron doors with a pickaxe."
		);
		
		public final TargetBlock.TargetMap TARGET_LIST = prop(
			"target_blocks", buildDefaultDoorTargets( ),
			"Door blocks that can be broken by mobs chasing you."
		);
		
		private
		TargetBlock[] buildDefaultDoorTargets( )
		{
			List< TargetBlock > targets = new ArrayList<>( );
			for( Block block : Block.REGISTRY ) {
				// Door blocks
				if( block instanceof BlockDoor || block instanceof BlockFenceGate || block instanceof BlockTrapDoor ) {
					targets.add( new TargetBlock( block ) );
				}
			}
			return targets.toArray( new TargetBlock[ 0 ] );
		}
	}
	
	public final IDLE_AI IDLE_AI = new IDLE_AI( );
	
	public
	class IDLE_AI extends PropertyCategory
	{
		@Override
		public
		String name( ) { return "idle_activities"; }
		
		@Override
		protected
		String comment( )
		{
			return "Options to customize all idle behaviors for monsters (fiddling/griefing).";
		}
		
		public final float REACH = prop(
			"reach", 3.5F,
			"Mobs' reach (from eye height) when targeting blocks. Player reach is about 4.5."
		);
		
		public final int RANGE_XZ = prop(
			"range", 12,
			"The range at which mobs will search for blocks to target horizontally (xz-plane).",
			PropertyCategory.R_INT_POS1
		);
		
		public final int RANGE_Y = prop(
			"range_vertical", 6,
			"The range at which mobs will search for blocks to target vertically (y-axis).",
			PropertyCategory.R_INT_POS1
		);
		
		public final int SCAN_COUNT = prop(
			"scan_count", 32,
			"The number of blocks each mob randomly searches to grief/fiddle with every \"scan_delay\" ticks.",
			PropertyCategory.R_INT_POS1
		);
		
		public final int SCAN_COUNT_GLOBAL = prop(
			"scan_count_global", 0,
			"The maximum number of blocks that can be searched in any given tick by all mobs. 0 is no limit."
		);
		
		public final int SCAN_DELAY = prop(
			"scan_delay", 2,
			"The number of ticks between each block scan.",
			PropertyCategory.R_INT_POS1
		);
	}
	
	public final FIDDLING FIDDLING = new FIDDLING( );
	
	public
	class FIDDLING extends PropertyCategory
	{
		@Override
		public
		String name( ) { return "idle_fiddling"; }
		
		@Override
		protected
		String comment( )
		{
			return "Options to customize monsters\' idle fiddling behavior.";
		}
		
		public final boolean ENABLED = prop(
			"_enabled", true,
			"If true, mobs will flip switches, press buttons, etc. while not doing anything else."
		);
		
		public final EntityListConfig MOB_LIST = prop(
			"mob_list", new EntryEntity[] {
				new EntryEntity( EntitySkeleton.class, 1.0F ), new EntryEntity( EntityPigZombie.class, 1.0F )
			},
			"List of mobs that can gain idle fiddling AI (note that the entity must have task-based AI enabled).\n" +
			"The number after each mob is the chance for that mob type to get the AI, from 0 to 1."
		);
		
		public final TargetBlock.TargetMap BLACK_LIST = prop(
			"target_blacklist", new TargetBlock[ 0 ],
			"Specific blocks that will NOT be fiddled with by mobs.\n" +
			"Only really useful if you whitelist an entire namespace (*) to prevent mobs from fiddling with a few blocks from that namespace."
		);
		
		public final TargetBlock.TargetMap TARGET_LIST = prop(
			"target_blocks", buildDefaultFiddleTargets( ),
			"Specific blocks that will be fiddled with by mobs."
		);
		
		private
		TargetBlock[] buildDefaultFiddleTargets( )
		{
			List< TargetBlock > targets = new ArrayList<>( );
			for( Block block : Block.REGISTRY ) {
				// Redstone blocks
				if( block instanceof BlockLever || block instanceof BlockButton || block instanceof BlockRedstoneDiode ) {
					targets.add( new TargetBlock( block ) );
				}
				// Door blocks
				else if( block instanceof BlockDoor || block instanceof BlockFenceGate ) {
					if( block.getDefaultState( ).getMaterial( ) != Material.IRON ) {
						targets.add( new TargetBlock( block ) );
					}
				}
				// Misc blocks
				else if( block == Blocks.TNT || block == Blocks.CAKE ) {
					targets.add( new TargetBlock( block ) );
				}
			}
			return targets.toArray( new TargetBlock[ 0 ] );
		}
	}
	
	public final GRIEFING GRIEFING = new GRIEFING( );
	
	public
	class GRIEFING extends PropertyCategory
	{
		@Override
		public
		String name( ) { return "idle_griefing"; }
		
		@Override
		protected
		String comment( )
		{
			return "Options to customize monsters\' idle block breaking.";
		}
		
		public final boolean ENABLED = prop(
			"_enabled", true,
			"If true, mobs will destroy blocks while not doing anything else."
		);
		
		public final boolean BREAK_LIGHTS = prop(
			"break_lights", true,
			"If true, block breaking AI will automatically target all light sources (light value > 1)."
		);
		
		public final boolean BREAK_SOUND = prop(
			"break_sound", false,
			"If true, a lound snapping sound (the vanilla door break sound) will be played when a block\n" +
			"is broken, which is audible regardless of distance."
		);
		
		public final float BREAK_SPEED = prop(
			"break_speed", 0.5F,
			"The block breaking speed multiplier for mobs, relative to the player's block breaking speed."
		);
		
		public final boolean LEAVE_DROPS = prop(
			"leave_drops", true,
			"If true, griefed blocks will leave item drops."
		);
		
		public final boolean MAD_CREEPERS = prop(
			"mad_creepers", true,
			"If true, creepers will be very mad about not having arms to break things with, and resort to what they know best..."
		);
		
		public final EntityListConfig MOB_LIST = prop(
			"mob_list", new EntryEntity[] {
				new EntryEntity( EntityZombie.class, 1.0F ), new EntryEntity( EntityCreeper.class, 1.0F ),
				new EntryEntity( EntityPigZombie.class, 1.0F )
			},
			"List of mobs that can gain passive griefing AI (note that the entity must have task-based AI enabled).\n" +
			"The number after each mob is the chance for that mob type to get the AI, from 0 to 1."
		);
		
		public final boolean REQUIRES_TOOLS = prop(
			"requires_tools", false,
			"If true, mobs will only target blocks they have the tools to harvest.\n" +
			"For example, they will only break stone with a pickaxe."
		);
		
		public final TargetBlock.TargetMap BLACK_LIST = prop(
			"target_blacklist", new TargetBlock[ 0 ],
			"Specific blocks that will NOT be griefed by mobs.\n" +
			"Only really useful if \"break_lights\" is enabled or when you whitelist an entire namespace (*) to create safe\n" +
			"light sources, prevent mobs from breaking normal world gen that produces light, or for removing a few blocks\n" +
			"from a namespace that you don\'t want mobs to break."
		);
		
		public final TargetBlock.TargetMap TARGET_LIST = prop(
			"target_blocks", buildDefaultGriefTargets( ),
			"Specific blocks that will be griefed by mobs."
		);
		
		public final TargetBlock.TargetMap TARGET_LOOTABLE = prop(
			"target_lootable", buildDefaultGriefTargetsLootable( ),
			"Specific lootable blocks that will be griefed by mobs.\n" +
			"Unlike the normal \"target_blocks\", these blocks will not be targeted if they still have a loot table tag\n" +
			"(e.g., unopened dungeon chests)."
		);
		
		private
		TargetBlock[] buildDefaultGriefTargets( )
		{
			List< TargetBlock > targets = new ArrayList<>( );
			for( Block block : Block.REGISTRY ) {
				// Bed blocks
				if( block instanceof BlockBed ) {
					targets.add( new TargetBlock( block ) );
				}
				// Crafting blocks
				else if( block instanceof BlockWorkbench || block instanceof BlockFurnace || block instanceof BlockEnchantmentTable || block instanceof BlockBrewingStand ) {
					targets.add( new TargetBlock( block ) );
				}
				// Access blocks
				else if( block instanceof BlockLadder ) {
					targets.add( new TargetBlock( block ) );
				}
				// Misc blocks
				else if( block == Blocks.FARMLAND || block == Blocks.GOLDEN_RAIL ) {
					targets.add( new TargetBlock( block ) );
				}
			}
			return targets.toArray( new TargetBlock[ 0 ] );
		}
		
		private
		TargetBlock[] buildDefaultGriefTargetsLootable( )
		{
			List< TargetBlock > targets = new ArrayList<>( );
			for( Block block : Block.REGISTRY ) {
				// Chest blocks
				if( block instanceof BlockChest ) {
					targets.add( new TargetBlock( block ) );
				}
			}
			return targets.toArray( new TargetBlock[ 0 ] );
		}
	}
	
	public final REACT_AI REACT_AI = new REACT_AI( );
	
	public
	class REACT_AI extends PropertyCategory
	{
		@Override
		public
		String name( ) { return "reaction_ai"; }
		
		@Override
		protected
		String comment( )
		{
			return "Options to customize reactive behaviors.";
		}
		
		@Override
		protected
		double[] defaultDblRange( )
		{
			return PropertyCategory.R_DBL_ONE;
		}
		
		public final boolean AVOID_EXPLOSIONS = prop(
			"avoid_explosions", true,
			"If true, all mobs will try to avoid TNT and creepers that are about to explode."
		);
		
		public final boolean CALL_HELP = prop(
			"call_for_help", true,
			"If true, all mobs will call for help from nearby mobs of the same type when struck.\n" +
			"Note that this does not trigger from killing blows."
		);
		
		public final double CALL_HELP_ON_DEATH = prop(
			"call_for_help_on_death", 0.2,
			"Chance for mobs to call for help when dealt a killing blow."
		);
		
		public final double DODGE_ARROWS = prop(
			"dodge_arrows", 0.4,
			"The chance any mob will try to sidestep an arrow fired in their direction."
		);
	}
	
	public final JOCKEYS JOCKEYS = new JOCKEYS( );
	
	public
	class JOCKEYS extends PropertyCategory
	{
		@Override
		public
		String name( ) { return "jockeys"; }
		
		@Override
		protected
		String comment( )
		{
			return "Options relating to which mobs should act as riders or mounts.";
		}
		
		public final EntityListConfig MOUNT_LIST = prop(
			"mount_list", new EntryEntity[] {
				new EntryEntity( EntitySpider.class, true ), new EntryEntity( EntitySlime.class, true ),
				new EntryEntity( EntityPig.class, true ), new EntryEntity( EntitySheep.class, true ),
				new EntryEntity( EntityCow.class, true ), new EntryEntity( EntityPolarBear.class, true )
			},
			"List of mobs that can be ridden on by normal-sized riders (not all entities can be controlled by their rider)."
		);
		
		public final EntityListConfig MOUNT_LIST_SMALL = prop(
			"mount_list_small", new EntryEntity[] {
				new EntryEntity( EntityChicken.class, true ), new EntryEntity( EntityRabbit.class, true )
			},
			"List of mobs that can be ridden on by small riders or normal-sized riders that are babies" +
			"(not all entities can be controlled by their rider)."
		);
		
		public final EntityListConfig RIDER_LIST = prop(
			"rider_list", new EntryEntity[] {
				new EntryEntity( EntityZombie.class, 0.05F ), new EntryEntity( EntitySkeleton.class, 0.1F ),
				new EntryEntity( EntityCreeper.class, 0.05F ), new EntryEntity( EntityWitch.class, 0.05F ),
				new EntryEntity( EntityPigZombie.class, 0.1F )
			},
			"List of mobs that can ride normal-sized mounts and the chance for them to gain the rider AI.\n" +
			"Note that the entity must have task-based AI enabled."
		);
		
		public final EntityListConfig RIDER_LIST_SMALL = prop(
			"rider_list_small", new EntryEntity[] { },
			"List of mobs that can only ride small mounts or normal-sized mounts that are babies and the chance for them to gain the rider AI.\n" +
			"Note that the entity must have task-based AI enabled."
		);
		
	}
	
	public final ELITE_AI ELITE_AI = new ELITE_AI( );
	
	public
	class ELITE_AI extends PropertyCategory
	{
		@Override
		public
		String name( ) { return "elite_ai"; }
		
		@Override
		protected
		String comment( )
		{
			return "Options to control the types of elite AI and their weighted chances of occurring.";
		}
		
		@Override
		protected
		double[] defaultDblRange( )
		{
			return PropertyCategory.R_DBL_ONE;
		}
		
		public final EntityListConfig MOB_LIST = prop(
			"_mob_list", new EntryEntity[] {
				new EntryEntity( EntityZombie.class, 0.05F, 0.05F ), new EntryEntity( EntitySkeleton.class, 0.2F, 0.05F ),
				new EntryEntity( EntityPigZombie.class, 0.1F, 0.05F, 0.02F )
			},
			"List of mobs that can gain special AI patterns and their chances to gain those patterns.\n" +
			"You can specify multiple chances for each entity - each chance will be rolled and multiple AIs can stack.\n" +
			"Note that the entity must have task-based AI enabled."
		);
		
		public final int AI_WEIGHT_TOTAL;
		
		{
			// Weights are dynamically created and stored in EliteAIHandler
			int weightTotal = 0;
			for( IEliteAI ai : EliteAIHandler.ELITE_AI_LIST ) {
				ai.setWeight( Math.max( 0, prop(
					"ai_" + ai.getName( ), 1,
					"Weight for the " + ai.getName( ) + " ai pattern to be chosen."
				) ) );
				weightTotal += ai.getWeight( );
			}
			AI_WEIGHT_TOTAL = weightTotal;
		}
		
		public final double BARRAGE_HEALTH_BOOST = prop(
			"barrage_health_boost", 20.0,
			"Flat health increase added to mobs with barrage ai.",
			PropertyCategory.R_DBL_ALL
		);
		
		public final double CHARGE_HEALTH_BOOST = prop(
			"charge_health_boost", 20.0,
			"Flat health increase added to mobs with charge ai.",
			PropertyCategory.R_DBL_ALL
		);
		
		public final double CHARGE_KNOCKBACK_RESISTANCE = prop(
			"charge_knockback_resistance", 0.5,
			"Knockback resistance added to mobs with charge ai."
		);
		
		public final double LEAP_SPEED_BOOST = prop(
			"leap_speed_boost", 0.1,
			"Speed increase multiplier to mobs with leap ai. Recommended to keep this well below 0.5.",
			PropertyCategory.R_DBL_ALL
		);
		
		public final float SHAMAN_HEAL_AMOUNT = prop(
			"shaman_heal_amount", 1.0F,
			"Amount (in half-hearts) healed by mobs with shaman ai every 2 seconds. Can be overridden by the mob\'s nbt data.",
			PropertyCategory.R_FLT_ALL
		);
		
		public final double SHAMAN_HEALTH_BOOST = prop(
			"shaman_health_boost", 20.0,
			"Flat health increase added to mobs with shaman ai.",
			PropertyCategory.R_DBL_ALL
		);
		
		public final double SPAWNER_HEALTH_BOOST = prop(
			"spawner_health_boost", 40.0,
			"Flat health increase added to mobs with spawner ai.",
			PropertyCategory.R_DBL_ALL
		);
		
		public final double SPAWNER_SPEED_BOOST = prop(
			"spawner_speed_boost", -0.2,
			"Speed increase multiplier to mobs with spawner ai. Recommended to keep this well below 0.5.",
			PropertyCategory.R_DBL_ALL
		);
		
		public final double SPRINT_BOOTS_SPEED_BOOST = prop(
			"sprint_boots_speed_boost", 0.1,
			"Speed increase multiplier to the boots worn by mobs with sprint ai (these can drop as loot!).\n" +
			"Recommended to keep this well below 0.5.",
			PropertyCategory.R_DBL_POS
		);
		
		public final float SPRINT_SPEED_BOOST = prop(
			"sprint_speed_boost", 0.7F,
			"Speed increase multiplier to mobs with sprint ai while they are sprinting. Can be overridden by the mob\'s nbt data.\n" +
			"Setting this to 0 breaks the sprint ai, so don\'t do that.",
			PropertyCategory.R_FLT_POS
		);
		
		public final float THIEF_AVOID_RANGE = prop(
			"thief_avoid_range", 16.0F,
			"The minimum distance that mobs with thief ai will try to keep from players once they have stolen an item.\n" +
			"Can be overridden by the mob\'s nbt data.",
			1.0F, Float.POSITIVE_INFINITY
		);
		
		public final double THIEF_HELMET_SPEED_BOOST = prop(
			"thief_helmet_speed_boost", 0.1,
			"Speed increase multiplier to the helmet worn by mobs with thief ai (these can drop as loot!).\n" +
			"Recommended to keep this well below 0.5.",
			PropertyCategory.R_DBL_POS
		);
		
		public final double THROW_SPEED_BOOST = prop(
			"throw_speed_boost", 0.1,
			"Speed increase multiplier to mobs with throw ai. Recommended to keep this well below 0.5.",
			PropertyCategory.R_DBL_ALL
		);
		
		public final double THROW_PLAYER_HEALTH_BOOST = prop(
			"throw_player_health_boost", 20.0,
			"Flat health increase added to mobs with throw-player ai.",
			PropertyCategory.R_DBL_ALL
		);
		
		public final double THROW_PLAYER_HELMET_DAMAGE = prop(
			"throw_player_helmet_damage_boost", 1.0,
			"Flat damage increase added to the helmet worn by mobs with throw-player ai (these can drop as loot!).",
			PropertyCategory.R_DBL_POS
		);
		
		public final double THROW_PLAYER_KNOCKBACK_RESISTANCE = prop(
			"throw_player_knockback_resistance", 0.5,
			"Knockback resistance added to mobs with throw-player ai."
		);
	}
	
	public final VILLAGES VILLAGES = new VILLAGES( );
	
	public
	class VILLAGES extends PropertyCategory
	{
		private final int[] R_INT_VILLAGE_REP = { -30, 10 };
		
		@Override
		public
		String name( ) { return "villages"; }
		
		@Override
		protected
		String comment( )
		{
			return "Options to control village aggression and reputation.\n" +
			       "For reference, starting reputation is 0, minimum is -30 and maximum is 10.\n" +
			       "You are considered an enemy to a village if your reputation is " + ReputationHandler.REPUTATION_HATED + " or lower.";
		}
		
		@Override
		double[] defaultDblRange( )
		{
			return PropertyCategory.R_DBL_ONE;
		}
		
		public final double BLOCK_ATTACK_CHANCE = prop(
			"block_aggression_chance", 0.1,
			"Chance for you to be marked as an aggressor (to be attacked) when you break any block in a village\n" +
			"that is not on the \"block_blacklist\" while your reputation is low enough."
		);
		
		public final int BLOCK_ATTACK_LIMIT = prop(
			"block_aggression_limit", -5,
			"The \"block_aggression_chance\" and \"block_treasured_aggression_chance\" only trigger if your reputation\n" +
			"in the village is less than or equal to this limit.",
			R_INT_VILLAGE_REP
		);
		
		public final TargetBlock.TargetMap BLOCK_BLACKLIST = prop(
			"block_blacklist", buildDefaultBlacklistTargets( ),
			"Specific blocks that will NOT anger villagers when broken."
		);
		
		public final double BLOCK_REP_CHANCE = prop(
			"block_rep_loss_chance", 0.15,
			"Chance for you to lose 1 reputation when you break any block in a village that is not on the\n" +
			"\"block_blacklist\" while your reputation is low enough."
		);
		
		public final int BLOCK_REP_LIMIT = prop(
			"block_rep_loss_limit", 8,
			"The \"block_rep_loss_chance\" and \"block_treasured_rep_loss_chance\" only trigger if your reputation\n" +
			"in the village is equal to or less than this limit.",
			R_INT_VILLAGE_REP
		);
		
		public final double TREASURED_ATTACK_CHANCE = prop(
			"block_treasured_aggression_chance", 1.0,
			"Chance for you to be marked as an aggressor (to be attacked) when you break a block in a village that\n" +
			"is on the \"block_treasured_list\" while your reputation is low enough."
		);
		
		public final TargetBlock.TargetMap TREASURED_LIST = prop(
			"block_treasured_list", new TargetBlock[] {
				new TargetBlock( Blocks.EMERALD_BLOCK ), new TargetBlock( Blocks.BOOKSHELF ),
				new TargetBlock( Blocks.FARMLAND ), new TargetBlock( Blocks.CAKE )
			},
			"Specific blocks that use separate chances for aggression and rep loss from other blocks."
		);
		
		public final double TREASURED_REP_CHANCE = prop(
			"block_treasured_rep_loss_chance", 1.0,
			"Chance for you to lose 1 reputation when you break a block in a village that is on the \"block_treasured_list\"\n" +
			"while your reputation is low enough."
		);
		
		public final TargetBlock.TargetMap BLOCK_WHITELIST = prop(
			"block_whitelist", new TargetBlock[ 0 ],
			"Specific blocks that WILL aggro villagers when broken. If any blocks are specified here, they will\n" +
			"then be the only blocks that anger villagers (i.e., trigger reputation loss and aggression)."
		);
		
		public final boolean COMMAND_INCLUDE_CENTER = prop(
			"command_include_center", true,
			"If true, the \"/villageinfo\" command will state the village center position.\n" +
			"As the command has unlimited search radius, disable this to prevent players from using the command\n" +
			"to easily find the nearest village."
		);
		
		public final double HELP_REP_CHANCE = prop(
			"help_rep_chance", 0.2,
			"Chance for you to earn 1 reputation for each monster killed near a village. The only reasonable way\n" +
			"to restore rep from " + ReputationHandler.REPUTATION_HATED + " or lower with \"villagers_defend\" enabled."
		);
		
		public final boolean HOUSE_REP = prop(
			"house_rep", true,
			"If true, all players known to a village will gain 1 rep when a new house is added to a village and\n" +
			"lose 1 rep when a house is lost. Highly recommended to keep \"refresh_houses\" enabled when this is."
		);
		
		public final boolean NAME_VILLAGERS = prop(
			"name_villagers", true,
			"If true, all villagers will spawn with randomized names based on their profession and career."
		);
		
		public final boolean NAME_VILLAGERS_ALWAYS_SHOW = prop(
			"name_villagers_always_show", true,
			"If true, villager names will be marked as \'always shown\' so that you do not need to mouse over them\n" +
			"to see their names. Also makes them a little easier to keep track of."
		);
		
		public final boolean REFRESH_HOUSES = prop(
			"refresh_houses", true,
			"If true, houses will stay a part of a village permanently once added (until their doors are destroyed\n" +
			"or all villagers in the village are killed), instead of being constantly added/removed as villagers move around."
		);
		
		public final boolean REPUTATION_PARTICLES = prop(
			"reputation_particles", true,
			"If true, particle effects will play when players lose or gain village reputation."
		);
		
		public final boolean REPUTATION_SOUNDS = prop(
			"reputation_sounds", true,
			"If true, villager sound effects will play when players lose or gain village reputation."
		);
		
		public final boolean VILLAGERS_DEFEND = prop(
			"villagers_defend", true,
			"If true, villagers will defend their village by attacking its aggressors and players with\n" +
			"\'hated\' standing (reputation <= " + ReputationHandler.REPUTATION_HATED + "), just like their iron golems do in vanilla."
		);
		
		private
		TargetBlock[] buildDefaultBlacklistTargets( )
		{
			List< TargetBlock > targets = new ArrayList<>( );
			for( Block block : Block.REGISTRY ) {
				// Natural blocks
				if( block instanceof BlockGrass || block instanceof BlockMycelium ||
				    block instanceof BlockStone || block instanceof BlockDirt || block instanceof BlockSand ||
				    block instanceof BlockPumpkin || block instanceof BlockMelon || block instanceof BlockHugeMushroom ||
				    block instanceof BlockSnow || block instanceof BlockIce || block instanceof BlockPackedIce ) {
					targets.add( new TargetBlock( block ) );
				}
				// Plant blocks
				else if( block instanceof IGrowable || block instanceof IPlantable || block instanceof IShearable ) {
					targets.add( new TargetBlock( block ) );
				}
				// Utility blocks
				else if( block instanceof BlockRailBase || block instanceof BlockShulkerBox ) {
					targets.add( new TargetBlock( block ) );
				}
				// Door blocks (reputation for these is handled elsewhere)
				else if( block instanceof BlockDoor || block instanceof BlockFenceGate ) {
					targets.add( new TargetBlock( block ) );
				}
				// Simple instant-break blocks
				else if( block != Blocks.AIR ) {
					try {
						//noinspection deprecation
						if( block.getDefaultState( ).getBlockHardness( null, BlockPos.ORIGIN ) == 0.0F &&
						    block.getDefaultState( ).getLightValue( ) < 4 ) {
							targets.add( new TargetBlock( block ) );
						}
					}
					catch( Exception ignore ) { }
				}
			}
			return targets.toArray( new TargetBlock[ 0 ] );
		}
	}
	
	
	// Contains basic implementations for all config option types, along with some useful constants.
	@SuppressWarnings( { "unused", "WeakerAccess", "SameParameterValue" } )
	private static abstract
	class PropertyCategory
	{
		/** Range: { -INF, INF } */
		static final double[] R_DBL_ALL = { Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY };
		/** Range: { 0.0, INF } */
		static final double[] R_DBL_POS = { 0.0, Double.POSITIVE_INFINITY };
		/** Range: { 0.0, 1.0 } */
		static final double[] R_DBL_ONE = { 0.0, 1.0 };
		
		/** Range: { -INF, INF } */
		static final float[] R_FLT_ALL = { Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY };
		/** Range: { 0.0, INF } */
		static final float[] R_FLT_POS = { 0.0F, Float.POSITIVE_INFINITY };
		/** Range: { 0.0, 1.0 } */
		static final float[] R_FLT_ONE = { 0.0F, 1.0F };
		
		/** Range: { MIN, MAX } */
		static final int[] R_INT_ALL       = { Integer.MIN_VALUE, Integer.MAX_VALUE };
		/** Range: { -1, MAX } */
		static final int[] R_INT_TOKEN_NEG = { -1, Integer.MAX_VALUE };
		/** Range: { 0, MAX } */
		static final int[] R_INT_POS0      = { 0, Integer.MAX_VALUE };
		/** Range: { 1, MAX } */
		static final int[] R_INT_POS1      = { 1, Integer.MAX_VALUE };
		/** Range: { 0, SRT } */
		static final int[] R_INT_SRT_POS   = { 0, Short.MAX_VALUE };
		/** Range: { 0, 255 } */
		static final int[] R_INT_BYT_UNS   = { 0, 0xff };
		/** Range: { 0, 127 } */
		static final int[] R_INT_BYT_POS   = { 0, Byte.MAX_VALUE };
		
		// Support for dynamically generated config categories.
		protected final String KEY;
		
		PropertyCategory( String key )
		{
			KEY = key;
			Config.configLoading.addCustomCategoryComment( name( ), comment( ) );
		}
		
		PropertyCategory( )
		{
			this( null );
		}
		
		abstract
		String name( );
		
		abstract
		String comment( );
		
		double[] defaultDblRange( )
		{
			return PropertyCategory.R_DBL_POS;
		}
		
		float[] defaultFltRange( )
		{
			return PropertyCategory.R_FLT_POS;
		}
		
		int[] defaultIntRange( )
		{
			return PropertyCategory.R_INT_POS0;
		}
		
		IBlockState prop( String key, IBlockState defaultValue, String comment )
		{
			String      target     = cprop( key, defaultValue, comment ).getString( );
			IBlockState blockState = TargetBlock.parseStateForMatch( target );
			
			// Fall back to old style
			if( blockState.getBlock( ) == Blocks.AIR ) {
				String[] pair = target.split( " ", 2 );
				if( pair.length > 1 ) {
					Block block = TargetBlock.getStringAsBlock( pair[ 0 ] );
					//noinspection deprecation // Meta will be replaced by block states in the future. Ignore this for now.
					return block.getStateFromMeta( Integer.parseInt( pair[ 1 ].trim( ) ) );
				}
			}
			return blockState;
		}
		
		Property cprop( String key, IBlockState defaultValue, String comment )
		{
			String defaultId = Block.REGISTRY.getNameForObject( defaultValue.getBlock( ) ).toString( )
			                   + " " + defaultValue.getBlock( ).getMetaFromState( defaultValue );
			comment = amendComment( comment, "Block", defaultId, "mod_id:block_id, mod_id:block_id[<properties>]" );
			return Config.configLoading.get( name( ), key, defaultId, comment );
		}
		
		TargetBlock.TargetMap prop( String key, TargetBlock[] defaultValues, String comment )
		{
			return TargetBlock.newTargetDefinition( cprop( key, defaultValues, comment ).getStringList( ) );
		}
		
		Property cprop( String key, TargetBlock[] defaultValues, String comment )
		{
			String[] defaultIds = new String[ defaultValues.length ];
			for( int i = 0; i < defaultIds.length; i++ ) {
				defaultIds[ i ] = defaultValues[ i ].toString( );
			}
			comment = amendComment( comment, "Block_Array", defaultIds, "mod_id:block_id, mod_id:block_id[<properties>], mod_id:*" );
			return Config.configLoading.get( name( ), key, defaultIds, comment );
		}
		
		EntityListConfig prop( String key, EntryEntity[] defaultValues, String comment )
		{
			return new EntityListConfig( cprop( key, defaultValues, comment ).getStringList( ) );
		}
		
		Property cprop( String key, EntryEntity[] defaultValues, String comment )
		{
			String[] defaultIds = new String[ defaultValues.length ];
			for( int i = 0; i < defaultIds.length; i++ ) {
				defaultIds[ i ] = defaultValues[ i ].toString( );
			}
			comment = amendComment( comment, "Entity_Array", defaultIds, "entity_id <extra_data>, ~entity_id <extra_data>" );
			return Config.configLoading.get( name( ), key, defaultIds, comment );
		}
		
		boolean prop( String key, boolean defaultValue, String comment )
		{
			return cprop( key, defaultValue, comment ).getBoolean( );
		}
		
		Property cprop( String key, boolean defaultValue, String comment )
		{
			comment = amendComment( comment, "Boolean", defaultValue, new Object[] { true, false } );
			return Config.configLoading.get( name( ), key, defaultValue, comment );
		}
		
		boolean[] prop( String key, boolean[] defaultValues, String comment )
		{
			return cprop( key, defaultValues, comment ).getBooleanList( );
		}
		
		Property cprop( String key, boolean[] defaultValues, String comment )
		{
			comment = amendComment( comment, "Boolean_Array", ArrayUtils.toObject( defaultValues ), new Object[] { true, false } );
			return Config.configLoading.get( name( ), key, defaultValues, comment );
		}
		
		int prop( String key, int defaultValue, String comment )
		{
			return cprop( key, defaultValue, comment ).getInt( );
		}
		
		int prop( String key, int defaultValue, String comment, int... range )
		{
			return cprop( key, defaultValue, comment, range ).getInt( );
		}
		
		Property cprop( String key, int defaultValue, String comment )
		{
			return cprop( key, defaultValue, comment, defaultIntRange( ) );
		}
		
		Property cprop( String key, int defaultValue, String comment, int... range )
		{
			comment = amendComment( comment, "Integer", defaultValue, range[ 0 ], range[ 1 ] );
			return Config.configLoading.get( name( ), key, defaultValue, comment, range[ 0 ], range[ 1 ] );
		}
		
		int[] prop( String key, int[] defaultValues, String comment )
		{
			return cprop( key, defaultValues, comment ).getIntList( );
		}
		
		int[] prop( String key, int[] defaultValues, String comment, int... range )
		{
			return cprop( key, defaultValues, comment, range ).getIntList( );
		}
		
		Property cprop( String key, int[] defaultValues, String comment )
		{
			return cprop( key, defaultValues, comment, defaultIntRange( ) );
		}
		
		Property cprop( String key, int[] defaultValues, String comment, int... range )
		{
			comment = amendComment( comment, "Integer_Array", ArrayUtils.toObject( defaultValues ), range[ 0 ], range[ 1 ] );
			return Config.configLoading.get( name( ), key, defaultValues, comment, range[ 0 ], range[ 1 ] );
		}
		
		float prop( String key, float defaultValue, String comment )
		{
			return (float) cprop( key, defaultValue, comment ).getDouble( );
		}
		
		float prop( String key, float defaultValue, String comment, float... range )
		{
			return (float) cprop( key, defaultValue, comment, range ).getDouble( );
		}
		
		Property cprop( String key, float defaultValue, String comment )
		{
			return cprop( key, defaultValue, comment, defaultFltRange( ) );
		}
		
		Property cprop( String key, float defaultValue, String comment, float... range )
		{
			comment = amendComment( comment, "Float", defaultValue, range[ 0 ], range[ 1 ] );
			return Config.configLoading.get( name( ), key, prettyFloatToDouble( defaultValue ), comment, prettyFloatToDouble( range[ 0 ] ), prettyFloatToDouble( range[ 1 ] ) );
		}
		
		double prop( String key, double defaultValue, String comment )
		{
			return cprop( key, defaultValue, comment ).getDouble( );
		}
		
		double prop( String key, double defaultValue, String comment, double... range )
		{
			return cprop( key, defaultValue, comment, range ).getDouble( );
		}
		
		Property cprop( String key, double defaultValue, String comment )
		{
			return cprop( key, defaultValue, comment, defaultDblRange( ) );
		}
		
		Property cprop( String key, double defaultValue, String comment, double... range )
		{
			comment = amendComment( comment, "Double", defaultValue, range[ 0 ], range[ 1 ] );
			return Config.configLoading.get( name( ), key, defaultValue, comment, range[ 0 ], range[ 1 ] );
		}
		
		double[] prop( String key, double[] defaultValues, String comment )
		{
			return cprop( key, defaultValues, comment ).getDoubleList( );
		}
		
		double[] prop( String key, double[] defaultValues, String comment, double... range )
		{
			return cprop( key, defaultValues, comment, range ).getDoubleList( );
		}
		
		Property cprop( String key, double[] defaultValues, String comment )
		{
			return cprop( key, defaultValues, comment, defaultDblRange( ) );
		}
		
		Property cprop( String key, double[] defaultValues, String comment, double... range )
		{
			comment = amendComment( comment, "Double_Array", ArrayUtils.toObject( defaultValues ), range[ 0 ], range[ 1 ] );
			return Config.configLoading.get( name( ), key, defaultValues, comment, range[ 0 ], range[ 1 ] );
		}
		
		String prop( String key, String defaultValue, String comment, String valueDescription )
		{
			return cprop( key, defaultValue, comment, valueDescription ).getString( );
		}
		
		String prop( String key, String defaultValue, String comment, String... validValues )
		{
			return cprop( key, defaultValue, comment, validValues ).getString( );
		}
		
		Property cprop( String key, String defaultValue, String comment, String valueDescription )
		{
			comment = amendComment( comment, "String", defaultValue, valueDescription );
			return Config.configLoading.get( name( ), key, defaultValue, comment, new String[ 0 ] );
		}
		
		Property cprop( String key, String defaultValue, String comment, String... validValues )
		{
			comment = amendComment( comment, "String", defaultValue, validValues );
			return Config.configLoading.get( name( ), key, defaultValue, comment, validValues );
		}
		
		private
		String amendComment( String comment, String type, Object[] defaultValues, String description )
		{
			return amendComment( comment, type, "{ " + toReadable( defaultValues ) + " }", description );
		}
		
		private
		String amendComment( String comment, String type, Object[] defaultValues, Object min, Object max )
		{
			return amendComment( comment, type, "{ " + toReadable( defaultValues ) + " }", min, max );
		}
		
		private
		String amendComment( String comment, String type, Object[] defaultValues, Object[] validValues )
		{
			return amendComment( comment, type, "{ " + toReadable( defaultValues ) + " }", validValues );
		}
		
		private
		String amendComment( String comment, String type, Object defaultValue, String description )
		{
			return comment + "\n   >> " + type + ":[ Value={ " + description + " }, Default=" + defaultValue + " ]";
		}
		
		private
		String amendComment( String comment, String type, Object defaultValue, Object min, Object max )
		{
			return comment + "\n   >> " + type + ":[ Range={ " + min + ", " + max + " }, Default=" + defaultValue + " ]";
		}
		
		private
		String amendComment( String comment, String type, Object defaultValue, Object[] validValues )
		{
			if( validValues.length < 2 )
				throw new IllegalArgumentException( "Attempted to create config with no options!" );
			
			return comment + "\n   >> " + type + ":[ Valid_Values={ " + toReadable( validValues ) + " }, Default=" + defaultValue + " ]";
		}
		
		private
		double prettyFloatToDouble( float f )
		{
			return Double.parseDouble( Float.toString( f ) );
		}
		
		private
		String toReadable( Object[] array )
		{
			if( array.length <= 0 )
				return "";
			
			StringBuilder commentBuilder = new StringBuilder( );
			for( Object value : array ) {
				commentBuilder.append( value ).append( ", " );
			}
			return commentBuilder.substring( 0, commentBuilder.length( ) - 2 );
		}
	}
}
