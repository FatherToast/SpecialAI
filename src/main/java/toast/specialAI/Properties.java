package toast.specialAI;

import java.util.HashMap;
import java.util.Random;

import net.minecraftforge.common.config.Configuration;
import toast.specialAI.ai.special.ISpecialAI;
import toast.specialAI.ai.special.SpecialAIHandler;

/**
 * This helper class automatically creates, stores, and retrieves properties.
 * Supported data types:
 * String, boolean, int, double
 *
 * Any property can be retrieved as an Object or String.
 * Any non-String property can also be retrieved as any other non-String property.
 * Retrieving a number as a boolean will produce a randomized output depending on the value.
 */
public abstract class Properties {
    // Mapping of all properties in the mod to their values.
    private static final HashMap<String, Object> map = new HashMap();
    // Common category names.
    public static final String GENERAL = "_general";
    public static final String JOCKEYS = "jockeys";
    public static final String GRIEFING = "passive_griefing";
    public static final String SPECIAL_AI = "special_ai";
    public static final String VILLAGES = "villages";

    // Initializes these properties.
    public static void init(Configuration config) {
        config.load();

        Properties.add(config, Properties.GENERAL, "avoid_explosions", true, "(True/false) If true, all mobs will try to avoid TNT and creepers about to explode. Default is true.");
        Properties.add(config, Properties.GENERAL, "break_speed", 0.5, "(0.0-INFINITY) The block breaking speed multiplier for mobs, relative to the player's block breaking speed. Default is 50% speed.");
        Properties.add(config, Properties.GENERAL, "call_for_help", true, "(True/false) If true, all mobs will call for help from nearby mobs of the same type when struck. Default is true.");
        Properties.add(config, Properties.GENERAL, "call_for_help_on_death", 0.2, "(0.0-1.0) Chance for mobs to call for help from a killing blow. Default is 20% chance.");
        Properties.add(config, Properties.GENERAL, "depacify_aggressive_chance", 0.005, "(0.0-1.0) Chance for a depacify list to be aggressive, instead of just neutral. Default is 0.5% chance.");
        Properties.add(config, Properties.GENERAL, "depacify_list", "Chicken,Cow,Pig,Sheep", "(Entity list) Comma-separated list of passive mobs (by entity id) that are made \"neutral\". You may put a tilde (~) in front of any entity id to make it specific. Specific entity ids will not count any entities extending (i.e., based on) the specified entity.\nMay or may not work on mod mobs. Defaults to any kind of chicken, cow, pig, and sheep.");
        Properties.add(config, Properties.GENERAL, "eat_breeding_items", true, "(True/false) If true, passive mobs will seek out and eat the items used to breed them laying on the floor. Default is true.");
        Properties.add(config, Properties.GENERAL, "eating_heals", true, "(True/false) If true, when mobs eat breeding items off the floor, they will regain health like wolves. Default is true.");

        Properties.add(config, Properties.JOCKEYS, "_rider_chance", 0.1, "(0.0-1.0) Chance for a valid rider mob to actually get the rider AI. Default is 10% chance.");
        Properties.add(config, Properties.JOCKEYS, "mount_list", "Spider,Pig,Sheep,Cow", "(Entity list) List of mobs that can be ridden on by normal-sized riders (normally non-aggressive mobs must have pathfinding AI enabled). Defaults to any kind of spider, pig, sheep, or cow.");
        Properties.add(config, Properties.JOCKEYS, "mount_list_small", "Chicken", "(Entity list) List of mobs that can be ridden on by small riders or normal-sized riders that are babies (normally non-aggressive mobs must have pathfinding AI enabled). Defaults to any kind of chicken.");
        Properties.add(config, Properties.JOCKEYS, "rider_list", "Zombie,Skeleton,Creeper", "(Entity list) List of mobs that can ride normal-sized mounts (note that the entity must have pathfinding AI enabled). Defaults to any kind of zombie, skeleton, or creeper.");
        Properties.add(config, Properties.JOCKEYS, "rider_list_small", "", "(Entity list) List of mobs that can only ride small mounts (note that the entity must have pathfinding AI enabled). Defaults to none.");

        Properties.add(config, Properties.GRIEFING, "_enabled", true, "(True/false) If true, mobs will passively grief you while not doing anything else. Default is true.");
        Properties.add(config, Properties.GRIEFING, "break_lights", true, "(True/false) If true, block breaking AI will automatically target all light sources. Default is true.");
        Properties.add(config, Properties.GRIEFING, "break_sound", true, "(True/false) If false, the lound snapping sound will not be played when greifing. Default is true.");
        Properties.add(config, Properties.GRIEFING, "grief_delay", 20, "(Integer > 0) The lower this number is, the more frequently mobs will search for things to grief. Default is 1/20 chance per tick.");
        Properties.add(config, Properties.GRIEFING, "grief_range", 3.5, "(0.0-INFINITY) Mobs' reach (from eye height) when griefing blocks. Player reach is about 4.5. Default is 3.5.");
        Properties.add(config, Properties.GRIEFING, "grief_scan_cap", 256, "(Integer > 0) The global maximum number of solid blocks to scan per tick when mobs search for a block to grief. Default is 256 blocks per tick.");
        Properties.add(config, Properties.GRIEFING, "grief_scan_cap_info", false, "(True/false) If true, the mod will print a message to your console when more than \"grief_scan_max\" entities are searching for blocks to grief. Default is false.");
        Properties.add(config, Properties.GRIEFING, "grief_scan_max", 256, "(Integer > 0) The global maximum number of entities allowed in the queue to scan. Default is 256 entities.");
        Properties.add(config, Properties.GRIEFING, "grief_scan_range", 16, "(Integer >= 0) The range at which mobs will search for blocks to grief. Default is 16 blocks.");
        Properties.add(config, Properties.GRIEFING, "leave_drops", true, "(True/false) If true, griefed blocks will leave item drops. Default is true.");
        Properties.add(config, Properties.GRIEFING, "mad_creepers", true, "(True/false) If true, creepers will be very mad about not having arms to break things with, and resort to what they know best... Default is true.");
        Properties.add(config, Properties.GRIEFING, "mob_list", "Zombie,Creeper", "(Entity list) List of mobs that gain passive griefing AI (note that the entity must have pathfinding AI enabled). Defaults to any kind of zombie or creeper.");
        Properties.add(config, Properties.GRIEFING, "requires_tools", false, "(True/false) If true, mobs will only target blocks they have the tools to harvest. Default is false.");
        Properties.add(config, Properties.GRIEFING, "target_blacklist", "", "(Block list) Specific blocks that will NOT be targeted by mobs, only really useful if \"break_lights\" is enabled to create safe light sources or prevent mobs from breaking normal world gen that produces light. Defaults to none.");
        Properties.add(config, Properties.GRIEFING, "target_blocks", "farmland,bed,crafting_table,wooden_door,trapdoor,fence_gate,ladder,cake", "(Block list) Specific blocks that will be targeted by mobs. Defaults to farmland, beds, crafting tables, wooden doors, wooden trapdoors, fence gates, ladders, and cakes.\nThis is a comma-separated list. You may specify metadata (0-15) with a space after the id, followed by the metadata. For example, \"cake\" will cause mobs to target all cakes, while \"cake 0\" will cause mobs to only target cakes that have not been partially eaten.");

        Properties.add(config, Properties.SPECIAL_AI, "_chance_1", 0.05, "(0.0-1.0) Chance for a valid mob in mob_list_1 to get a special AI pattern. Default is 5% chance.");
        Properties.add(config, Properties.SPECIAL_AI, "_chance_2", 0.05, "(0.0-1.0) Same as _chance_1, but for for mob_list_2. Multiple AIs can be stacked. Default is 5% chance.");
        Properties.add(config, Properties.SPECIAL_AI, "_chance_3", 0.2, "(0.0-1.0) Same as _chance_1, but for for mob_list_3. Multiple AIs can be stacked. Default is 20% chance.");
        Properties.add(config, Properties.SPECIAL_AI, "_mob_list_1", "Zombie,Skeleton", "(Entity list) List of mobs that can gain special AI patterns (note that the entity must have pathfinding AI enabled). Defaults to any kind of zombie or skeleton.");
        Properties.add(config, Properties.SPECIAL_AI, "_mob_list_2", "Zombie", "(Entity list) Same as mob_list_1, but uses _chance_2. Defaults to any kind of zombie.");
        Properties.add(config, Properties.SPECIAL_AI, "_mob_list_3", "Skeleton", "(Entity list) Same as mob_list_1, but uses _chance_3. Defaults to any kind of skeleton.");
        for (ISpecialAI ai : SpecialAIHandler.SPECIAL_AI_LIST) {
            ai.setWeight(Math.max(0, config.get(Properties.SPECIAL_AI, "ai_" + ai.getName(), 1).getInt(1)));
            SpecialAIHandler.AI_WEIGHT += ai.getWeight();
        }

        Properties.add(config, Properties.VILLAGES, "block_aggression_chance", 0.1, "(0.0-1.0) Chance for you to be marked as an aggressor (to be attacked) when you break a block in a village that is not on the \"block_blacklist\". Default is 10% chance.");
        Properties.add(config, Properties.VILLAGES, "block_aggression_limit", -5, "(-30-+10) The \"block_aggression_chance\" only triggers if your reputation in the village is equal to or less than this limit. Default is -5.");
        Properties.add(config, Properties.VILLAGES, "block_blacklist",
                "wooden_door,spruce_door,birch_door,jungle_door,acacia_door,dark_oak_door," +
                    "stone,grass,dirt,sand,leaves,leaves2," +
                    "wheat,potatoes,carrots,pumpkin_stem,melon_stem,reeds,nether_wart,pumpkin,melon_block,cocoa,cactus," +
                    "tallgrass,brown_mushroom,red_mushroom,yellow_flower,double_plant,deadbush,snow_layer,ice",
                "(Block list) Specific blocks that will NOT aggro villagers when broken. Defaults to wooden doors, stone, grass, dirt, sand, leaves, plants (except poppies and trees), snow cover, and ice.");
        Properties.add(config, Properties.VILLAGES, "block_rep_loss_chance", 0.1, "(0.0-1.0) Chance for you to lose 1 reputation when you break a block in a village that is not on the \"block_blacklist\". Default is 10% chance.");
        Properties.add(config, Properties.VILLAGES, "block_rep_loss_limit", 8, "(-30-+10) The \"block_rep_loss_chance\" only triggers if your reputation in the village is equal to or less than this limit. Default is 8.");
        Properties.add(config, Properties.VILLAGES, "block_special_aggression_chance", 1.0, "(0.0-1.0) Chance for you to be marked as an aggressor (to be attacked) when you break a block in a village that is on the \"block_special_list\". Only triggers if your reputation in the village is -5 or less. Default is 100% chance.");
        Properties.add(config, Properties.VILLAGES, "block_special_list", "emerald_block", "(Block list) Specific blocks that use a separate chance for aggression and rep loss from other blocks. Defaults to emerald blocks.");
        Properties.add(config, Properties.VILLAGES, "block_special_rep_loss_chance", 1.0, "(0.0-1.0) Chance for you to lose 1 reputation when you break a block in a village that is on the \"block_special_list\". Default is 100% chance.");
        Properties.add(config, Properties.VILLAGES, "block_whitelist", "", "(Block list) Specific blocks that WILL aggro villagers when broken. If any blocks are specified here, they will then be the only blocks that aggro villagers. Default is none.");
        Properties.add(config, Properties.VILLAGES, "help_rep_chance", 0.2, "(0.0-1.0) Chance for you to earn 1 reputation for each monster killed near a village. The only reasonable way to restore rep from below -14 with \"villagers_defend\" enabled. Default is 20% chance.");
        Properties.add(config, Properties.VILLAGES, "house_rep", true, "(True/false) If true, all players known to a village will gain 1 rep when a new house is added to a village and lose 1 rep when a house is lost. Highly recommended to keep \"refresh_houses\" enabled when this is. Default is true.");
        Properties.add(config, Properties.VILLAGES, "refresh_houses", true, "(True/false) If true, houses will stay a part of a village permanently once added (until their doors are destroyed or all villagers in the village are killed), instead of being constantly added/removed as villagers move around.\nAlso potentially fixes a bug causing village reputation to reset when wandering too far from a village. Default is true.");
        Properties.add(config, Properties.VILLAGES, "villagers_defend", true, "(True/false) If true, villagers will defend their village by attacking its aggressors and players with bad reputation (<= -15), just like iron golems do. Default is true.");

        config.addCustomCategoryComment(Properties.GENERAL, "General and miscellaneous options.");
        config.addCustomCategoryComment(Properties.JOCKEYS, "Options relating to which mobs should act as riders or mounts.");
        config.addCustomCategoryComment(Properties.GRIEFING, "Options to customize monsters' passive block griefing.");
        config.addCustomCategoryComment(Properties.SPECIAL_AI, "Options to control the types of special AI and their weighted chances of occurring.");
        config.addCustomCategoryComment(Properties.VILLAGES, "Options to control village aggression and reputation.");

        config.save();
    }

