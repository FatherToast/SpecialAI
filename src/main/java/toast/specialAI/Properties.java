package toast.specialAI;

import java.util.Arrays;
import java.util.HashSet;

import net.minecraft.block.Block;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.entity.monster.EntityPolarBear;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.monster.EntitySpider;
import net.minecraft.entity.monster.EntityWitch;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.passive.EntityRabbit;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.init.Blocks;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import toast.specialAI.ai.special.ISpecialAI;
import toast.specialAI.ai.special.SpecialAIHandler;
import toast.specialAI.util.BlockHelper;
import toast.specialAI.util.EntityListConfig;
import toast.specialAI.util.EntityListConfig.EntryEntity;
import toast.specialAI.util.TargetBlock;

/**
 * This helper class loads, stores, and retrieves config options.
 */
public class Properties {

	public static Properties get() {
		return Properties.INSTANCE;
	}
	public static void load(Configuration configuration) {
		Properties.config = configuration;
        Properties.config.load();
        Properties.INSTANCE = new Properties();
        Properties.config.save();
		Properties.config = null;
	}


	public final GENERAL GENERAL = new GENERAL();
	public class GENERAL extends PropertyCategory {
		@Override
		public String name() { return "_general"; }
		@Override
		protected String comment() {
			return "General and/or miscellaneous options.";
		}

        public final boolean DEBUG = this.prop("_debug_mode", false,
        	"If true, the mod will start up in debug mode.");

        public final double AGGRESSIVE_CHANCE = this.prop("depacify_aggressive_chance", 0.005,
        	"Chance for an entity on the depacify list to spawn aggressive, instead of just neutral.",
        	PropertyCategory.RDBL_ONE);

        public final EntityListConfig DEPACIFY_LIST = this.prop("depacify_list", new EntryEntity[] {
    			new EntryEntity(EntityChicken.class, 1.0F), new EntryEntity(EntityCow.class, 1.0F),
    			new EntryEntity(EntityPig.class, 1.0F), new EntryEntity(EntitySheep.class, 1.0F)
			},
    		"List of passive mobs (by entity id) that are made \"neutral\".\n"
    		+ "You may put a tilde (~) in front of any entity id to make it specific. Specific entity ids will not count any entities extending (i.e., based on) the specified entity.\n"
    		+ "Additional number after the entity id is the chance (0.0 to 1.0) for entities of that type to spawn with the AI.\n"
    		+ "May or may not work on mod mobs.");

        public final boolean EAT_BREEDING_ITEMS = this.prop("eat_breeding_items", true,
        	"If true, passive mobs will seek out and eat the items used to breed them laying on the floor.");

        public final boolean EATING_HEALS = this.prop("eating_heals", true,
        	"If true, when mobs eat breeding items off the floor, they will regain health like wolves.");

    };

	public final IDLE_AI IDLE_AI = new IDLE_AI();
	public class IDLE_AI extends PropertyCategory {
		@Override
		public String name() { return "idle_activities"; }
		@Override
		protected String comment() {
			return "Options to customize all idle behaviors for monsters.";
		}

        public final float REACH = this.prop("reach", 3.5F,
        	"Mobs' reach (from eye height) when targeting blocks. Player reach is about 4.5.");

        public final int RANGE_XZ = this.prop("range", 12,
        	"The range at which mobs will search for blocks to target horizontally (xz-plane.");

        public final int RANGE_Y = this.prop("range_vertical", 6,
        	"The range at which mobs will search for blocks to target vertically (y-axis).");

        public final int SCAN_COUNT = this.prop("scan_count", 24,
        	"The number of blocks mobs randomly search to grief/fiddle with each 3 ticks.",
        	PropertyCategory.RINT_POS1);

        public final int SCAN_COUNT_GLOBAL = this.prop("scan_count_global", 0,
        	"The maximum number of blocks that can be searched in any given tick by all mobs. 0 is no limit.");

	}

