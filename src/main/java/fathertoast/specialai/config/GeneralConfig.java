package fathertoast.specialai.config;

import fathertoast.crust.api.config.common.AbstractConfigCategory;
import fathertoast.crust.api.config.common.AbstractConfigFile;
import fathertoast.crust.api.config.common.ConfigManager;
import fathertoast.crust.api.config.common.field.BooleanField;
import fathertoast.crust.api.config.common.field.DoubleField;
import fathertoast.crust.api.config.common.field.IntField;
import fathertoast.crust.api.config.common.field.collection.BlockStateSetField;
import fathertoast.crust.api.config.common.field.collection.EntityMapField;
import fathertoast.crust.api.config.common.field.collection.EntitySetField;
import fathertoast.crust.api.config.common.value.collection.BlockStateSet;
import fathertoast.crust.api.config.common.value.collection.EntityMap;
import fathertoast.crust.api.config.common.value.collection.EntitySet;
import fathertoast.crust.api.config.common.value.collection.value.ArrayValueCodec;
import fathertoast.crust.api.config.common.value.collection.value.DoubleValueCodec;
import fathertoast.crust.api.config.common.value.collection.value.EnumValueCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.EntityType;

import static fathertoast.specialai.ai.UniversalMeleeAttackGoal.MovementStrategy;

@SuppressWarnings( "UnstableApiUsage" )
public class GeneralConfig extends AbstractConfigFile {
    
    public final Animals ANIMALS;
    public final Reactions REACTIONS;
    public final Jockeys JOCKEYS;
    public final DoorBreaking DOOR_BREAKING;
    
    /** Builds the config spec that should be used for this config. */
    GeneralConfig( ConfigManager cfgManager, String cfgName ) {
        super( cfgManager, cfgName,
                "This config contains options for several miscellaneous features in the mod, such as: " +
                        "animals, reactions, jockeys, and door breaking."
        );
        
        ANIMALS = new Animals( this );
        REACTIONS = new Reactions( this );
        JOCKEYS = new Jockeys( this );
        DOOR_BREAKING = new DoorBreaking( this );
        
        // Print description for each movement strategy
        SPEC.decreaseIndent();
        SPEC.newLine( 2 );
        SPEC.titledComment( ChatFormatting.YELLOW + "Movement strategies",
                "Below is a description of every available 'movement strategy' type.",
                "Movement strategies are used by Special AI's melee attack goal to determine how to move the goal owner towards its target.",
                "Mobs from both vanilla and other mods can vary a lot when it comes to how they are designed to move.",
                "For this reason, Special AI's melee attack goal sometimes needs to treat different mobs differently," +
                        " and that is what movement strategies are for."
        );
        SPEC.increaseIndent();
        for( MovementStrategy strategy : MovementStrategy.values() ) {
            SPEC.fileOnlyNewLine();
            SPEC.titledComment( strategy.getSerializedName(), strategy.getDescription() );
        }
    }
    
    public static class Animals extends AbstractConfigCategory<GeneralConfig> {
        
        public final EntityMapField<Double> depacifyList;
        
        public final EntityMapField<Double> aggressiveList;
        
        public final EntityMapField<MovementStrategy> movementStratList;
        