    // Gets the mod's random number generator.
    public static Random random() {
        return _SpecialAI.random;
    }

    // Passes to the mod.
    public static void debugException(String message) {
        _SpecialAI.debugException(message);
    }

    // Loads the property as the specified value.
    public static void add(Configuration config, String category, String field, String defaultValue, String comment) {
        Properties.map.put(category + "@" + field, config.get(category, field, defaultValue, comment).getString());
    }
    public static void add(Configuration config, String category, String field, int defaultValue) {
        Properties.map.put(category + "@" + field, Integer.valueOf(config.get(category, field, defaultValue).getInt(defaultValue)));
    }
    public static void add(Configuration config, String category, String field, int defaultValue, String comment) {
        Properties.map.put(category + "@" + field, Integer.valueOf(config.get(category, field, defaultValue, comment).getInt(defaultValue)));
    }
    public static void add(Configuration config, String category, String field, int defaultValue, int minValue, int maxValue) {
        Properties.map.put(category + "@" + field, Integer.valueOf(Math.max(minValue, Math.min(maxValue, config.get(category, field, defaultValue).getInt(defaultValue)))));
    }
    public static void add(Configuration config, String category, String field, int defaultValue, int minValue, int maxValue, String comment) {
        Properties.map.put(category + "@" + field, Integer.valueOf(Math.max(minValue, Math.min(maxValue, config.get(category, field, defaultValue, comment).getInt(defaultValue)))));
    }
    public static void add(Configuration config, String category, String field, boolean defaultValue) {
        Properties.map.put(category + "@" + field, Boolean.valueOf(config.get(category, field, defaultValue).getBoolean(defaultValue)));
    }
    public static void add(Configuration config, String category, String field, boolean defaultValue, String comment) {
        Properties.map.put(category + "@" + field, Boolean.valueOf(config.get(category, field, defaultValue, comment).getBoolean(defaultValue)));
    }
    public static void add(Configuration config, String category, String field, double defaultValue) {
        Properties.map.put(category + "@" + field, Double.valueOf(config.get(category, field, defaultValue).getDouble(defaultValue)));
    }
    public static void add(Configuration config, String category, String field, double defaultValue, String comment) {
        Properties.map.put(category + "@" + field, Double.valueOf(config.get(category, field, defaultValue, comment).getDouble(defaultValue)));
    }
    public static void add(Configuration config, String category, String field, double defaultValue, double minValue, double maxValue, String comment) {
        Properties.map.put(category + "@" + field, Double.valueOf(Math.max(minValue, Math.min(maxValue, config.get(category, field, defaultValue, comment).getDouble(defaultValue)))));
    }

