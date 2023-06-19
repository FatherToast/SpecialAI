package fathertoast.specialai.ai;


import fathertoast.crust.api.config.common.ConfigUtil;
import fathertoast.crust.api.lib.NBTHelper;
import fathertoast.specialai.SpecialAI;
import fathertoast.specialai.ai.elite.EliteAIHelper;
import fathertoast.specialai.ai.griefing.EatBreedingItemGoal;
import fathertoast.specialai.ai.griefing.IdleActionsGoal;
import fathertoast.specialai.ai.griefing.SpecialBreakDoorGoal;
import fathertoast.specialai.config.Config;
import fathertoast.specialai.config.EliteAIConfig;
import fathertoast.specialai.util.BlockHelper;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.pathfinding.GroundPathNavigator;
import net.minecraft.pathfinding.PathNavigator;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.eventbus.api.Event;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * This class handles game events to 'hook in' this mod's AI patterns or to help manage them outside of the AI tick.
 * <p>
 * The bulk of this class performs the actual application of AI patterns to entities on spawn, and contains most of the
 * logic for application.
 * <p>
 * Additionally, it also uses the server tick to run actions that can't be done during the AI tick.
 */
public final class AIManager {
    // NBT tags used to store info about this mod's AI.
    private static final String TAG_DEPACIFY = "depacify";
    private static final String TAG_AGGRESSIVE = "aggressive";
    
    private static final String TAG_AVOID_EXPLOSIONS = "avoid_explosions";
    private static final String TAG_CALL_FOR_HELP = "call_for_help";
    private static final String TAG_DODGE_ARROWS = "dodge_arrows";
    private static final String TAG_ARROW_DODGE_CHECKED = SpecialAI.MOD_ID + "_dodge_check";
    
    private static final String TAG_RIDER = "rider";
    
    private static final String TAG_DOOR_BREAK = "door_breaking";
    
    private static final String TAG_HIDE = "hiding";
    private static final String TAG_GRIEF = "griefing";
    private static final String TAG_FIDDLE = "fiddling";
    
    public static final String TAG_ELITE_AI = "elite_ai";
    private static final String TAG_FORCE_INIT = "force_init";
    
    /** All actions currently waiting to be performed at the end of the server tick. */
    private static final List<Supplier<Boolean>> TICK_END_ACTIONS = new ArrayList<>();
    
    /** Queues an action to perform at the end of the server tick. Will be called at the end of each tick until it returns 'true'. */
    public static void queue( Supplier<Boolean> action ) { TICK_END_ACTIONS.add( action ); }
    
    /** The number of remaining global block scans that can be performed this server tick. */
    private static int scansLeft = Config.IDLE.GENERAL.scanCountGlobal.get();
    
    /** @return Decrements the number of global scans remaining and returns true if a scan can be made. */
    public static boolean canScan() {
        if( Config.IDLE.GENERAL.scanCountGlobal.get() <= 0 ) return true;
        
        if( scansLeft > 0 ) {
            scansLeft--;
            if( scansLeft == 0 ) {
                SpecialAI.LOG.warn( "Maximum scans reached. If you are getting spammed by this under normal conditions, " +
                        "you should change your scan settings in {}.", ConfigUtil.toRelativePath( Config.IDLE.SPEC.getFile() ) );
            }
            return true;
        }
        return false;
    }
    
    /** @param entity Clears the entity's AI action goals entirely. */
    @SuppressWarnings( "unused" )
    private static void clearActionAI( MobEntity entity ) {
        for( PrioritizedGoal task : new ArrayList<>( entity.goalSelector.availableGoals ) ) {
            entity.goalSelector.removeGoal( task.getGoal() );
        }
    }
    
    /** @param entity Clears the entity's AI target goals entirely. */
    @SuppressWarnings( "unused" )
    private static void clearTargetAI( MobEntity entity ) {
        for( PrioritizedGoal task : new ArrayList<>( entity.targetSelector.availableGoals ) ) {
            entity.targetSelector.removeGoal( task.getGoal() );
        }
    }
    
    /** @param entity Adds dodge arrows AI to the entity. */
    private static void addDodgeArrowsAI( MobEntity entity, double dodgeChance ) {
        entity.goalSelector.addGoal( -1, new DodgeArrowsGoal( entity, dodgeChance ) );
    }
    