	public final FIDDLING FIDDLING = new FIDDLING();
	public class FIDDLING extends PropertyCategory {
		@Override
		public String name() { return "idle_fiddling"; }
		@Override
		protected String comment() {
			return "Options to customize monsters' idle fiddling behavior.";
		}

        public final boolean ENABLED = this.prop("_enabled", true,
        	"If true, mobs will flip switches, press buttons, etc. while not doing anything else.");

        public final EntityListConfig MOB_LIST = this.prop("mob_list", new EntryEntity[] { new EntryEntity(EntitySkeleton.class, 1.0F), new EntryEntity(EntityPigZombie.class, 1.0F) },
        	"List of mobs that can gain idle fiddling AI (note that the entity must have task-based AI enabled).");

        public final HashSet<TargetBlock> BLACKLIST = this.prop("target_blacklist", new Block[] { },
    		"Specific blocks that will NOT be fiddled with by mobs.\n"
    		+ "Only really useful if you whitelist an entire namespace (*) to prevent mobs from fiddling with a few blocks from that namespace.");

        public final HashSet<TargetBlock> TARGETLIST = this.prop("target_blocks", new Block[] {
        		Blocks.TNT, Blocks.LEVER, Blocks.WOODEN_BUTTON, Blocks.STONE_BUTTON,
        		Blocks.UNPOWERED_COMPARATOR, Blocks.POWERED_COMPARATOR,
        		Blocks.UNPOWERED_REPEATER, Blocks.POWERED_REPEATER,
        		Blocks.CAKE, Blocks.TRAPDOOR,
        		Blocks.OAK_DOOR, Blocks.BIRCH_DOOR, Blocks.SPRUCE_DOOR, Blocks.JUNGLE_DOOR, Blocks.ACACIA_DOOR, Blocks.DARK_OAK_DOOR,
        		Blocks.OAK_FENCE_GATE, Blocks.BIRCH_FENCE_GATE, Blocks.SPRUCE_FENCE_GATE, Blocks.JUNGLE_FENCE_GATE, Blocks.ACACIA_FENCE_GATE, Blocks.DARK_OAK_FENCE_GATE
    		},
    		"Specific blocks that will be fiddled with by mobs.");

	}

	public final GRIEFING GRIEFING = new GRIEFING();
	public class GRIEFING extends PropertyCategory {
		@Override
		public String name() { return "idle_griefing"; }
		@Override
		protected String comment() {
			return "Options to customize monsters' idle block breaking.";
		}

        public final boolean ENABLED = this.prop("_enabled", true,
        	"If true, mobs will destroy blocks while not doing anything else.");

        public final boolean BREAK_LIGHTS = this.prop("break_lights", true,
        	"If true, block breaking AI will automatically target all light sources (light value > 1).");

        public final boolean BREAK_SOUND = this.prop("break_sound", false,
        	"If true, a lound snapping sound (the vanilla door break sound) will be played when a block is broken, which is audible regardless of distance.");

        public final float BREAK_SPEED = this.prop("break_speed", 0.5F,
        	"The block breaking speed multiplier for mobs, relative to the player's block breaking speed.");

        public final boolean LEAVE_DROPS = this.prop("leave_drops", true,
        	"If true, griefed blocks will leave item drops.");

        public final boolean MAD_CREEPERS = this.prop("mad_creepers", true,
        	"If true, creepers will be very mad about not having arms to break things with, and resort to what they know best...");

        public final EntityListConfig MOB_LIST = this.prop("mob_list", new EntryEntity[] {
        		new EntryEntity(EntityZombie.class, 1.0F), new EntryEntity(EntityCreeper.class, 1.0F),
        		new EntryEntity(EntityPigZombie.class, 0.0F)
        	},
        	"List of mobs that can gain passive griefing AI (note that the entity must have task-based AI enabled).");