        public final BooleanField eatBreedingItems;
        public final DoubleField eatingReach;
        public final BooleanField eatingHeals;
        public final IntField eatingCooldown;
        public final EntitySetField eatingBlacklist;
        
        
        Animals( GeneralConfig parent ) {
            super( parent, "animals",
                    "Options to customize mobs that are typically passive." );
            
            depacifyList = SPEC.define( new EntityMapField<>( "depacify_entities", createDefaultDepacifyList(),
                    "List of passive mobs (by entity type registry id) that are made 'neutral' like wolves.",
                    "Additional value after the entity type is the chance (0.0 to 1.0) for entities of that type to spawn with the AI." ) );
            
            SPEC.newLine();
            
            aggressiveList = SPEC.define( new EntityMapField<>( "aggressive_entities", createDefaultAggressiveList(),
                    "List of neutral (including depacified) mobs that are made 'aggressive' like monsters.",
                    "Additional value after the entity type is the chance (0.0 to 1.0) for entities of that type to spawn with the AI." ) );
            
            SPEC.newLine();
            
            movementStratList = SPEC.define( new EntityMapField<>( "movement_strategies", createDefaultMovementStrategyList(),
                    "A list of entities that should use a custom movement strategy for their Special AI melee attack AI.",
                    "Additional value after the entity type is the type of movement strategy the entity should use (see bottom of config " +
                            "for more info on strategies).",
                    "Note that this field ONLY applies to mobs that have been given Special AI's melee attack AI.",
                    "The AI is only given to mobs that are listed in the 'depacify' list or the 'aggressive' list above, " +
                            "if they don't already have a recognized attack goal." ) );
            
            SPEC.newLine();
            
            eatBreedingItems = SPEC.define( new BooleanField( "eat_breeding_items", true,
                    "If true, passive mobs will seek out and eat the items used to breed them laying on the floor." ) );
            
            eatingReach = SPEC.define( new DoubleField( "eating_reach", 2.0, DoubleField.Range.NON_NEGATIVE,
                    "Mobs' reach (from foot position) when targeting breeding items to eat. Player reach is about 4.5. " +
                            "When in range, the item will be slowly 'vacuumed' toward the passive mob." ) );
            
            eatingHeals = SPEC.define( new BooleanField( "eating_heals", true,
                    "If true, when mobs eat breeding items off the floor, they will regain health (like " +
                            "wolves). The option \"eat_breeding_items\" needs to be enabled for this to have any effect." ) );
            
            eatingCooldown = SPEC.define( new IntField( "eating_cooldown", 2, IntField.Range.NON_NEGATIVE,
                    "The cooldown in ticks between each time the mob will consume one item out of the food item stack it has found." ) );
            
            eatingBlacklist = SPEC.define( new EntitySetField( "eating_blacklist", new EntitySet.Builder<>().build(),
                    "List of animal entities that should not get the 'eat breeding items' AI." ) );
        }
        
        private static EntityMap<Double> createDefaultDepacifyList() {
            return new EntityMap.Builder<>( DoubleValueCodec.PERCENT )
                    // Farm animals
                    .put( EntityType.CHICKEN, 1.0 ).put( EntityType.COW, 1.0 )
                    .put( EntityType.PIG, 1.0 ).put( EntityType.SHEEP, 1.0 )
                    // Wild animals
                    .put( EntityType.RABBIT, 0.25 ).put( EntityType.FOX, 0.3 )
                    .put( EntityType.BAT, 1.0 )
                    // Nether
                    .put( EntityType.STRIDER, 1.0 )
                    // Water
                    .put( EntityType.SQUID, 1.0 ).put( EntityType.COD, 1.0 )
                    .put( EntityType.SALMON, 1.0 ).put( EntityType.TROPICAL_FISH, 1.0 )
                    .build();
        }
        
        private static EntityMap<Double> createDefaultAggressiveList() {
            return new EntityMap.Builder<>( DoubleValueCodec.PERCENT )
                    // Farm animals
                    .put( EntityType.COW, 0.04 )
                    // Wild animals
                    .put( EntityType.RABBIT, 0.02 ).put( EntityType.FOX, 0.03 )
                    .put( EntityType.BAT, 0.05 )
                    // Nether
                    .put( EntityType.STRIDER, 0.02 )
                    // Water
                    .put( EntityType.SQUID, 0.05 ).put( EntityType.COD, 0.02 )
                    .put( EntityType.SALMON, 0.04 )
                    .build();
        }
        
        private static EntityMap<MovementStrategy> createDefaultMovementStrategyList() {
            final EntityMap.Builder<MovementStrategy, ?> builder = new EntityMap.Builder<>(
                    EnumValueCodec.of( MovementStrategy.AUTO )
            );
            return builder.buildWithDefault( MovementStrategy.AUTO );
        }
    }
    
    public static class Reactions extends AbstractConfigCategory<GeneralConfig> {
        
        public final EntityMapField<Double> avoidExplosionsList;
        
        public final EntityMapField<Double> callForHelpList;
        public final EntityMapField<Double> callForHelpOnDeathList;
        
