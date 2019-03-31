package fathertoast.specialai.ai;

import fathertoast.specialai.*;
import fathertoast.specialai.ai.elite.*;
import fathertoast.specialai.ai.grief.*;
import fathertoast.specialai.ai.react.*;
import fathertoast.specialai.config.*;
import fathertoast.specialai.village.*;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.*;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.pathfinding.PathNavigateGround;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.List;
import java.util.UUID;

@SuppressWarnings( "WeakerAccess" )
public
class AIHandler
{
	public static final int NBT_TYPE_PRIMITIVE = 99;
	
	// NBT tags used to store info about this mod's AI.
	private static final String AVOID_EXPLOSIONS_TAG = "AvoidExplosions";
	private static final String DODGE_ARROWS_TAG     = "DodgeArrows";
	private static final String DODGE_ARROW_INIT_TAG = "SAIArrowDodgeCheck";
	
	private static final String DEFEND_VILLAGE_TAG = "DefendVillage";
	private static final String DEPACIFY_TAG       = "Depacify";
	
	public static final String DOOR_BREAK_TAG        = "DoorBreaking";
	public static final String DOOR_BREAK_SPEED      = "DoorBreakSpeed";
	public static final String DOOR_BREAK_TARGET_TAG = "DoorNeedsTarget";
	public static final String DOOR_BREAK_TOOL_TAG   = "DoorNeedsTool";
	public static final String DOOR_BREAK_BLOCK_TAG  = "DoorBlocks";
	
	public static final String IDLE_RANGE_XZ_TAG = "IdleScanRangeXZ";
	public static final String IDLE_RANGE_Y_TAG  = "IdleScanRangeY";
	
	public static final String GRIEF_TAG           = "Griefing";
	public static final String GRIEF_BREAK_SPEED   = "GriefBreakSpeed";
	public static final String GRIEF_TOOL_TAG      = "GriefNeedsTool";
	public static final String GRIEF_LIGHT_TAG     = "GriefLights";
	public static final String GRIEF_BLOCK_TAG     = "GriefBlocks";
	public static final String GRIEF_LOOTABLE_TAG  = "GriefLootable";
	public static final String GRIEF_EXCEPTION_TAG = "GriefBlacklist";
	
	public static final String FIDDLE_TAG           = "Fiddling";
	public static final String FIDDLE_BLOCK_TAG     = "FiddleBlocks";
	public static final String FIDDLE_EXCEPTION_TAG = "FiddleBlacklist";
	
	private static final String RIDER_TAG = "Rider";
	
	private static final String ELITE_AI_TAG   = "EliteAI";
	private static final String FORCE_INIT_TAG = "ForceInit";
	
	/* Mutex Bits:
	 * AIs may only run concurrently if they share no "mutex bits".
	 * Therefore, AIs with 0 for their "mutex" number may always run with any other AI.
	 * Use bitwise OR to combine multiple bits. */
	public static final byte BIT_NONE     = 0b0000;
	public static final byte BIT_MOVEMENT = 0b0001;
	public static final byte BIT_FACING   = 0b0010;
	public static final byte BIT_SWIMMING = 0b0100;
	
	/* The "mutex bit" used by all targeting tasks so that none of them run at the same time. */
	public static final byte TARGET_BIT = 0b0001;
	
	private static int scansLeft = Config.get( ).IDLE_AI.SCAN_COUNT_GLOBAL;
	
	// Decrements the number of scans left and returns true if a scan can be made.
	public static
	boolean canScan( )
	{
		return Config.get( ).IDLE_AI.SCAN_COUNT_GLOBAL <= 0 || scansLeft-- > 0;
	}
	
	// Clears the entity's AI tasks.
	@SuppressWarnings( "unused" )
	private static
	void clearAI( EntityLiving entity )
	{
		for( EntityAITasks.EntityAITaskEntry entry : entity.tasks.taskEntries.toArray( new EntityAITasks.EntityAITaskEntry[ 0 ] ) ) {
			entity.tasks.removeTask( entry.action );
		}
	}
	
	// Clears the entity's AI target tasks.
	@SuppressWarnings( "unused" )
	private static
	void clearTargetAI( EntityLiving entity )
	{
		for( EntityAITasks.EntityAITaskEntry entry : entity.targetTasks.taskEntries.toArray( new EntityAITasks.EntityAITaskEntry[ 0 ] ) ) {
			entity.targetTasks.removeTask( entry.action );
		}
	}
	