        public final boolean REQUIRES_TOOLS = this.prop("requires_tools", false,
        	"If true, mobs will only target blocks they have the tools to harvest.");

        public final HashSet<TargetBlock> BLACKLIST = this.prop("target_blacklist", new Block[] { },
    		"Specific blocks that will NOT be griefed by mobs.\n"
    		+ "Only really useful if \"break_lights\" is enabled or when you whitelist an entire namespace (*) to create safe light sources, prevent mobs from breaking normal world gen that produces light, or for removing a few blocks from a namespace that you don\'t want mobs to break.");

        public final HashSet<TargetBlock> TARGETLIST = this.prop("target_blocks", new Block[] {
        		Blocks.FARMLAND, Blocks.BED, Blocks.CRAFTING_TABLE, Blocks.BREWING_STAND, Blocks.TRAPDOOR, Blocks.LADDER,
        		Blocks.OAK_DOOR, Blocks.BIRCH_DOOR, Blocks.SPRUCE_DOOR, Blocks.JUNGLE_DOOR, Blocks.ACACIA_DOOR, Blocks.DARK_OAK_DOOR,
        		Blocks.OAK_FENCE_GATE, Blocks.BIRCH_FENCE_GATE, Blocks.SPRUCE_FENCE_GATE, Blocks.JUNGLE_FENCE_GATE, Blocks.ACACIA_FENCE_GATE, Blocks.DARK_OAK_FENCE_GATE,
        		Blocks.GOLDEN_RAIL
    		},
    		"Specific blocks that will be griefed by mobs.\n"
    		+ "You may specify metadata (0-15) with a space after the id, followed by the metadata. For example, \"cake\" will cause mobs to target all cakes, while \"cake 0\" will cause mobs to only target cakes that have not been partially eaten.\n"
    		+ "You may whitelist an entire namespace (mod) by using the namespace followed by a *. For example, \"minecraft:*\" will whitelist every vanilla block. This does not work if you try to also specify metadata.");

        public final HashSet<TargetBlock> TARGET_LOOTABLE = this.prop("target_lootable", new Block [] {
	        	Blocks.CHEST, Blocks.TRAPPED_CHEST
	        },
        	"Specific lootable blocks that will be griefed by mobs.\n"
        	+ "Unlike the normal \"target_blocks\", these blocks will not be targeted if they still have a loot table tag (e.g., unopened dungeon chests).");

	}

	public final REACT_AI REACT_AI = new REACT_AI();
	public class REACT_AI extends PropertyCategory {
		@Override
		public String name() { return "reaction_ai"; }
		@Override
		protected String comment() {
			return "Options to customize reactive behaviors.";
		}

        public final boolean AVOID_EXPLOSIONS = this.prop("avoid_explosions", true,
        	"If true, all mobs will try to avoid TNT and creepers that are about to explode.");

        public final boolean CALL_HELP = this.prop("call_for_help", true,
        	"If true, all mobs will call for help from nearby mobs of the same type when struck.");

        public final double CALL_HELP_ON_DEATH = this.prop("call_for_help_on_death", 0.2,
        	"Chance for mobs to call for help from a killing blow.",
        	PropertyCategory.RDBL_ONE);

        public final double DODGE_ARROWS = this.prop("dodge_arrows", 0.4,
        	"The chance any mob will try to sidestep an arrow fired in their direction.",
        	PropertyCategory.RDBL_ONE);

	}

	public final JOCKEYS JOCKEYS = new JOCKEYS();
	public class JOCKEYS extends PropertyCategory {
		@Override
		public String name() { return "jockeys"; }
		@Override
		protected String comment() {
			return "Options relating to which mobs should act as riders or mounts.";
		}

		public final EntityListConfig MOUNT_LIST = this.prop("mount_list", new EntryEntity[] {
				new EntryEntity(EntitySpider.class, true), new EntryEntity(EntitySlime.class, true),
				new EntryEntity(EntityPig.class, true), new EntryEntity(EntitySheep.class, true),
				new EntryEntity(EntityCow.class, true), new EntryEntity(EntityPolarBear.class, true)
			},
    		"List of mobs that can be ridden on by normal-sized riders (not all entities can be controlled by their rider).");