        public final EntityMapField<Double[]> dodgeArrowsList;
        
        
        Reactions( GeneralConfig parent ) {
            super( parent, "reaction_ai",
                    "Options to customize reactive behaviors." );
            
            avoidExplosionsList = SPEC.define( new EntityMapField<>( "avoid_explosions",
                    new EntityMap.Builder<>( DoubleValueCodec.NON_NEGATIVE ).buildWithDefault( 1.4 ),
                    "List of mobs that will try to avoid TNT and creepers that are about to explode.",
                    "Additional value after the entity type is their movement speed multiplier while fleeing." ) );
            
            SPEC.newLine();
            
            callForHelpList = SPEC.define( new EntityMapField<>( "call_for_help",
                    new EntityMap.Builder<>( DoubleValueCodec.PERCENT ).buildWithDefault( 1.0 ),
                    "List of mobs that will call for help from nearby mobs of the same type when " +
                            "struck. This does not trigger from killing blows (see below).",
                    "Additional value after the entity type is the chance (0.0 to 1.0) for entities of that type to spawn with the AI." ) );
            
            callForHelpOnDeathList = SPEC.define( new EntityMapField<>( "call_for_help_on_death",
                    new EntityMap.Builder<>( DoubleValueCodec.PERCENT ).buildWithDefault( 0.1 ),
                    "List of mobs that will call for help when dealt a killing blow and the chance for it to occur." ) );
            
            SPEC.newLine();
            
            dodgeArrowsList = SPEC.define( new EntityMapField<>( "dodge_arrows", createDefaultDodgeArrowsList(),
                    "List of mobs that will try to sidestep an arrow fired in their direction.",
                    "Additional values after the entity type are the chance (0.0 to 1.0) for entities of that type " +
                            "to spawn with the AI, followed by the chance for entities of that type with the AI to " +
                            "attempt to dodge (rolled for each arrow)." ) );
        }
        
        private static EntityMap<Double[]> createDefaultDodgeArrowsList() {
            return new EntityMap.Builder<>( ArrayValueCodec.of( 2, Double.class, DoubleValueCodec.PERCENT ) )
                    .put( EntityType.SKELETON, new Double[] { 1.0, 0.5 } )
                    .put( EntityType.STRAY, new Double[] { 1.0, 0.5 } )
                    .put( EntityType.WITHER_SKELETON, new Double[] { 1.0, 0.5 } )
                    .buildWithDefault( new Double[] { 0.2, 0.5 } );
        }
    }
    
    public static class Jockeys extends AbstractConfigCategory<GeneralConfig> {
        
        public final EntitySetField mountWhitelist;
        public final EntitySetField mountWhitelistSmall;
        public final EntitySetField mountBlacklist;
        
        public final EntityMapField<Double> riderWhitelist;
        public final EntityMapField<Double> riderWhitelistSmall;
        public final EntitySetField riderBlacklist;
        
        
        Jockeys( GeneralConfig parent ) {
            super( parent, "jockeys",
                    "Options relating to which mobs should act as riders or mounts." );
            
            mountWhitelist = SPEC.define( new EntitySetField( "mount_entities.whitelist", createDefaultMountWhitelist(),
                    "List of mobs that can be ridden on by normal-sized riders (not all entities can be controlled by their rider)." ) );
            
            mountWhitelistSmall = SPEC.define( new EntitySetField( "mount_entities.small_list", createDefaultMountWhitelistSmall(),
                    "List of mobs that can be ridden on by small riders or normal-sized riders that are babies " +
                            "(not all entities can be controlled by their rider)." ) );
            
            mountBlacklist = SPEC.define( new EntitySetField( "mount_entities.blacklist", new EntitySet.Builder<>().build(),
                    "List of mobs that cannot be ridden by any riders; normal-sized or small." ) );
            
            SPEC.newLine();
            
            riderWhitelist = SPEC.define( new EntityMapField<>( "rider_entities.whitelist", createDefaultRiderWhitelist(),
                    "List of mobs that can ride normal-sized mounts and the chance for them to gain the rider AI. " +
                            "Note that the entity must have task-based AI enabled." ) );
            
            riderWhitelistSmall = SPEC.define( new EntityMapField<>( "rider_entities.small_list", createDefaultRiderWhitelistSmall(),
                    "List of mobs that can only ride small mounts or normal-sized mounts that are babies and the " +
                            "chance for them to gain the rider AI. Note that the entity must have task-based AI enabled." ) );
            
            riderBlacklist = SPEC.define( new EntitySetField( "rider_entities.blacklist", new EntitySet.Builder<>().build(),
                    "List of mobs that cannot ride any mounts." ) );
            
        }
        