	// Adds avoid explosions AI to the entity.
	private static
	void addAvoidExplosionsAI( EntityCreature entity )
	{
		entity.tasks.addTask( -1, new EntityAIAvoidExplosions( entity ) );
	}
	
	// Adds avoid explosions AI to the entity.
	private static
	void addDodgeArrowsAI( EntityCreature entity, float dodgeChance )
	{
		entity.tasks.addTask( -1, new EntityAIDodgeArrows( entity, dodgeChance ) );
	}
	
	// Adds defend village AI to the mob.
	private static
	void addDefendVillageAI( EntityCreature entity, boolean addedAttackAI )
	{
		if( !addedAttackAI ) {
			entity.tasks.addTask( 0, new EntityAIAttackMeleePassive( entity, 0.7, false ) );
		}
		entity.targetTasks.addTask( 0, new EntityAIVillagerDefendVillage( entity ) );
	}
	
	// Adds hurt by target AI to the mob.
	private static
	void addHurtByTargetAI( EntityCreature entity, byte depacify, boolean addedAttackAI )
	{
		if( !addedAttackAI ) {
			entity.tasks.addTask( 0, new EntityAIAttackMeleePassive( entity, entity instanceof EntityChicken ? 1.8 : 1.4, false ) );
		}
		entity.targetTasks.addTask( 0, new EntityAIHurtByTarget( entity, Config.get( ).REACT_AI.CALL_HELP ) );
		if( depacify > 1 ) {
			entity.targetTasks.addTask( 1, new EntityAINearestAttackableTarget<>( entity, EntityPlayer.class, true ) );
		}
	}
	
	// Sets the entity's "call for help" to true.
	private static
	void setHelpAI( EntityCreature entity )
	{
		for( EntityAITasks.EntityAITaskEntry entry : entity.targetTasks.taskEntries.toArray( new EntityAITasks.EntityAITaskEntry[ 0 ] ) ) {
			if( entry.action.getClass( ) == EntityAIHurtByTarget.class ) {
				int priority = entry.priority;
				entity.targetTasks.removeTask( entry.action );
				entity.targetTasks.addTask( priority, new EntityAIHurtByTarget( entity, true ) );
				return;
			}
		}
	}
	
	// Gives the entity rider AI.
	private static
	void addRiderAI( EntityLiving entity, boolean small )
	{
		entity.tasks.addTask( getPassivePriority( entity ), new EntityAIRider( entity, small ) );
	}
	
	// Adds the passive griefing AI to the same priority as the mob's wandering AI.
	private static
	void addEatingAI( EntityAnimal entity )
	{
		entity.tasks.addTask( getPassivePriority( entity ), new EntityAIEatBreedingItem( entity ) );
	}
	
	// Adds the passive griefing AI to the same priority as the mob's wandering AI.
	private static
	void addGriefAI( EntityLiving entity, boolean griefing, boolean fiddling, NBTTagCompound tag )
	{
		entity.tasks.addTask( getPassivePriority( entity ), new EntityAIGriefBlocks( entity, griefing, fiddling, tag ) );
	}
	
	// Returns the priority to assign to an idle AI.
	private static
	int getPassivePriority( EntityLiving entity )
	{
		int highest = Integer.MIN_VALUE;
		for( EntityAITasks.EntityAITaskEntry entry : entity.tasks.taskEntries.toArray( new EntityAITasks.EntityAITaskEntry[ 0 ] ) ) {
			if( entry.action instanceof EntityAIWander || entry.action instanceof EntityAIWatchClosest || entry.action instanceof EntityAILookIdle )
				return entry.priority;
			if( highest < entry.priority ) {
				highest = entry.priority;
			}
		}
		return highest + 1;
	}
	
	// Gives the entity digging AI.
	@SuppressWarnings( "unused" )
	// NYI
	private static
	void addDigAI( EntityLiving entity )
	{
		//entity.tasks.addTask(0, new EntityAIDig(entity));
	}
	