    /** @param entity Adds avoid explosions AI to the entity. */
    private static void addAvoidExplosionsAI( CreatureEntity entity, double speedMulti ) {
        entity.goalSelector.addGoal( -1, new AvoidExplosionsGoal( entity, speedMulti ) );
    }
    
    //    /** @param entity Adds defend village AI to the entity, as well as attack AI if needed. */
    //    private static void addDefendVillageTargetAI( VillagerEntity entity ) {
    //        entity.targetSelector.addGoal( 0, new VillagerDefendVillageGoal( entity ) );
    //    }
    
    /** @param entity Adds hurt by target AI to the entity, as well as attack AI if needed. */
    private static void addHurtByTargetAI( CreatureEntity entity ) {
        entity.targetSelector.addGoal( 0, new HurtByTargetGoal( entity ) );
    }
    
    /** @param entity Adds aggressive AI to the entity, as well as attack AI if needed. */
    private static void addAggressiveTargetAI( CreatureEntity entity ) {
        entity.targetSelector.addGoal( 1, new NearestAttackableTargetGoal<>( entity, PlayerEntity.class, true ) );
    }
    
    /** @param entity Adds a melee attack AI to the entity, unless an attack AI is detected. */
    private static void addMeleeAttackAI( CreatureEntity entity ) {
        // Make sure the entity doesn't already have a recognized attack AI
        for( PrioritizedGoal task : new ArrayList<>( entity.goalSelector.availableGoals ) ) {
            if( task.getGoal() instanceof MeleeAttackGoal || task.getGoal() instanceof OcelotAttackGoal ||
                    task.getGoal() instanceof RangedAttackGoal || task.getGoal() instanceof RangedBowAttackGoal || task.getGoal() instanceof RangedCrossbowAttackGoal ) {
                return;
            }
        }
        entity.goalSelector.addGoal( 0, new AnimalMeleeAttackGoal( entity, false ) );
    }
    
    /** @param entity Sets the entity's "call for help" flag to true. */
    private static void setHelpAI( MobEntity entity ) {
        for( PrioritizedGoal task : new ArrayList<>( entity.targetSelector.availableGoals ) ) {
            if( task.getGoal() instanceof HurtByTargetGoal ) {
                ((HurtByTargetGoal) task.getGoal()).setAlertOthers();
                return;
            }
        }
    }
    
    /** @param entity Adds rider AI to the entity. */
    private static void addRiderAI( MobEntity entity, boolean small ) {
        entity.goalSelector.addGoal( getPassivePriority( entity ), new RiderGoal( entity, small ) );
    }
    
    /** @param entity Adds eat breeding items AI to the entity. */
    private static void addEatingAI( AnimalEntity entity ) {
        entity.goalSelector.addGoal( getPassivePriority( entity ), new EatBreedingItemGoal( entity ) );
    }
    
    /** @param entity Adds idle griefing/fiddling AI to the entity, if at least one of the behaviors is enabled. */
    private static void addIdleAI( MobEntity entity, boolean hiding, boolean griefing, boolean fiddling ) {
        if( hiding || griefing || fiddling ) {
            entity.goalSelector.addGoal( getPassivePriority( entity ), new IdleActionsGoal( entity, hiding, griefing, fiddling ) );
        }
    }
    
    /** @return Returns the priority that idle AI patterns should be assigned to. */
    private static int getPassivePriority( MobEntity entity ) {
        if( entity.goalSelector.availableGoals.isEmpty() ) return 6;
        int highest = Integer.MIN_VALUE;
        for( PrioritizedGoal task : new ArrayList<>( entity.goalSelector.availableGoals ) ) {
            if( task.getGoal() instanceof RandomWalkingGoal || task.getGoal() instanceof LookAtGoal || task.getGoal() instanceof LookRandomlyGoal )
                return task.getPriority();
            if( highest < task.getPriority() ) {
                highest = task.getPriority();
            }
        }
        return highest + 1;
    }
    
    /** @param entity Adds digging AI to the entity. */
    @SuppressWarnings( "unused" )
    private static void addDigAI( MobEntity entity ) {
        //entity.goalSelector.addGoal( 0, new DigGoal( entity ) );
    }
    