		public final EntityListConfig MOUNT_LIST_SMALL = this.prop("mount_list_small", new EntryEntity[] {
				new EntryEntity(EntityChicken.class, true), new EntryEntity(EntityRabbit.class, true)
			},
    		"List of mobs that can be ridden on by small riders or normal-sized riders that are babies (not all entities can be controlled by their rider).");

		public final EntityListConfig RIDER_LIST = this.prop("rider_list", new EntryEntity[] {
				new EntryEntity(EntityZombie.class, 0.05F), new EntryEntity(EntitySkeleton.class, 0.1F),
				new EntryEntity(EntityCreeper.class, 0.05F), new EntryEntity(EntityWitch.class, 0.05F),
				new EntryEntity(EntityPigZombie.class, 0.1F)
			},
			"List of mobs that can ride normal-sized mounts and the chance for them to gain the rider AI.\n"
			+ "Note that the entity must have task-based AI enabled.");

		public final EntityListConfig RIDER_LIST_SMALL = this.prop("rider_list_small", new EntryEntity[] { },
			"List of mobs that can only ride small mounts or normal-sized mounts that are babies and the chance for them to gain the rider AI.\n"
			+ "Note that the entity must have task-based AI enabled.");

	}

	public final SPECIAL_AI SPECIAL_AI = new SPECIAL_AI();
	public class SPECIAL_AI extends PropertyCategory {
		@Override
		public String name() { return "special_ai"; }
		@Override
		protected String comment() {
			return "Options to control the types of special AI and their weighted chances of occurring.";
		}

        @Override
		protected double[] defaultDblRange() {
			return PropertyCategory.RDBL_ONE;
		}

        public final EntityListConfig MOB_LIST = this.prop("_mob_list", new EntryEntity[] {
        		new EntryEntity(EntityZombie.class, 0.05F, 0.05F), new EntryEntity(EntitySkeleton.class, 0.2F, 0.05F),
        		new EntryEntity(EntityPigZombie.class, 0.1F, 0.05F, 0.02F)
        	},
        	"List of mobs that can gain special AI patterns and their chances to gain those patterns.\n"
        	+ "You can specify multiple chances for each entity - each chance will be rolled and multiple AIs can stack.\n"
        	+ "Note that the entity must have task-based AI enabled.");

        {
        	// Weights are dynamically created and stored externally
        	SpecialAIHandler.AI_WEIGHT = 0;
	        for (ISpecialAI ai : SpecialAIHandler.SPECIAL_AI_LIST) {
	            ai.setWeight(Math.max(0, Properties.config.get(this.name(), "ai_" + ai.getName(), 1, null, PropertyCategory.RINT_POS0[0], PropertyCategory.RINT_POS0[1]).getInt()));
	            SpecialAIHandler.AI_WEIGHT += ai.getWeight();
	        }
        }

	}

	public final VILLAGES VILLAGES = new VILLAGES();
	public class VILLAGES extends PropertyCategory {
		@Override
		public String name() { return "villages"; }
		@Override
		protected String comment() {
			return "Options to control village aggression and reputation.";
		}

        @Override
		protected double[] defaultDblRange() {
			return PropertyCategory.RDBL_ONE;
		}

        public final double BLOCK_ATTACK_CHANCE = this.prop("block_aggression_chance", 0.1,
        	"Chance for you to be marked as an aggressor (to be attacked) when you break a block in a village that is not on the \"block_blacklist\" if your reputation is low enough.");

        public final int BLOCK_ATTACK_LIMIT = this.prop("block_aggression_limit", -5,
        	"The \"block_aggression_chance\" only triggers if your reputation in the village is less than or equal to this limit (the same limit is also used for special blocks).",
        	-30, 10);