	// Gives the entity door-breaking AI.
	private static
	void addDoorBreakAI( EntityLiving entity, NBTTagCompound tag )
	{
		if( entity.getNavigator( ) instanceof PathNavigateGround ) {
			// Remove any pre-existing door-breaking ai
			for( EntityAITasks.EntityAITaskEntry entry : entity.tasks.taskEntries.toArray( new EntityAITasks.EntityAITaskEntry[ 0 ] ) ) {
				if( entry.action instanceof EntityAIBreakDoor ) {
					entity.tasks.removeTask( entry.action );
				}
			}
			
			// Add the new ai
			((PathNavigateGround) entity.getNavigator( )).setBreakDoors( true );
			entity.tasks.addTask( 1, new EntityAIBreakDoorSpecial( entity, tag ) );
		}
		else {
			SpecialAIMod.log( ).warn(
				"Attempted to add door-breaking ai to entity '{}' with incompatible navigator '{}'",
				EntityList.getKey( entity ), entity.getNavigator( ).getClass( ).getSimpleName( )
			);
		}
	}
	
	/**
	 * Called each tick.
	 * TickEvent.Type type = the type of tick.
	 * Side side = the side this tick is on.
	 * TickEvent.Phase phase = the phase of this tick (START, END).
	 *
	 * @param event The event being triggered.
	 */
	@SubscribeEvent( priority = EventPriority.NORMAL )
	public
	void onServerTick( TickEvent.ServerTickEvent event )
	{
		if( event.phase == TickEvent.Phase.END ) {
			scansLeft = Config.get( ).IDLE_AI.SCAN_COUNT_GLOBAL;
		}
	}
	
	/**
	 * Called each tick for each world.
	 * TickEvent.Type type = the type of tick.
	 * Side side = the side this tick is on.
	 * TickEvent.Phase phase = the phase of this tick (START, END).
	 * World world = the world that is ticking.
	 *
	 * @param event The event being triggered.
	 */
	@SubscribeEvent( priority = EventPriority.NORMAL )
	public
	void onWorldTick( TickEvent.WorldTickEvent event )
	{
		if( event.world != null && event.phase == TickEvent.Phase.END ) {
			Entity entity;
			for( int i = 0; i < event.world.loadedEntityList.size( ); i++ ) {
				entity = event.world.loadedEntityList.get( i );
				if( entity instanceof EntityItem ) {
					equipToThief( (EntityItem) entity );
				}
			}
		}
	}
	
	private
	void equipToThief( EntityItem item )
	{
		final String TAG = "ThiefUUID";
		if( item.getEntityData( ).hasUniqueId( TAG ) ) {
			UUID   thiefId = item.getEntityData( ).getUniqueId( TAG );
			Entity entity;
			for( int i = 0; i < item.world.loadedEntityList.size( ); i++ ) {
				entity = item.world.loadedEntityList.get( i );
				if( entity instanceof EntityLiving && thiefId.equals( entity.getUniqueID( ) ) ) {
					entity.setItemStackToSlot( EntityEquipmentSlot.MAINHAND, item.getItem( ) );
					((EntityLiving) entity).setDropChance( EntityEquipmentSlot.MAINHAND, 2.0F );
					((EntityLiving) entity).enablePersistence( );
					item.setDead( );
					return;
				}
			}
			item.getEntityData( ).removeTag( TAG + "Most" );
			item.getEntityData( ).removeTag( TAG + "Least" );
		}
	}
	