    /** @param entity Adds door breaking AI to the entity, replacing any pre-existing door breaking AI. */
    private static void addDoorBreakAI( MobEntity entity ) {
        PathNavigator nav = entity.getNavigation();
        if( nav instanceof GroundPathNavigator ) {
            int priority = 1;
            // Remove any pre-existing door-breaking ai
            for( PrioritizedGoal task : new ArrayList<>( entity.goalSelector.availableGoals ) ) {
                if( task.getGoal() instanceof BreakDoorGoal ) {
                    if( task.getPriority() < priority ) {
                        priority = task.getPriority();
                    }
                    entity.goalSelector.removeGoal( task.getGoal() );
                }
            }
            
            // Add the new ai
            ((GroundPathNavigator) nav).setCanOpenDoors( true );
            entity.goalSelector.addGoal( priority, new SpecialBreakDoorGoal( entity ) );
        }
        else {
            SpecialAI.LOG.warn( "Attempted to add door-breaking ai to entity '{}' with incompatible navigator '{}'",
                    SpecialAI.toString( entity.getType() ), nav.getClass().getSimpleName() );
        }
    }
    
    /**
     * Called for the server at the start and end of each tick.
     * <p>
     * It is usually wise to check the phase (start/end) before doing anything.
     *
     * @param event The event data.
     */
    public static void onServerTick( TickEvent.ServerTickEvent event ) {
        if( event.phase == TickEvent.Phase.END ) {
            // Reset the global scan limit
            scansLeft = Config.IDLE.GENERAL.scanCountGlobal.get();
            
            // Run any queued actions
            if( !TICK_END_ACTIONS.isEmpty() ) {
                TICK_END_ACTIONS.removeIf( Supplier::get );
            }
        }
    }
    
    /**
     * Called when any entity is spawned in the world, including by chunk loading and dimension transition.
     *
     * @param event The event data.
     */
    public static void onJoinWorld( EntityJoinWorldEvent event ) {
        // None of this should be done on the client side
        if( event.getWorld().isClientSide() || !event.getEntity().isAlive() ) return;
        
        // Check if this is an arrow that can be dodged
        if( event.getEntity() instanceof ProjectileEntity && !event.getEntity().getPersistentData().getBoolean( TAG_ARROW_DODGE_CHECKED ) ) {
            event.getEntity().getPersistentData().putBoolean( TAG_ARROW_DODGE_CHECKED, true );
            DodgeArrowsGoal.doDodgeCheckForArrow( event.getEntity() );
        }
        
        // Only initialize AI on mob entities, where the base AI system is implemented
        if( event.getEntity() instanceof MobEntity ) {
            initializeSpecialAI( (MobEntity) event.getEntity() );
        }
    }
    
