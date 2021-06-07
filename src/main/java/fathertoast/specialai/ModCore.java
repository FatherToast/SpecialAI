package fathertoast.specialai;

import fathertoast.specialai.config.Config;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The core of the mod. Contains basic info about the mod, initializes configs, and hooks into FML.
 */
@Mod( ModCore.MOD_ID )
public class ModCore {
    /** The mod id and namespace used by this mod. */
    public static final String MOD_ID = "specialai";
    /** The base lang key for translating text from this mod. */
    public static final String LANG_KEY = ModCore.MOD_ID + ".";
    
    /** The logger used by this mod. */
    public static final Logger LOG = LogManager.getLogger();
    
    /* TODO LIST:
     *  - Re-implement features:
     *      + Depacify/aggressive
     *      + Eat dropped items
     *      + Door breaking
     *      + Idle griefing/fiddling
     *      + Reactions
     *          - Avoid explosions
     *          - Call for help (on death)
     *          - Dodge arrows (plus NEW - better reaction to sniping)
     *      + Jockeys
     *      + Villages
     *      + Elite AI
     *          - Shaman
     *          - Jump/Leap
     *          - Sprint
     *          - Barrage
     *          - Charge
     *          - Thief
     *          - Spawner
     *          - Throw allies/enemies
     *  - Improve ai application configs, such as blacklist for elite ai
     *
     * Primary features:
     *  - Dig: break down walls when unable to path to target
     *  - Stalk: try to hide behind blocks when target is looking in their general direction
     *  - Portals: walk into nearby portals (part of idle fiddling?)
     *  - Counter-potions: reactive potion drinking (similar to witches) from a small "inventory"
     *      + health (damage for undead) when low on health
     *	    + fire resistance when on fire or taking fire damage
     *	    + water breathing when drowning
     *	    ? strength when target is armored/enchanted
     *	    ? invisibility when shot by arrows
     *  - Bosses: Rare, powerful monsters
     *      + Name influenced by entity type
     *      + Uses a "unique" item that is a guaranteed drop
     *      + Unique item is enchanted and has a special enchantment/modifier
     *      + The special modifier is more powerful than otherwise obtainable, but may come with a drawback
     *      + Unique item is named based on its special (prefixed by "<bossname>'s")
     *      + Uses the counter-potion system
     *	? Flock: hunt together
     *  ? Spread fire: ignite flammable blocks when on fire and chasing a target
     *
     * Utility features:
     *  - More editing for AI, particularly for editing pre-existing AI
     *  - More effective configs, more per-entity-id options
     */
    
    public ModCore() {
        Config.initialize();
    }
}