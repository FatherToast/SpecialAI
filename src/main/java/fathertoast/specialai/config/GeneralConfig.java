package fathertoast.specialai.config;

import fathertoast.crust.api.config.common.AbstractConfigCategory;
import fathertoast.crust.api.config.common.AbstractConfigFile;
import fathertoast.crust.api.config.common.ConfigManager;
import fathertoast.crust.api.config.common.field.*;
import fathertoast.crust.api.config.common.value.BlockList;
import fathertoast.crust.api.config.common.value.EntityEntry;
import fathertoast.crust.api.config.common.value.EntityList;
import net.minecraft.entity.EntityType;

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
        
        SPEC.fileOnlyNewLine();
        SPEC.describeEntityList();
        SPEC.fileOnlyNewLine();
        SPEC.describeBlockList();
        
        ANIMALS = new Animals( this );
        REACTIONS = new Reactions( this );
        JOCKEYS = new Jockeys( this );
        DOOR_BREAKING = new DoorBreaking( this );
    }
    
    public static class Animals extends AbstractConfigCategory<GeneralConfig> {
        
        public final EntityListField.Combined depacifyList;
        
        public final EntityListField.Combined aggressiveList;
        
        public final BooleanField eatBreedingItems;
        public final DoubleField eatingReach;
        public final BooleanField eatingHeals;
        public final IntField eatingCooldown;
        
        Animals( GeneralConfig parent ) {
            super( parent, "animals",
                    "Options to customize mobs that are typically passive." );
            
            //TODO Decide whether horses should be on this list, and verify each of these work (especially the squid)
            depacifyList = new EntityListField.Combined(
                    SPEC.define( new EntityListField( "depacify_entities.whitelist", new EntityList(
                            // Farm animals
                            new EntityEntry( EntityType.CHICKEN, 1.0 ), new EntityEntry( EntityType.COW, 1.0 ),
                            new EntityEntry( EntityType.PIG, 1.0 ), new EntityEntry( EntityType.SHEEP, 1.0 ),
                            new EntityEntry( EntityType.RABBIT, 1.0 ),
                            // Nether
                            new EntityEntry( EntityType.STRIDER, 1.0 ),
                            // Water
                            new EntityEntry( EntityType.SQUID, 1.0 ), new EntityEntry( EntityType.COD, 1.0 ),
                            new EntityEntry( EntityType.SALMON, 1.0 ), new EntityEntry( EntityType.TROPICAL_FISH, 1.0 )
                    ).setSinglePercent(),
                            "List of passive mobs (by entity type registry id) that are made 'neutral' like wolves.",
                            "Additional value after the entity type is the chance (0.0 to 1.0) for entities of that type to spawn with the AI." ) ),
                    SPEC.define( new EntityListField( "depacify_entities.blacklist", new EntityList().setNoValues() ) )
            );
            
            SPEC.newLine();
            
            aggressiveList = new EntityListField.Combined(
                    SPEC.define( new EntityListField( "aggressive_entities.whitelist", new EntityList(
                            new EntityEntry( EntityType.COW, 0.04 ), new EntityEntry( EntityType.RABBIT, 0.02 ),
                            new EntityEntry( EntityType.STRIDER, 0.02 ),
                            new EntityEntry( EntityType.SQUID, 1.0 ), new EntityEntry( EntityType.COD, 0.02 ),
                            new EntityEntry( EntityType.SALMON, 0.04 )
                    ).setSinglePercent(),
                            "List of neutral (including depacified) mobs that are made 'aggressive' like monsters.",
                            "Additional value after the entity type is the chance (0.0 to 1.0) for entities of that type to spawn with the AI." ) ),
                    SPEC.define( new EntityListField( "aggressive_entities.blacklist", new EntityList().setNoValues() ) )
            );
            
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
        }
    }
    
    public static class Reactions extends AbstractConfigCategory<GeneralConfig> {
        
        public final EntityListField.Combined avoidExplosionsList;
        
        public final EntityListField.Combined callForHelpList;
        public final EntityListField.Combined callForHelpOnDeathList;
        
        public final EntityListField.Combined dodgeArrowsList;
        
        Reactions( GeneralConfig parent ) {
            super( parent, "reaction_ai",
                    "Options to customize reactive behaviors." );
            
            avoidExplosionsList = new EntityListField.Combined(
                    SPEC.define( new EntityListField( "avoid_explosions.whitelist", new EntityList(
                            new EntityEntry( 1.4 )
                    ).setSingleValue().setRangePos(),
                            "List of mobs that will try to avoid TNT and creepers that are about to explode.",
                            "Additional value after the entity type is their movement speed multiplier while fleeing." ) ),
                    SPEC.define( new EntityListField( "avoid_explosions.blacklist", new EntityList().setNoValues() ) )
            );
            
            SPEC.newLine();
            
            callForHelpList = new EntityListField.Combined(
                    SPEC.define( new EntityListField( "call_for_help.whitelist", new EntityList(
                            new EntityEntry( 1.0 )
                    ).setSinglePercent(),
                            "List of mobs that will call for help from nearby mobs of the same type when " +
                                    "struck. This does not trigger from killing blows (see below).",
                            "Additional value after the entity type is the chance (0.0 to 1.0) for entities of that type to spawn with the AI." ) ),
                    SPEC.define( new EntityListField( "call_for_help.blacklist", new EntityList().setNoValues() ) )
            );
            callForHelpOnDeathList = new EntityListField.Combined(
                    SPEC.define( new EntityListField( "call_for_help_on_death.whitelist", new EntityList(
                            new EntityEntry( 0.1 )
                    ).setSinglePercent(),
                            "List of mobs that will call for help when dealt a killing blow and the chance for it to occur." ) ),
                    SPEC.define( new EntityListField( "call_for_help_on_death.blacklist", new EntityList().setNoValues() ) )
            );
            
            SPEC.newLine();
            
            dodgeArrowsList = new EntityListField.Combined(
                    SPEC.define( new EntityListField( "dodge_arrows.whitelist", new EntityList(
                            new EntityEntry( 0.2, 0.5 ),
                            new EntityEntry( EntityType.SKELETON, 1.0, 0.5 ), new EntityEntry( EntityType.STRAY, 1.0, 0.5 ),
                            new EntityEntry( EntityType.WITHER_SKELETON, 1.0, 0.5 )
                    ).setMultiValue( 2 ).setRange0to1(),
                            "List of mobs that will try to sidestep an arrow fired in their direction.",
                            "Additional values after the entity type are the chance (0.0 to 1.0) for entities of that type " +
                                    "to spawn with the AI, followed by the chance for entities of that type with the AI to " +
                                    "attempt to dodge (rolled for each arrow)." ) ),
                    SPEC.define( new EntityListField( "dodge_arrows.blacklist", new EntityList().setNoValues() ) )
            );
        }
    }
    
    public static class Jockeys extends AbstractConfigCategory<GeneralConfig> {
        
        public final EntityListField mountWhitelist;
        public final EntityListField mountWhitelistSmall;
        public final EntityListField mountBlacklist;
        
        public final EntityListField riderWhitelist;
        public final EntityListField riderWhitelistSmall;
        public final EntityListField riderBlacklist;
        
        Jockeys( GeneralConfig parent ) {
            super( parent, "jockeys",
                    "Options relating to which mobs should act as riders or mounts." );
            
            mountWhitelist = SPEC.define( new EntityListField( "mount_entities.whitelist", new EntityList(
                    // Classic
                    new EntityEntry( EntityType.SPIDER ), new EntityEntry( EntityType.SLIME ),
                    new EntityEntry( EntityType.SKELETON_HORSE ), new EntityEntry( EntityType.ZOMBIE_HORSE ),
                    // Passive mobs
                    new EntityEntry( EntityType.PIG ), new EntityEntry( EntityType.SHEEP ),
                    new EntityEntry( EntityType.COW ), new EntityEntry( EntityType.POLAR_BEAR ),
                    // Horse-type mobs
                    new EntityEntry( EntityType.HORSE ), new EntityEntry( EntityType.DONKEY ),
                    new EntityEntry( EntityType.MULE ), new EntityEntry( EntityType.LLAMA, false ),
                    // Nether
                    new EntityEntry( EntityType.STRIDER ), new EntityEntry( EntityType.HOGLIN ),
                    new EntityEntry( EntityType.ZOGLIN )
            ).setNoValues(),
                    "List of mobs that can be ridden on by normal-sized riders (not all entities can be controlled by their rider)." ) );
            mountWhitelistSmall = SPEC.define( new EntityListField( "mount_entities.small_list", new EntityList(
                    // Classic
                    new EntityEntry( EntityType.CHICKEN ), new EntityEntry( EntityType.RABBIT ),
                    new EntityEntry( EntityType.CAVE_SPIDER )
            ).setNoValues(),
                    "List of mobs that can be ridden on by small riders or normal-sized riders that are babies " +
                            "(not all entities can be controlled by their rider)." ) );
            mountBlacklist = SPEC.define( new EntityListField( "mount_entities.blacklist", new EntityList().setNoValues() ) );
            
            SPEC.newLine();
            
            riderWhitelist = SPEC.define( new EntityListField( "rider_entities.whitelist", new EntityList(
                    // Classic
                    new EntityEntry( EntityType.SKELETON, 0.1 ), new EntityEntry( EntityType.STRAY, 0.1 ),
                    new EntityEntry( EntityType.WITHER_SKELETON, 0.1 ), new EntityEntry( EntityType.ZOMBIE, 0.05 ),
                    new EntityEntry( EntityType.CREEPER, 0.05 ), new EntityEntry( EntityType.WITCH, 0.05 ),
                    // Nether
                    new EntityEntry( EntityType.PIGLIN, 0.1 ), new EntityEntry( EntityType.PIGLIN_BRUTE, 0.1 )
            ).setSinglePercent(),
                    "List of mobs that can ride normal-sized mounts and the chance for them to gain the rider AI. " +
                            "Note that the entity must have task-based AI enabled." ) );
            riderWhitelistSmall = SPEC.define( new EntityListField( "rider_entities.small_list", new EntityList().setSinglePercent(),
                    "List of mobs that can only ride small mounts or normal-sized mounts that are babies and the " +
                            "chance for them to gain the rider AI. Note that the entity must have task-based AI enabled." ) );
            riderBlacklist = SPEC.define( new EntityListField( "rider_entities.blacklist", new EntityList().setNoValues() ) );
            
        }
    }
    
    public static class DoorBreaking extends AbstractConfigCategory<GeneralConfig> {
        
        public final EntityListField.Combined entityList;
        
        public final BooleanField requiresTarget;
        public final BooleanField requiresTools;
        
        public final BooleanField leaveDrops;
        
        public final DoubleField breakSpeed;
        public final BooleanField madCreepers;
        
        public final BooleanField targetDoors;
        public final BlockListField.Combined targetList;
        
        DoorBreaking( GeneralConfig parent ) {
            super( parent, "door_breaking",
                    "Options to customize door-breaking behaviors." );
            
            entityList = new EntityListField.Combined(
                    SPEC.define( new EntityListField( "entities.whitelist", new EntityList(
                            new EntityEntry( EntityType.ZOMBIE, 1.0 ), new EntityEntry( EntityType.CREEPER, 1.0 )
                    ).setSinglePercent(),
                            "List of mobs that can gain door breaking AI (note that the entity must have task-based AI enabled).",
                            "Additional value after the entity type is the chance (0.0 to 1.0) for entities of that type to spawn with the AI." ) ),
                    SPEC.define( new EntityListField( "entities.blacklist", new EntityList().setNoValues() ) )
            );
            
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
            targetList = new BlockListField.Combined(
                    SPEC.define( new BlockListField( "targets.whitelist", new BlockList(),
                            "List of blocks that that can be broken by the door breaking AI." ) ),
                    SPEC.define( new BlockListField( "targets.blacklist", new BlockList() ) )
            );
        }
    }
}