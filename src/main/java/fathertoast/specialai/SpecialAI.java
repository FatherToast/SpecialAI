package fathertoast.specialai;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistryEntry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;

/**
 * The core of the mod. Contains basic info about the mod and hooks into FML.
 */
@Mod( SpecialAI.MOD_ID )
public class SpecialAI {
    
    /* Feature List:
     * (KEY: - = complete in current version, o = incomplete feature from previous version,
     *   + = incomplete new feature, ? = feature to consider adding)
     *
     * GENERAL
     *  - animals
     *      - depacify list (fight back when attacked)
     *      - aggressive list
     *      - idle eat breeding items
     *      ? fix squid not pathing at all and fish moving super slowly in xz-plane
     *  - jockeys
     *  - door breaking
     *      + fix pathfinding to path through all valid doors - is this possible without a mixin on WalkNodeProcessor?
     *  + dig (break blocks to reach target)
     *      ? seems to require complete custom pathfinder
     *  ? use item for attack
     *      + replace attack ai with "item use", which simulates item use for its "attack"
     *      ? probably needs flexible config (list of items to use, then one category auto-generated per listed item)
     *  ? flock (mobs hunt in packs)
     *  ? spread fire (ignite flammable blocks when on fire and chasing a target)
     *
     * REACTIONS
     *  - avoid explosions
     *  - call for help (on death)
     *  - dodge arrows
     *  + aggro when sniped (enable revenge targeting beyond follow range)
     *  ? reactive potions (potion drinking from a small "inventory" - similar to witch logic)
     *      ? how the heck to animate this?
     *      ? chance to drop potions
     *      + health (damage for undead) when low on health
     *      + fire resistance when on fire or taking fire damage
     *      + water breathing when drowning
     *      ? strength when target is armored/enchanted
     *      ? speed when target is far away
     *      ? invisibility when shot by arrows
     *  ? stalk (try to hide when target is looking)
     *
     * IDLE BEHAVIORS
     *  - griefing (break blocks)
     *  - fiddling (right-click blocks)
     *  + creepers hide in chests
     *  ? walk into portals
     *
     * ELITE AI
     *  - leap
     *  - jump
     *  - sprint
     *  - slam
     *  - barrage
     *  - charge
     *  - thief
     *  - shaman
     *  - spawner
     *  - throw ally
     *  - throw enemy
     *  + fishing (wields a fishing rod and uses it against its target)
     *  + grappling (similar to fishing, but pulls the mob to target instead, mob may mount the target?)
     *
     * VILLAGES - TODO all these features must be re-evaluated due to village overhaul
     *  o rep is never forgotten (vanilla bug fix)
     *  o hostile against very low rep players
     *  o rep affected by building or breaking certain blocks in village
     *  o chance to attack when breaking blocks in village
     *  o rep gained by killing hostile mobs near village
     *  o command to check your village rep
     *
     * BOSSES
     *  + name influenced by entity type and abilities
     *  + "unique" item
     *      + equipped, guaranteed drop
     *      + has a special enchantment/modifier (may be otherwise unobtainable, may have drawback)
     *      + named based on boss and special (prefixed by "<boss_name>'s")
     *  ? nemesis system
     *
     * AI EDITOR
     *  ? enable complete (as possible) editing of pre-existing ai
     *  ? is this too much to include with this mod? maybe have the whole system only enabled by a config option
     */
    
    /** The mod id and namespace used by this mod. */
    public static final String MOD_ID = "specialai";
    
    /** The base lang key for translating text from this mod. */
    public static final String LANG_KEY = SpecialAI.MOD_ID + ".";
    
    /** The logger used by this mod. */
    public static final Logger LOG = LogManager.getLogger();
    
    /** @return Returns a Forge registry entry as a string, or "null" if it is null. */
    public static String toString( @Nullable ForgeRegistryEntry<?> regEntry ) { return regEntry == null ? "null" : toString( regEntry.getRegistryName() ); }
    
    /** @return Returns the resource location as a string, or "null" if it is null. */
    public static String toString( @Nullable ResourceLocation res ) { return res == null ? "null" : res.toString(); }
}