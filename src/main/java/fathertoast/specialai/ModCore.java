package fathertoast.specialai;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistryEntry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;

/**
 * The core of the mod. Contains basic info about the mod, initializes configs, and hooks into FML.
 */
@Mod( ModCore.MOD_ID )
public class ModCore {
    /* TODO LIST:
     *  - Re-implement features:
     *      + Villages
     *
     * Primary features:
     *  - New Elite AIs:
     *      + slam: wields an axe, performs knockback slam "explosion" when damaged in close range
     *      + fishing: wields a fishing rod and uses it against its target
     *      + grappling: similar to fishing, but pulls the mob to target instead, mob may mount the target?
     *  - Reactions:
     *      + better response to being sniped
     *  - Door Breaking:
     *      + fix pathfinding to path through valid doors - is this possible without a mixin on WalkNodeProcessor?
     *  - Use Item Attack: Replace mob attack AI with "item use", which simulates item use for its "attack"
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
     *      + Nemesis system? (See Darkmega)
     *      + Name influenced by entity type
     *      + Uses a "unique" item that is a guaranteed drop
     *      + Unique item is enchanted and has a special enchantment/modifier
     *      + The special modifier is more powerful than otherwise obtainable, but may come with a drawback
     *      + Unique item is named based on its special (prefixed by "<boss_name>'s")
     *      + Uses the counter-potion system
     *	? Flock: hunt together
     *  ? Spread fire: ignite flammable blocks when on fire and chasing a target
     *
     * Utility features:
     *  - Fix squid not pathing at all and fish moving super slowly in xz-plane
     *  - More editing for AI, particularly for editing pre-existing AI
     *  - An extra whitelist for each elite AI to apply it directly
     *  - More effective configs, more per-entity-id options
     */
    
    /** The mod id and namespace used by this mod. */
    public static final String MOD_ID = "specialai";
    
    /** The base lang key for translating text from this mod. */
    public static final String LANG_KEY = ModCore.MOD_ID + ".";
    
    /** The less than or equal to symbol (<=). */
    public static final String LESS_OR_EQUAL = "\u2264";
    /** The greater than or equal to symbol (>=). */
    public static final String GREATER_OR_EQUAL = "\u2265";
    
    /** The logger used by this mod. */
    public static final Logger LOG = LogManager.getLogger();
    
    /** @return Returns a Forge registry entry as a string, or "null" if it is null. */
    public static String toString( @Nullable ForgeRegistryEntry<?> regEntry ) { return regEntry == null ? "null" : toString( regEntry.getRegistryName() ); }
    
    /** @return Returns the resource location as a string, or "null" if it is null. */
    public static String toString( @Nullable ResourceLocation res ) { return res == null ? "null" : res.toString(); }
}