package fathertoast.specialai.config;

import fathertoast.crust.api.config.common.AbstractConfigCategory;
import fathertoast.crust.api.config.common.AbstractConfigFile;
import fathertoast.crust.api.config.common.ConfigManager;
import fathertoast.crust.api.config.common.field.*;
import fathertoast.crust.api.config.common.value.BlockEntry;
import fathertoast.crust.api.config.common.value.BlockList;
import fathertoast.crust.api.config.common.value.EntityEntry;
import fathertoast.crust.api.config.common.value.EntityList;
import net.minecraft.block.*;
import net.minecraft.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class IdleConfig extends AbstractConfigFile {
    
    public final IdleGeneral GENERAL;
    public final Griefing GRIEFING;
    public final Fiddling FIDDLING;
    public final Hiding HIDING;
    
    /** Builds the config spec that should be used for this config. */
    IdleConfig( ConfigManager cfgManager, String cfgName ) {
        super( cfgManager, cfgName,
                "This config contains options for idle behaviors; actions taken by mobs when they are bored."
        );
        
        SPEC.fileOnlyNewLine();
        SPEC.describeEntityList();
        SPEC.fileOnlyNewLine();
        SPEC.describeBlockList();
        
        GENERAL = new IdleGeneral( this );
        GRIEFING = new Griefing( this );
        FIDDLING = new Fiddling( this );
        HIDING = new Hiding( this );
    }
    
    public static class IdleGeneral extends AbstractConfigCategory<IdleConfig> {
        
        public final DoubleField reach;
        
        public final IntField rangeHorizontal;
        public final IntField rangeVertical;
        
        public final IntField scanDelay;
        public final IntField scanCount;
        public final IntField scanCountGlobal;
        
        IdleGeneral( IdleConfig parent ) {
            super( parent, "idle_general",
                    "Options that affect all idle behaviors for monsters (griefing and fiddling)." );
            
            reach = SPEC.define( new DoubleField( "reach", 3.5, DoubleField.Range.NON_NEGATIVE,
                    "Mobs' reach (from eye height) when targeting blocks. Player reach is about 4.5." ) );
            
            SPEC.newLine();
            
            rangeHorizontal = SPEC.define( new IntField( "scan_range.horizontal", 12, IntField.Range.POSITIVE,
                    "The range at which mobs will search for blocks to target horizontally (xz-plane) and vertically (y-axis)." ) );
            rangeVertical = SPEC.define( new IntField( "scan_range.vertical", 6, IntField.Range.POSITIVE ) );
            
            SPEC.newLine();
            
            scanDelay = SPEC.define( new IntField( "scan_delay", 2, IntField.Range.POSITIVE,
                    "The number of ticks between each block scan." ) );
            scanCount = SPEC.define( new IntField( "scan_count", 32, IntField.Range.POSITIVE,
                    "The number of blocks each mob randomly searches to grief/fiddle with every \"scan_delay\" ticks." ) );
            scanCountGlobal = SPEC.define( new IntField( "global_scan_count", 3000, IntField.Range.NON_NEGATIVE,
                    "The maximum number of blocks that can be searched in any given tick by all mobs. 0 is no limit." ) );
        }
    }
    
    public static class Griefing extends AbstractConfigCategory<IdleConfig> {
        
        public final EntityListField.Combined entityList;
        
        public final BooleanField requiresTools;
        
        public final BooleanField leaveDrops;
        public final BooleanField breakSound;
        
        public final DoubleField breakSpeed;
        public final BooleanField madCreepers;
        
        public final BooleanField targetLights;
        public final BooleanField targetBeds;
        public final BlockListField targetWhitelist;
        public final BlockListField targetWhitelistLootable;
        public final BlockListField targetBlacklist;
        
        Griefing( IdleConfig parent ) {
            super( parent, "idle_griefing",
                    "Options to customize monsters' idle block breaking behavior." );
            
            entityList = new EntityListField.Combined(
                    SPEC.define( new EntityListField( "entities.whitelist", new EntityList(
                            new EntityEntry( EntityType.ZOMBIE, 1.0 ), new EntityEntry( EntityType.CREEPER, 1.0 )
                    ).setSinglePercent(),
                            "List of mobs that can gain passive griefing AI (note that the entity must have task-based AI enabled).",
                            "Additional value after the entity type is the chance (0.0 to 1.0) for entities of that type to spawn with the AI." ) ),
                    SPEC.define( new EntityListField( "entities.blacklist", new EntityList().setNoValues() ) )
            );
            
            SPEC.newLine();
            
            requiresTools = SPEC.define( new BooleanField( "requires_tools", true,
                    "If true, mobs will only grief blocks they have the tools to harvest.",
                    "For example, they will only break furnaces if they have a pickaxe." ) );
            
            SPEC.newLine();
            
            leaveDrops = SPEC.define( new BooleanField( "leaves_drops", true,
                    "If true, blocks griefed by mobs will leave item drops." ) );
            breakSound = SPEC.define( new BooleanField( "break_sound", false,
                    "If true, a loud snapping sound (the vanilla door break sound) will be played when a " +
                            "block is broken, which is audible regardless of distance." ) );
            
            SPEC.newLine();
            
            breakSpeed = SPEC.define( new DoubleField( "break_speed", 0.5, DoubleField.Range.NON_NEGATIVE,
                    "The block breaking speed multiplier for mobs griefing blocks, relative to the player's block breaking speed." ) );
            madCreepers = SPEC.define( new BooleanField( "mad_creepers", true,
                    "If true, creepers will be upset about not having arms to grief blocks with and resort to what they know best." ) );
            
            SPEC.newLine();
            
            targetLights = SPEC.define( new BooleanField( "targets.auto_target_lights", true,
                    "If true, idle griefing AI will automatically target all light sources (light value > 1). " +
                            "This will do its best to avoid natural sources such as fire and redstone ore." ) );
            targetBeds = SPEC.define( new BooleanField( "targets.auto_target_beds", true,
                    "If true, idle griefing AI will automatically target all blocks that derive from the vanilla beds." ) );
            targetWhitelist = SPEC.define( new BlockListField( "targets.whitelist", buildDefaultGriefTargets(),
                    "List of blocks that can be broken by the idle griefing AI." ) );
            targetWhitelistLootable = SPEC.define( new BlockListField( "targets.lootable_list", buildDefaultLootableGriefTargets(),
                    "Like \"grief_targets.whitelist\", but these blocks will not be targeted if they have a loot table tag.",
                    "For example, unopened dungeon chests will not be targeted." ) );
            targetBlacklist = SPEC.define( new BlockListField( "targets.blacklist", new BlockList() ) );
        }
        
        /** Build a list of special use blocks. */
        private static BlockList buildDefaultGriefTargets() {
            // Start with specific blocks
            final List<BlockEntry> targets = new ArrayList<>( Arrays.asList(
                    // Farm blocks
                    new BlockEntry( Blocks.FARMLAND ), new BlockEntry( Blocks.BEEHIVE )
            ) );
            // Add block groups, possibly including mod blocks
            for( Block block : ForgeRegistries.BLOCKS ) {
                // Basic crafting blocks (note blast furnace & smoker are covered by abstract furnace)
                if( block instanceof CraftingTableBlock || block instanceof AbstractFurnaceBlock || block instanceof BrewingStandBlock ||
                        
                        // Advanced crafting blocks (note fletching & smithing tables are covered by crafting table)
                        block instanceof StonecutterBlock || block instanceof LoomBlock || block instanceof CartographyTableBlock ||
                        
                        // Equipment reworking blocks
                        block instanceof EnchantingTableBlock || block instanceof AnvilBlock || block instanceof GrindstoneBlock ||
                        
                        // Access blocks
                        block instanceof LadderBlock || block instanceof ScaffoldingBlock
                ) {
                    targets.add( new BlockEntry( block ) );
                }
            }
            return new BlockList( targets.toArray( new BlockEntry[0] ) );
        }
        
        /** Build a list of chest blocks. */
        private static BlockList buildDefaultLootableGriefTargets() {
            List<BlockEntry> targets = new ArrayList<>();
            for( Block block : ForgeRegistries.BLOCKS ) {
                // Chest blocks
                if( block instanceof AbstractChestBlock || block instanceof BarrelBlock ) {
                    targets.add( new BlockEntry( block ) );
                }
            }
            return new BlockList( targets.toArray( new BlockEntry[0] ) );
        }
    }
    
    public static class Fiddling extends AbstractConfigCategory<IdleConfig> {
        
        public final EntityListField.Combined entityList;
        
        public final BooleanField targetSwitches;
        public final BooleanField targetDoors;
        public final BlockListField.Combined targetList;
        
        Fiddling( IdleConfig parent ) {
            super( parent, "idle_fiddling",
                    "Options to customize monsters' idle fiddling behavior (block interaction)." );
            
            entityList = new EntityListField.Combined(
                    SPEC.define( new EntityListField( "entities.whitelist", new EntityList(
                            new EntityEntry( EntityType.SKELETON, 1.0 ), new EntityEntry( EntityType.STRAY, 1.0 ),
                            new EntityEntry( EntityType.WITHER_SKELETON, 1.0 ),
                            new EntityEntry( EntityType.ZOMBIFIED_PIGLIN, 1.0 ), new EntityEntry( EntityType.PIGLIN, 1.0 )
                    ).setSinglePercent(),
                            "List of mobs that can gain passive fiddling AI (note that the entity must have task-based AI enabled).",
                            "Additional value after the entity type is the chance (0.0 to 1.0) for entities of that type to spawn with the AI." ) ),
                    SPEC.define( new EntityListField( "entities.blacklist", new EntityList().setNoValues() ) )
            );
            
            SPEC.newLine();
            
            targetSwitches = SPEC.define( new BooleanField( "targets.auto_target_switches", true,
                    "If true, idle fiddling AI will automatically target all blocks that derive from",
                    "the vanilla levers and buttons." ) );
            targetDoors = SPEC.define( new BooleanField( "targets.auto_target_doors", true,
                    "If true, idle fiddling AI will automatically target all non-metal blocks that derive",
                    "from the vanilla doors, fence gates, and trapdoors." ) );
            targetList = new BlockListField.Combined(
                    SPEC.define( new BlockListField( "targets.whitelist", new BlockList(
                            new BlockEntry( Blocks.REPEATER ), new BlockEntry( Blocks.COMPARATOR ),
                            new BlockEntry( Blocks.TNT ), new BlockEntry( Blocks.CAKE ) ),
                            "List of blocks that can be interacted with by the idle fiddling AI." ) ),
                    SPEC.define( new BlockListField( "targets.blacklist", new BlockList() ) )
            );
        }
    }
    
    public static class Hiding extends AbstractConfigCategory<IdleConfig> {
        
        public final EntityListField.Combined entityList;
        
        public final BooleanField avoidLootableTargets;
        public final BlockListField.Combined targetList;
        
        Hiding( IdleConfig parent ) {
            super( parent, "idle_hiding",
                    "Options to customize monsters' idle hiding behavior. This causes the mob to crawl " +
                            "inside a container block and pop out when the container is opened (or destroyed)." );
            
            entityList = new EntityListField.Combined(
                    SPEC.define( new EntityListField( "entities.whitelist", new EntityList(
                            new EntityEntry( EntityType.CREEPER, 1.0 ), new EntityEntry( EntityType.SPIDER, 1.0 )
                    ).setSinglePercent(),
                            "List of mobs that can gain passive hiding AI (note that the entity must have task-based AI enabled).",
                            "Additional value after the entity type is the chance (0.0 to 1.0) for entities of that type to spawn with the AI." ) ),
                    SPEC.define( new EntityListField( "entities.blacklist", new EntityList().setNoValues() ) )
            );
            
            SPEC.newLine();
            
            avoidLootableTargets = SPEC.define( new BooleanField( "targets.avoid_lootable", true,
                    "If true, blocks will not be targeted if they have a loot table tag.",
                    "For example, unopened dungeon chests will not be targeted." ) );
            targetList = new BlockListField.Combined(
                    SPEC.define( new BlockListField( "targets.whitelist", buildDefaultHideTargets(),
                            "List of blocks that can be hidden in by the idle hiding AI. " +
                                    "Note that only blocks with tile entities are able to be hidden in." ) ),
                    SPEC.define( new BlockListField( "targets.blacklist", new BlockList() ) )
            );
        }
        
        /** Build a list of chest blocks. */
        private static BlockList buildDefaultHideTargets() {
            List<BlockEntry> targets = new ArrayList<>();
            for( Block block : ForgeRegistries.BLOCKS ) {
                // Non-ender chest blocks
                if( block instanceof AbstractChestBlock && !(block instanceof EnderChestBlock) || block instanceof BarrelBlock ) {
                    targets.add( new BlockEntry( block ) );
                }
            }
            return new BlockList( targets.toArray( new BlockEntry[0] ) );
        }
    }
}