        private static EntitySet createDefaultMountWhitelist() {
            return new EntitySet.Builder<>()
                    // Monsters
                    .add( EntityType.SPIDER )
                    // Passive & neutral mobs
                    .add( EntityType.SHEEP ).add( EntityType.COW ).add( EntityType.POLAR_BEAR )
                    // Horse-type mobs
                    .add( EntityType.SKELETON_HORSE ).add( EntityType.ZOMBIE_HORSE )
                    .add( EntityType.HORSE ).add( EntityType.DONKEY )
                    .add( EntityType.MULE )
                    .addExtends( EntityType.LLAMA )
                    // Nether
                    .add( EntityType.HOGLIN )
                    .build();
        }
        
        private static EntitySet createDefaultMountWhitelistSmall() {
            return new EntitySet.Builder<>()
                    .add( EntityType.CHICKEN ).add( EntityType.RABBIT )
                    .add( EntityType.CAVE_SPIDER )
                    .build();
        }
        
        private static EntityMap<Double> createDefaultRiderWhitelist() {
            return new EntityMap.Builder<>( DoubleValueCodec.PERCENT )
                    .put( EntityType.SKELETON, 0.1 ).put( EntityType.STRAY, 0.1 )
                    .put( EntityType.ZOMBIE, 0.05 ).put( EntityType.WITCH, 0.05 )
                    .put( EntityType.CREEPER, 0.05 )
                    // Nether
                    .put( EntityType.PIGLIN, 0.1 ).put( EntityType.PIGLIN_BRUTE, 0.1 )
                    .put( EntityType.WITHER_SKELETON, 0.1 )
                    .build();
        }
        
        private static EntityMap<Double> createDefaultRiderWhitelistSmall() {
            return new EntityMap.Builder<>( DoubleValueCodec.PERCENT ).build();
        }
    }
    
    public static class DoorBreaking extends AbstractConfigCategory<GeneralConfig> {
        
        public final EntityMapField<Double> entityList;
        
        public final BooleanField requiresTarget;
        public final BooleanField requiresTools;
        
        public final BooleanField leaveDrops;
        
        public final DoubleField breakSpeed;
        public final BooleanField madCreepers;
        
        public final BooleanField targetDoors;
        public final BlockStateSetField targetList;
        
        
        DoorBreaking( GeneralConfig parent ) {
            super( parent, "door_breaking",
                    "Options to customize door-breaking behaviors." );
            
            entityList = SPEC.define( new EntityMapField<>( "entities", createDefaultEntityList(),
                    "List of mobs that can gain door breaking AI (note that the entity must have task-based AI enabled).",
                    "Additional value after the entity type is the chance (0.0 to 1.0) for entities of that type to spawn with the AI." ) );
            
            SPEC.newLine();
            
            requiresTarget = SPEC.define( new BooleanField( "require_target", true,
                    "If true, mobs will only break doors while they are chasing an attack target. " +
                            "Disabling this typically leads to mobs smashing into your house to get to blocks they are targeting " +
                            "as part of an idle griefing or fiddling behavior, such as torches or chests." ) );
            
            requiresTools = SPEC.define( new BooleanField( "require_tools", true,
                    "If true, mobs will only break doors they have the tools to harvest.",
                    "For example, they will only break iron doors if they have a pickaxe." ) );
            
            SPEC.newLine();
            
            leaveDrops = SPEC.define( new BooleanField( "leave_drops", true,
                    "If true, doors broken by mobs will leave item drops." ) );
            
            SPEC.newLine();
            
            breakSpeed = SPEC.define( new DoubleField( "break_speed", 0.33, DoubleField.Range.NON_NEGATIVE,
                    "The block breaking speed multiplier for mobs breaking doors, relative to the player's block breaking speed." ) );
            
            madCreepers = SPEC.define( new BooleanField( "mad_creepers", true,
                    "If true, creepers will resort to what they know best when they meet a door blocking their path." ) );
            
            SPEC.newLine();
            
            targetDoors = SPEC.define( new BooleanField( "targets.auto_target_doors", true,
                    "If true, door breaking AI will automatically target all blocks that derive from the " +
                            "vanilla doors, fence gates, and trapdoors." ) );
            
            targetList = SPEC.define( new BlockStateSetField( "targets.list", new BlockStateSet.Builder<>().build(),
                    "List of blocks that that can be broken by the door breaking AI." ) );
        }
        
        private static EntityMap<Double> createDefaultEntityList() {
            return new EntityMap.Builder<>( DoubleValueCodec.PERCENT )
                    .put( EntityType.ZOMBIE, 1.0 )
                    .put( EntityType.CREEPER, 1.0 )
                    .build();
        }
    }
}