        public final HashSet<TargetBlock> BLOCK_BLACKLIST = this.prop("block_blacklist", new Block[] {
        		Blocks.STONE, Blocks.DIRT, Blocks.SAND, Blocks.MYCELIUM, Blocks.LEAVES, Blocks.LEAVES2, Blocks.SNOW_LAYER, Blocks.ICE,
				Blocks.OAK_DOOR, Blocks.BIRCH_DOOR, Blocks.SPRUCE_DOOR, Blocks.JUNGLE_DOOR, Blocks.ACACIA_DOOR, Blocks.DARK_OAK_DOOR,
				Blocks.OAK_FENCE_GATE, Blocks.BIRCH_FENCE_GATE, Blocks.SPRUCE_FENCE_GATE, Blocks.JUNGLE_FENCE_GATE, Blocks.ACACIA_FENCE_GATE, Blocks.DARK_OAK_FENCE_GATE,
				Blocks.WHEAT, Blocks.POTATOES, Blocks.CARROTS, Blocks.NETHER_WART, Blocks.PUMPKIN_STEM, Blocks.MELON_STEM,
				Blocks.PUMPKIN, Blocks.MELON_BLOCK, Blocks.REEDS, Blocks.COCOA, Blocks.CACTUS, Blocks.CHORUS_PLANT, Blocks.CHORUS_FLOWER,
				Blocks.TALLGRASS, Blocks.BROWN_MUSHROOM, Blocks.RED_MUSHROOM, Blocks.YELLOW_FLOWER, Blocks.DOUBLE_PLANT, Blocks.DEADBUSH
	        },
            "Specific blocks that will NOT aggro villagers when broken.");

        public final double BLOCK_REP_CHANCE = this.prop("block_rep_loss_chance", 0.1,
        	"Chance for you to lose 1 reputation when you break a block in a village that is not on the \"block_blacklist\" if your reputation is low enough.");

        public final int BLOCK_REP_LIMIT = this.prop("block_rep_loss_limit", 8,
        	"The \"block_rep_loss_chance\" only triggers if your reputation in the village is equal to or less than this limit.",
        	-30, 10);

        public final double SP_BLOCK_ATTACK_CHANCE = this.prop("block_special_aggression_chance", 1.0,
        	"Chance for you to be marked as an aggressor (to be attacked) when you break a block in a village that is on the \"block_special_list\" if your reputation is low enough.");

        public final HashSet<TargetBlock> SP_BLOCK_LIST = this.prop("block_special_list", new Block[] { Blocks.EMERALD_BLOCK },
        	"Specific blocks that use separate chances for aggression and rep loss from other blocks.");

        public final double SP_BLOCK_REP_CHANCE = this.prop("block_special_rep_loss_chance", 1.0,
        	"Chance for you to lose 1 reputation when you break a block in a village that is on the \"block_special_list\".");

        public final HashSet<TargetBlock> BLOCK_WHITELIST = this.prop("block_whitelist", new Block[] { },
        	"Specific blocks that WILL aggro villagers when broken. If any blocks are specified here, they will then be the only blocks that aggro villagers (i.e., trigger rep loss and aggression).");

        public final double HELP_REP_CHANCE = this.prop("help_rep_chance", 0.2,
        	"Chance for you to earn 1 reputation for each monster killed near a village. The only reasonable way to restore rep from below -14 with \"villagers_defend\" enabled.");

        public final boolean HOUSE_REP = this.prop("house_rep", true,
        	"If true, all players known to a village will gain 1 rep when a new house is added to a village and lose 1 rep when a house is lost. Highly recommended to keep \"refresh_houses\" enabled when this is.");

        public final boolean REFRESH_HOUSES = this.prop("refresh_houses", true,
        	"If true, houses will stay a part of a village permanently once added (until their doors are destroyed or all villagers in the village are killed), instead of being constantly added/removed as villagers move around.");