    /**
     * Called when a mob is spawned in the world, including by chunk loading and dimension transition.
     *
     * @param entity The entity to initialize.
     */
    public static void initializeSpecialAI( MobEntity entity ) {
        // The tag all info for this mod is stored on for the entity
        final CompoundNBT tag = NBTHelper.getForgeData( entity, SpecialAI.MOD_ID );
        
        // Dodge arrows
        if( !NBTHelper.containsNumber( tag, TAG_DODGE_ARROWS ) ) {
            final double[] dodgeValues = Config.GENERAL.REACTIONS.dodgeArrowsList.getValues( entity );
            tag.putDouble( TAG_DODGE_ARROWS, dodgeValues != null && entity.getRandom().nextDouble() < dodgeValues[0] ? dodgeValues[1] : 0.0 );
        }
        if( tag.getDouble( TAG_DODGE_ARROWS ) > 0.0F ) {
            addDodgeArrowsAI( entity, tag.getDouble( TAG_DODGE_ARROWS ) );
        }
        
        if( entity instanceof CreatureEntity ) {
            // Set to true any time an attack target AI is added
            boolean needsAttackAI = false;
            
            // Avoid explosions
            if( !NBTHelper.containsNumber( tag, TAG_AVOID_EXPLOSIONS ) ) {
                tag.putDouble( TAG_AVOID_EXPLOSIONS, Config.GENERAL.REACTIONS.avoidExplosionsList.getValue( entity ) );
            }
            if( tag.getDouble( TAG_AVOID_EXPLOSIONS ) > 0.0 ) {
                addAvoidExplosionsAI( (CreatureEntity) entity, tag.getDouble( TAG_AVOID_EXPLOSIONS ) );
            }
            
            // Eat breeding items
            if( Config.GENERAL.ANIMALS.eatBreedingItems.get() && entity instanceof AnimalEntity ) {
                addEatingAI( (AnimalEntity) entity );
            }
            
            // Defend village
            //            if( entity instanceof VillagerEntity ) {
            //                addDefendVillageTargetAI( (VillagerEntity) entity );
            //                needsAttackAI = true;
            //            }
            
            // Depacify
            if( !NBTHelper.containsNumber( tag, TAG_DEPACIFY ) ) {
                tag.putBoolean( TAG_DEPACIFY, Config.GENERAL.ANIMALS.depacifyList.rollChance( entity ) );
            }
            if( tag.getBoolean( TAG_DEPACIFY ) ) {
                addHurtByTargetAI( (CreatureEntity) entity );
                needsAttackAI = true;
            }
            
            // Aggressive
            if( !NBTHelper.containsNumber( tag, TAG_AGGRESSIVE ) ) {
                tag.putBoolean( TAG_AGGRESSIVE, Config.GENERAL.ANIMALS.aggressiveList.rollChance( entity ) );
            }
            if( tag.getBoolean( TAG_AGGRESSIVE ) ) {
                addAggressiveTargetAI( (CreatureEntity) entity );
                needsAttackAI = true;
            }
            
            if( needsAttackAI ) {
                addMeleeAttackAI( (CreatureEntity) entity );
            }
        }
        
        // Call for help
        if( !NBTHelper.containsNumber( tag, TAG_CALL_FOR_HELP ) ) {
            tag.putBoolean( TAG_CALL_FOR_HELP, Config.GENERAL.REACTIONS.callForHelpList.rollChance( entity ) );
        }
        if( tag.getBoolean( TAG_CALL_FOR_HELP ) ) {
            setHelpAI( entity );
        }
        
        // Rider
        final boolean small = Config.GENERAL.JOCKEYS.riderWhitelistSmall.get().contains( entity );
        if( !NBTHelper.containsNumber( tag, TAG_RIDER ) ) {
            final boolean makeRider;
            if( Config.GENERAL.JOCKEYS.riderBlacklist.get().contains( entity ) ) {
                makeRider = false;
            }
            // Small rider whitelist is a special case, so it gets priority over the normal whitelist
            else if( small ) {
                makeRider = Config.GENERAL.JOCKEYS.riderWhitelistSmall.get().rollChance( entity );
            }
            else {
                makeRider = Config.GENERAL.JOCKEYS.riderWhitelist.get().rollChance( entity );
            }
            tag.putBoolean( TAG_RIDER, makeRider );
        }
        if( tag.getBoolean( TAG_RIDER ) ) {
            addRiderAI( entity, small );
        }
        
        // Passive griefing
        if( !NBTHelper.containsNumber( tag, TAG_HIDE ) ) {
            tag.putBoolean( TAG_HIDE, Config.IDLE.HIDING.entityList.rollChance( entity ) );
        }
        if( !NBTHelper.containsNumber( tag, TAG_GRIEF ) ) {
            tag.putBoolean( TAG_GRIEF, Config.IDLE.GRIEFING.entityList.rollChance( entity ) );
        }
        if( !NBTHelper.containsNumber( tag, TAG_FIDDLE ) ) {
            tag.putBoolean( TAG_FIDDLE, Config.IDLE.FIDDLING.entityList.rollChance( entity ) );
        }
        addIdleAI( entity, tag.getBoolean( TAG_HIDE ), tag.getBoolean( TAG_GRIEF ), tag.getBoolean( TAG_FIDDLE ) );

        /* WIP
        // Digging
        if (entity instanceof ZombieEntity) {
            addDigAI(entity);
        }
         */
        
        // Door-breaking AI
        if( !NBTHelper.containsNumber( tag, TAG_DOOR_BREAK ) ) {
            tag.putBoolean( TAG_DOOR_BREAK, Config.GENERAL.DOOR_BREAKING.entityList.rollChance( entity ) );
        }
        if( tag.getBoolean( TAG_DOOR_BREAK ) ) {
            addDoorBreakAI( entity );
        }
        
        // Elite AI
        final CompoundNBT eliteTag = NBTHelper.containsCompound( tag, TAG_ELITE_AI ) ?
                tag.getCompound( TAG_ELITE_AI ) : initializeEliteAIData( tag, entity );
        EliteAIHelper.loadEliteAI( entity, eliteTag, tag.getBoolean( TAG_FORCE_INIT ) );
        tag.remove( TAG_FORCE_INIT );
    }
    