    // Gets the Object property.
    public static Object getProperty(String category, String field) {
        return Properties.map.get(category + "@" + field);
    }

    // Gets the value of the property (instead of an Object representing it).
    public static String getString(String category, String field) {
        return Properties.getProperty(category, field).toString();
    }
    public static boolean getBoolean(String category, String field) {
        return Properties.getBoolean(category, field, Properties.random());
    }
    public static boolean getBoolean(String category, String field, Random random) {
        Object property = Properties.getProperty(category, field);
        if (property instanceof Boolean)
            return ((Boolean) property).booleanValue();
        if (property instanceof Integer)
            return random.nextInt( ((Number) property).intValue()) == 0;
        if (property instanceof Double)
            return random.nextDouble() < ((Number) property).doubleValue();
        Properties.debugException("Tried to get boolean for invalid property! @" + property.getClass().getName());
        return false;
    }
    public static int getInt(String category, String field) {
        Object property = Properties.getProperty(category, field);
        if (property instanceof Number)
            return ((Number) property).intValue();
        if (property instanceof Boolean)
            return ((Boolean) property).booleanValue() ? 1 : 0;
        Properties.debugException("Tried to get int for invalid property! @" + property.getClass().getName());
        return 0;
    }
    public static double getDouble(String category, String field) {
        Object property = Properties.getProperty(category, field);
        if (property instanceof Number)
            return ((Number) property).doubleValue();
        if (property instanceof Boolean)
            return ((Boolean) property).booleanValue() ? 1.0 : 0.0;
        Properties.debugException("Tried to get double for invalid property! @" + property.getClass().getName());
        return 0.0;
    }
}