        public final boolean VILLAGERS_DEFEND = this.prop("villagers_defend", true,
        	"If true, villagers will defend their village by attacking its aggressors and players with bad reputation (<= -15), just like iron golems do.");

	}


	private static Configuration config;
	private static Properties INSTANCE;

    // Contains basic implementations for all config option types, along with some useful constants.
	private static abstract class PropertyCategory {

		/** Range: { -INF, INF } */
		protected static final double[] RDBL_ALL = { Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY };
		/** Range: { 0.0, INF } */
		protected static final double[] RDBL_POS = { 0.0, Double.POSITIVE_INFINITY };
		/** Range: { 0.0, 1.0 } */
		protected static final double[] RDBL_ONE = { 0.0, 1.0 };

		/** Range: { -INF, INF } */
		protected static final float[] RFLT_ALL = { Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY };
		/** Range: { 0.0, INF } */
		protected static final float[] RFLT_POS = { 0.0F, Float.POSITIVE_INFINITY };
		/** Range: { 0.0, 1.0 } */
		protected static final float[] RFLT_ONE = { 0.0F, 1.0F };

		/** Range: { MIN, MAX } */
		protected static final int[] RINT_ALL = { Integer.MIN_VALUE, Integer.MAX_VALUE };
		/** Range: { -1, MAX } */
		protected static final int[] RINT_TOKEN_NEG = { -1, Integer.MAX_VALUE };
		/** Range: { 0, MAX } */
		protected static final int[] RINT_POS0 = { 0, Integer.MAX_VALUE };
		/** Range: { 1, MAX } */
		protected static final int[] RINT_POS1 = { 1, Integer.MAX_VALUE };
		/** Range: { 0, SRT } */
		protected static final int[] RINT_SRT_POS = { 0, Short.MAX_VALUE };
		/** Range: { 0, 255 } */
		protected static final int[] RINT_BYT_UNS = { 0, 0xff };
		/** Range: { 0, 127 } */
		protected static final int[] RINT_BYT_POS = { 0, Byte.MAX_VALUE };

		public PropertyCategory() {
	        Properties.config.addCustomCategoryComment(this.name(), this.comment());
		}

		public abstract String name();
		protected abstract String comment();

		protected double[] defaultDblRange() {
			return PropertyCategory.RDBL_POS;
		}
		protected float[] defaultFltRange() {
			return PropertyCategory.RFLT_POS;
		}
		protected int[] defaultIntRange() {
			return PropertyCategory.RINT_POS0;
		}


	    protected HashSet<TargetBlock> prop(String key, Block[] defaultValues, String comment) {
	    	return BlockHelper.newBlockSet(this.cprop(key, defaultValues, comment).getStringList());
	    }
	    protected Property cprop(String key, Block[] defaultValues, String comment) {
	    	String[] defaultIds = new String[defaultValues.length];
	    	for (int i = 0; i < defaultIds.length; i++) {
	    		defaultIds[i] = Block.REGISTRY.getNameForObject(defaultValues[i]).toString();
	    	}
	    	comment = this.amendComment(comment, "Block_Array", defaultIds, "mod_id:block_id, mod_id:block_id meta, mod_id:*");
	    	return Properties.config.get(this.name(), key, defaultIds, comment);
	    }

	    protected EntityListConfig prop(String key, EntryEntity[] defaultValues, String comment) {
	    	return new EntityListConfig(this.cprop(key, defaultValues, comment).getStringList());
	    }
	    protected Property cprop(String key, EntryEntity[] defaultValues, String comment) {
	    	String[] defaultIds = new String[defaultValues.length];
	    	for (int i = 0; i < defaultIds.length; i++) {
	    		defaultIds[i] = defaultValues[i].toString();
	    	}
	    	comment = this.amendComment(comment, "Entity_Array", defaultIds, "entity_id <extra_data>, ~entity_id <extra_data>");
	    	return Properties.config.get(this.name(), key, defaultIds, comment);
	    }