	/**
	 * Called by World.spawnEntityInWorld().
	 * Entity entity = the entity joining the world.
	 * World world = the world the entity is joining.
	 *
	 * @param event The event being triggered.
	 */
	@SubscribeEvent( priority = EventPriority.LOW )
	public
	void onEntityJoinWorld( EntityJoinWorldEvent event )
	{
		if( event.getWorld( ).isRemote )
			return;
		
		if( event.getEntity( ) instanceof EntityArrow && !event.getEntity( ).getEntityData( ).getBoolean( DODGE_ARROW_INIT_TAG ) ) {
			event.getEntity( ).getEntityData( ).setBoolean( DODGE_ARROW_INIT_TAG, true );
			EntityAIDodgeArrows.doDodgeCheckForArrow( event.getEntity( ) );
		}
		
		if( !(event.getEntity( ) instanceof EntityLiving) )
			return;
		
		EntityLiving   theEntity = (EntityLiving) event.getEntity( );
		NBTTagCompound tag       = SpecialAIMod.getTag( theEntity );
		
		if( theEntity instanceof EntityCreature ) {
			// So we don't add multiple attack AIs
			boolean addedAttackAI = false;
			
			// Avoid explosions
			if( !tag.hasKey( AVOID_EXPLOSIONS_TAG, NBT_TYPE_PRIMITIVE ) ) {
				tag.setBoolean( AVOID_EXPLOSIONS_TAG, Config.get( ).REACT_AI.AVOID_EXPLOSIONS );
			}
			if( tag.getBoolean( AVOID_EXPLOSIONS_TAG ) ) {
				addAvoidExplosionsAI( (EntityCreature) theEntity );
			}
			
			// Dodge arrows
			if( !tag.hasKey( DODGE_ARROWS_TAG, NBT_TYPE_PRIMITIVE ) ) {
				tag.setFloat( DODGE_ARROWS_TAG, (float) Config.get( ).REACT_AI.DODGE_ARROWS );
			}
			if( tag.getFloat( DODGE_ARROWS_TAG ) > 0.0F ) {
				addDodgeArrowsAI( (EntityCreature) theEntity, tag.getFloat( DODGE_ARROWS_TAG ) );
			}
			
			// Defend village
			if( !tag.hasKey( DEFEND_VILLAGE_TAG, NBT_TYPE_PRIMITIVE ) ) {
				tag.setBoolean( DEFEND_VILLAGE_TAG, Config.get( ).VILLAGES.VILLAGERS_DEFEND && theEntity instanceof EntityVillager );
			}
			if( tag.getBoolean( DEFEND_VILLAGE_TAG ) ) {
				addDefendVillageAI( (EntityCreature) theEntity, addedAttackAI );
				addedAttackAI = true;
			}
			
			// Depacify
			byte depacify;
			if( tag.hasKey( DEPACIFY_TAG, NBT_TYPE_PRIMITIVE ) ) {
				depacify = tag.getByte( DEPACIFY_TAG );
			}
			else {
				if( Config.get( ).GENERAL.DEPACIFY_LIST.rollChance( theEntity ) ) {
					if( theEntity.getRNG( ).nextDouble( ) < Config.get( ).GENERAL.AGGRESSIVE_CHANCE ) {
						depacify = 2;
					}
					else {
						depacify = 1;
					}
				}
				else {
					depacify = 0;
				}
			}
			if( depacify > 0 ) {
				addHurtByTargetAI( (EntityCreature) theEntity, depacify, addedAttackAI );
				addedAttackAI = true;
			}
			// Call for help (already covered if depacified)
			else if( Config.get( ).REACT_AI.CALL_HELP ) {
				setHelpAI( (EntityCreature) theEntity );
			}
		}
		
		// Rider
		boolean small = Config.get( ).JOCKEYS.RIDER_LIST_SMALL.contains( theEntity );
		if( !tag.hasKey( RIDER_TAG, NBT_TYPE_PRIMITIVE ) ) {
			EntityListConfig list;
			if( small ) {
				list = Config.get( ).JOCKEYS.RIDER_LIST_SMALL;
			}
			else {
				list = Config.get( ).JOCKEYS.RIDER_LIST;
			}
			tag.setBoolean( RIDER_TAG, list.rollChance( theEntity ) );
		}
		if( tag.getBoolean( RIDER_TAG ) ) {
			addRiderAI( theEntity, small );
		}
		
		// Eat breeding items
		if( Config.get( ).GENERAL.EAT_BREEDING_ITEMS && theEntity instanceof EntityAnimal ) {
			addEatingAI( (EntityAnimal) theEntity );
		}
		
		// Passive griefing
		if( !tag.hasKey( GRIEF_TAG, NBT_TYPE_PRIMITIVE ) ) {
			tag.setBoolean( GRIEF_TAG, Config.get( ).GRIEFING.MOB_LIST.rollChance( theEntity ) );
		}
		if( !tag.hasKey( FIDDLE_TAG, NBT_TYPE_PRIMITIVE ) ) {
			tag.setBoolean( FIDDLE_TAG, Config.get( ).FIDDLING.MOB_LIST.rollChance( theEntity ) );
		}
		final boolean griefing = Config.get( ).GRIEFING.ENABLED && tag.getBoolean( GRIEF_TAG );
		final boolean fiddling = Config.get( ).FIDDLING.ENABLED && tag.getBoolean( FIDDLE_TAG );
		if( griefing || fiddling ) {
			addGriefAI( theEntity, griefing, fiddling, tag );
		}

        /* WIP
        // Digging
        if (event.entity instanceof EntityZombie) {
            addDigAI(theEntity);
        }
         */
		
		// Door-breaking AI
		if( !tag.hasKey( DOOR_BREAK_TAG, NBT_TYPE_PRIMITIVE ) ) {
			tag.setBoolean( DOOR_BREAK_TAG, Config.get( ).DOOR_BREAKING.MOB_LIST.rollChance( theEntity ) );
		}
		if( Config.get( ).DOOR_BREAKING.ENABLED && tag.getBoolean( DOOR_BREAK_TAG ) ) {
			addDoorBreakAI( theEntity, tag );
		}
		
		// Elite AI
		NBTTagCompound aiTag;
		if( !tag.hasKey( ELITE_AI_TAG, tag.getId( ) ) ) {
			aiTag = new NBTTagCompound( );
			tag.setTag( ELITE_AI_TAG, aiTag );
			
			// Apply new AI(s), if needed
			float[] chances = Config.get( ).ELITE_AI.MOB_LIST.getChances( theEntity );
			if( chances != null ) {
				for( float chance : chances ) {
					if( chance > 0.0F && theEntity.getRNG( ).nextFloat( ) < chance ) {
						EliteAIHandler.saveSpecialAI( theEntity, aiTag );
					}
				}
			}
			
			// Mark this entity to init, if not already forced
			if( !tag.hasKey( FORCE_INIT_TAG, NBT_TYPE_PRIMITIVE ) ) {
				tag.setBoolean( FORCE_INIT_TAG, true );
			}
		}
		else {
			aiTag = tag.getCompoundTag( ELITE_AI_TAG );
		}
		EliteAIHandler.addSpecialAI( theEntity, aiTag, tag.getBoolean( FORCE_INIT_TAG ) );
		tag.removeTag( FORCE_INIT_TAG );
	}
	