    /**
     * Picks the elite AIs a new entity should have, saves all decisions to the entity data,
     * and marks the entity for attribute/equipment/etc. initialization.
     */
    private static CompoundNBT initializeEliteAIData( CompoundNBT tag, MobEntity entity ) {
        CompoundNBT eliteTag = NBTHelper.getOrCreateCompound( tag, TAG_ELITE_AI );
        
        // Apply random-weighted AI selection
        final double[] chances = Config.ELITE_AI.GENERAL.entityList.getValues( entity );
        if( chances != null ) {
            for( double chance : chances ) {
                if( chance > 0.0 && entity.getRandom().nextDouble() < chance ) {
                    EliteAIHelper.saveEliteAI( eliteTag, entity );
                }
            }
        }
        
        // Apply specific AI selection
        for( EliteAIConfig.EliteAICategory ai : Config.ELITE_AI.getEliteAICategories() ) {
            if( ai.entityList.rollChance( entity ) ) {
                EliteAIHelper.saveEliteAI( eliteTag, ai.TYPE );
            }
        }
        
        // Mark this entity to init, if not already forced
        if( !NBTHelper.containsNumber( tag, TAG_FORCE_INIT ) ) {
            tag.putBoolean( TAG_FORCE_INIT, true );
        }
        return eliteTag;
    }
    
    /**
     * Called by EntityLivingBase.onDeath().
     * EntityLivingBase entityLiving = the entity dying.
     * DamageSource source = the damage source that killed the entity.
     *
     * @param event The event being triggered.
     */
    public static void onLivingDeath( LivingDeathEvent event ) {
        // Call for help on death
        final double chance = Config.GENERAL.REACTIONS.callForHelpOnDeathList.getValue( event.getEntityLiving() );
        if( chance > 0.0 && event.getEntityLiving() instanceof MobEntity && event.getEntityLiving().getRandom().nextDouble() < chance ) {
            MobEntity entity = (MobEntity) event.getEntityLiving();
            Entity target = event.getSource().getEntity();
            if( target instanceof LivingEntity ) {
                
                // Alert all similar entities around the killed entity to the killer
                final double range = entity.getAttributeValue( Attributes.FOLLOW_RANGE );
                AxisAlignedBB boundingBox = AxisAlignedBB.unitCubeFromLowerCorner( entity.position() ).inflate( range, 10.0, range );
                
                // Note this logic is duplicated from the "hurt by target" goal, it is just massively simplified
                for( MobEntity other : entity.level.getLoadedEntitiesOfClass( entity.getClass(), boundingBox ) ) {
                    if( entity != other && other.getTarget() == null &&
                            (!(entity instanceof TameableEntity) || ((TameableEntity) entity).getOwner() == ((TameableEntity) other).getOwner()) &&
                            !other.isAlliedTo( target ) ) {
                        other.setTarget( (LivingEntity) target );
                    }
                }
            }
        }
    }
    
    /**
     * Called when a player right-clicks while targeting a block.
     *
     * @param event The event data.
     */
    public static void onRightClickBlock( PlayerInteractEvent.RightClickBlock event ) {
        if( !event.getWorld().isClientSide() && event.getUseBlock() != Event.Result.DENY ) {
            BlockHelper.spawnHiddenMob( event.getWorld(), event.getPos(), event.getPlayer() );
        }
    }
    
    /**
     * Called right before a block is broken by a player.
     *
     * @param event The event data.
     */
    public static void onBlockBreak( BlockEvent.BreakEvent event ) {
        if( !event.getWorld().isClientSide() ) {
            BlockHelper.spawnHiddenMob( event.getWorld(), event.getPos(), event.getPlayer() );
        }
    }
    
    /**
     * Called after an explosion has calculated targets, but before applying effects.
     *
     * @param event The event data.
     */
    public static void onExplosionDetonate( ExplosionEvent.Detonate event ) {
        final IWorld world = event.getWorld();
        if( !world.isClientSide() ) {
            LivingEntity source = event.getExplosion().getSourceMob();
            PlayerEntity player = source instanceof PlayerEntity ? (PlayerEntity) source : null;
            for( BlockPos pos : event.getAffectedBlocks() ) {
                BlockHelper.spawnHiddenMob( world, pos, player );
            }
        }
    }
}