		protected boolean prop(String key, boolean defaultValue, String comment) {
	    	return this.cprop(key, defaultValue, comment).getBoolean();
	    }
		protected Property cprop(String key, boolean defaultValue, String comment) {
	    	comment = this.amendComment(comment, "Boolean", defaultValue, new Object[] { true, false });
	    	return Properties.config.get(this.name(), key, defaultValue, comment);
	    }

		protected boolean[] prop(String key, boolean[] defaultValues, String comment) {
	    	return this.cprop(key, defaultValues, comment).getBooleanList();
	    }
		protected Property cprop(String key, boolean[] defaultValues, String comment) {
	    	comment = this.amendComment(comment, "Boolean_Array", Arrays.asList(defaultValues).toArray(), new Object[] { true, false });
	    	return Properties.config.get(this.name(), key, defaultValues, comment);
	    }

		protected int prop(String key, int defaultValue, String comment) {
	    	return this.cprop(key, defaultValue, comment).getInt();
	    }
		protected int prop(String key, int defaultValue, String comment, int... range) {
	    	return this.cprop(key, defaultValue, comment, range).getInt();
	    }
		protected Property cprop(String key, int defaultValue, String comment) {
	    	return this.cprop(key, defaultValue, comment, this.defaultIntRange());
	    }
		protected Property cprop(String key, int defaultValue, String comment, int... range) {
	    	comment = this.amendComment(comment, "Integer", defaultValue, range[0], range[1]);
	    	return Properties.config.get(this.name(), key, defaultValue, comment, range[0], range[1]);
	    }

		protected int[] prop(String key, int[] defaultValues, String comment) {
	    	return this.cprop(key, defaultValues, comment).getIntList();
	    }
		protected int[] prop(String key, int[] defaultValues, String comment, int... range) {
	    	return this.cprop(key, defaultValues, comment, range).getIntList();
	    }
		protected Property cprop(String key, int[] defaultValues, String comment) {
	    	return this.cprop(key, defaultValues, comment, this.defaultIntRange());
	    }
		protected Property cprop(String key, int[] defaultValues, String comment, int... range) {
	    	comment = this.amendComment(comment, "Integer_Array", Arrays.asList(defaultValues).toArray(), range[0], range[1]);
	    	return Properties.config.get(this.name(), key, defaultValues, comment, range[0], range[1]);
	    }

		protected float prop(String key, float defaultValue, String comment) {
	    	return (float) this.cprop(key, defaultValue, comment).getDouble();
	    }
	    protected float prop(String key, float defaultValue, String comment, float... range) {
	    	return (float) this.cprop(key, defaultValue, comment, range).getDouble();
	    }
	    protected Property cprop(String key, float defaultValue, String comment) {
	    	return this.cprop(key, defaultValue, comment, this.defaultFltRange());
	    }
	    protected Property cprop(String key, float defaultValue, String comment, float... range) {
	    	comment = this.amendComment(comment, "Float", defaultValue, range[0], range[1]);
	    	return Properties.config.get(this.name(), key, this.prettyFloatToDouble(defaultValue), comment, this.prettyFloatToDouble(range[0]), this.prettyFloatToDouble(range[1]));
	    }

		protected double prop(String key, double defaultValue, String comment) {
	    	return this.cprop(key, defaultValue, comment).getDouble();
	    }
	    protected double prop(String key, double defaultValue, String comment, double... range) {
	    	return this.cprop(key, defaultValue, comment, range).getDouble();
	    }
	    protected Property cprop(String key, double defaultValue, String comment) {
	    	return this.cprop(key, defaultValue, comment, this.defaultDblRange());
	    }
	    protected Property cprop(String key, double defaultValue, String comment, double... range) {
	    	comment = this.amendComment(comment, "Double", defaultValue, range[0], range[1]);
	    	return Properties.config.get(this.name(), key, defaultValue, comment, range[0], range[1]);
	    }