	/**
	 * Called by EntityLivingBase.onDeath().
	 * EntityLivingBase entityLiving = the entity dying.
	 * DamageSource source = the damage source that killed the entity.
	 *
	 * @param event The event being triggered.
	 */
	@SubscribeEvent( priority = EventPriority.NORMAL )
	public
	void onLivingDeath( LivingDeathEvent event )
	{
		// Call for help on death
		if( Config.get( ).REACT_AI.CALL_HELP_ON_DEATH > 0.0 && event.getEntityLiving( ) instanceof EntityLiving &&
		    event.getEntityLiving( ).getRNG( ).nextDouble( ) < Config.get( ).REACT_AI.CALL_HELP_ON_DEATH ) {
			
			EntityLiving theEntity = (EntityLiving) event.getEntityLiving( );
			Entity       target    = event.getSource( ).getTrueSource( );
			if( target instanceof EntityLivingBase ) {
				IAttributeInstance attribute = theEntity.getEntityAttribute( SharedMonsterAttributes.FOLLOW_RANGE );
				double             range     = attribute.getAttributeValue( );
				
				List entities = theEntity.world.getEntitiesWithinAABBExcludingEntity( theEntity, new AxisAlignedBB(
					theEntity.posX, theEntity.posY, theEntity.posZ,
					theEntity.posX + 1.0, theEntity.posY + 1.0, theEntity.posZ + 1.0
				).expand( range, 10.0, range ) );
				
				for( Object entity : entities ) {
					if( entity instanceof EntityLiving && (entity.getClass( ).isAssignableFrom( theEntity.getClass( ) ) || theEntity.getClass( ).isAssignableFrom( entity.getClass( ) )) ) {
						EntityLiving alliedEntity = (EntityLiving) entity;
						if( alliedEntity.getAttackTarget( ) == null && !alliedEntity.isOnSameTeam( target ) ) {
							alliedEntity.setAttackTarget( (EntityLivingBase) target );
						}
					}
				}
			}
		}
	}
}