	    protected double[] prop(String key, double[] defaultValues, String comment) {
	    	return this.cprop(key, defaultValues, comment).getDoubleList();
	    }
	    protected double[] prop(String key, double[] defaultValues, String comment, double... range) {
	    	return this.cprop(key, defaultValues, comment, range).getDoubleList();
	    }
	    protected Property cprop(String key, double[] defaultValues, String comment) {
	    	return this.cprop(key, defaultValues, comment, this.defaultDblRange());
	    }
	    protected Property cprop(String key, double[] defaultValues, String comment, double... range) {
	    	comment = this.amendComment(comment, "Double_Array", Arrays.asList(defaultValues).toArray(), range[0], range[1]);
	    	return Properties.config.get(this.name(), key, defaultValues, comment, range[0], range[1]);
	    }

	    protected String prop(String key, String defaultValue, String comment, String valueDescription) {
	    	return this.cprop(key, defaultValue, comment, valueDescription).getString();
	    }
	    protected String prop(String key, String defaultValue, String comment, String... validValues) {
	    	return this.cprop(key, defaultValue, comment, validValues).getString();
	    }
	    protected Property cprop(String key, String defaultValue, String comment, String valueDescription) {
	    	comment = this.amendComment(comment, "String", defaultValue, valueDescription);
	    	return Properties.config.get(this.name(), key, defaultValue, comment, new String[0]);
	    }
	    protected Property cprop(String key, String defaultValue, String comment, String... validValues) {
	    	comment = this.amendComment(comment, "String", defaultValue, validValues);
	    	return Properties.config.get(this.name(), key, defaultValue, comment, validValues);
	    }

	    private String amendComment(String comment, String type, Object[] defaultValues, String description) {
	    	return this.amendComment(comment, type, "{ " + this.toReadable(defaultValues) + " }", description);
	    }
	    private String amendComment(String comment, String type, Object[] defaultValues, Object min, Object max) {
	    	return this.amendComment(comment, type, "{ " + this.toReadable(defaultValues) + " }", min, max);
	    }
	    private String amendComment(String comment, String type, Object[] defaultValues, Object[] validValues) {
	    	return this.amendComment(comment, type, "{ " + this.toReadable(defaultValues) + " }", validValues);
	    }
	    private String amendComment(String comment, String type, Object defaultValue, String description) {
	    	return new StringBuilder(comment).append("\n   >> ").append(type).append(":[ ")
	    		.append("Value={ ").append(description).append(" }, Default=").append(defaultValue)
	    		.append(" ]").toString();
	    }
	    private String amendComment(String comment, String type, Object defaultValue, Object min, Object max) {
	    	return new StringBuilder(comment).append("\n   >> ").append(type).append(":[ ")
	    		.append("Range={ ").append(min).append(", ").append(max).append(" }, Default=").append(defaultValue)
	    		.append(" ]").toString();
	    }
	    private String amendComment(String comment, String type, Object defaultValue, Object[] validValues) {
	    	if (validValues.length < 2) throw new IllegalArgumentException("Attempted to create config with no options!");

	    	return new StringBuilder(comment).append("\n   >> ").append(type).append(":[ ")
	    		.append("Valid_Values={ ").append(this.toReadable(validValues)).append(" }, Default=").append(defaultValue)
	    		.append(" ]").toString();
	    }

	    private double prettyFloatToDouble(float f) {
	    	return Double.parseDouble(Float.toString(f));
	    }
	    private String toReadable(Object[] array) {
	    	if (array.length <= 0) return "";

	    	StringBuilder commentBuilder = new StringBuilder();
    		for (Object value : array) {
    			commentBuilder.append(value).append(", ");
    		}
    		return commentBuilder.substring(0, commentBuilder.length() - 2).toString();
	    }
	}
}
