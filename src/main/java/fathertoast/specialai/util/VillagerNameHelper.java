package fathertoast.specialai.util;

import fathertoast.crust.api.lib.NBTHelper;
import fathertoast.specialai.SpecialAI;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.behavior.TradeWithVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.checkerframework.checker.units.qual.C;

import java.util.*;

/**
 * This class handles random name generation for villagers, influenced by the villager's profession.
 */
@SuppressWarnings( "SpellCheckingInspection" )
public final class VillagerNameHelper {
    /** Villager first name tag key. */
    private static final String TAG_FIRST_NAME = "FirstName";
    /** Mod data tag key. */
    private static final String TAG_MOD_DATA = SpecialAI.MOD_ID + "Data";
    
    /** 1-in-chances for each name part to be added. */
    private static final int[] NAME_PART_CHANCES = { 1, 1, 4, 16, 64 };
    
    //TODO Expose these in configs (mainly to allow name support for mod-added professions)
    private static final Map<String, String[]> CAREER_TITLES = new HashMap<>();
    private static final Map<String, List<String>> NAME_POOLS = new HashMap<>();


    /**
     * Generates a new, random name for the given villager and sets it as the entity's display name.<br>
     * If a first name already exists in the villager's NBT, we only generate a new job title and merge
     * it with the existing name, making sure the original first name never
     * changes even if a new profession is picked.
     */
    public static void setVillagerName( RandomSource random, Villager entity, VillagerData data ) {
        // Get the villager's profession and career (sub-profession)
        CompoundTag tag = new CompoundTag();
        entity.save( tag );

        CompoundTag modData = NBTHelper.getOrCreateCompound( NBTHelper.getForgeData( entity ), TAG_MOD_DATA );

        VillagerProfession prof = data.getProfession();
        String profKey = getKey( prof );
        
        // Build the pool of appropriate name parts
        List<String> namePool = new ArrayList<>( NAME_POOLS.get( "" ) );
        if( NAME_POOLS.containsKey( profKey ) ) {
            namePool.addAll( NAME_POOLS.get( profKey ) );
        }
        
        // Get the pool of job titles
        String[] titles = CAREER_TITLES.get( profKey );
        
        if( titles == null ) {
            SpecialAI.LOG.warn( "Naming villager with unknown profession '{}'", profKey );
            List<String[]> values = new ArrayList<>( CAREER_TITLES.values() );
            titles = values.get( random.nextInt( values.size() ) );
        }
        
        // Begin building the name
        StringBuilder name = new StringBuilder( 32 );

        // No existing name in NBT, generate a fresh one
        if ( !modData.contains( TAG_FIRST_NAME ) ) {
            // Build the main name out of a random number of parts and then capitalize it
            for ( int chance : NAME_PART_CHANCES ) {
                if ( random.nextInt( chance ) == 0 ) {
                    name.append( namePool.get( random.nextInt( namePool.size() ) ));
                }
            }
            name.setCharAt( 0, Character.toUpperCase( name.charAt( 0 ) ));

            // Mc/Mac names
            if ( random.nextInt( 100 ) == 0 ) {
                name.insert( 0, random.nextBoolean() ? "Mc" : "Mac" );
            }
            // Save this to NBT for later
            String firstName = name.toString();
            modData.putString( TAG_FIRST_NAME, firstName );
        }
        else {
            // Append the existing name from NBT
            name.append( modData.getString( TAG_FIRST_NAME ) );
        }

        // Add job title
        String title = titles[random.nextInt( titles.length )];
        if( random.nextInt( 3 ) == 0 ) {
            name.append( " the " ).append( title );
        }
        else {
            name.insert( 0, " " ).insert( 0, title );
        }
        entity.setCustomName( Component.literal( name.toString() ) );
    }

    /**
     * @return The registry name of the given profession as a String,
     *         if it exists in the Forge registry. Throws an exception otherwise.
     */
    private static String getKey( VillagerProfession profession ) {
        return SpecialAI.toString( ForgeRegistries.VILLAGER_PROFESSIONS.getKey( profession ) );
    }
    
    /**
     * Constructs first name and profession/career names for
     * all vanilla villager professions.<br><br>
     * Called from {@link fathertoast.specialai.ModEventHandler#setup(FMLCommonSetupEvent)}
     */
    public static void initCareerComponents() {
        String profKey;
        
        NAME_POOLS.put( "", Arrays.asList(
                "grab", "thar", "ger", "ald", "mas", "on", "o", "din", "thor", "jon", "ath", "an", "burb", "en",
                "a", "e", "i", "u", "hab", "bloo", "ena", "dit", "aph", "ern", "bor", "dav", "id", "toast", "son", "dottir",
                "for", "wen", "lob", "ed", "die", "van", "y", "zap", "ear", "ben", "don", "bran", "gro", "jen", "bob",
                "ette", "ere", "man", "qua", "bro", "cree", "per", "skel", "ton", "zom", "bie", "wolf", "end", "er", "pig",
                "sil", "ver", "fish", "cow", "chic", "ken", "sheep", "lla", "rab", "bit", "squid", "hell"
        ) );

        profKey = getKey( VillagerProfession.FARMER );
        CAREER_TITLES.put( profKey, new String[] {
                "Farmer", "Farmer", "Gardener", "Gardener", "Seedmonger", "Seedmonger",
                "Cropper", "Greenskeeper", "Agriculturalist", "Horticulturist", "Seedsman", "Landscaper", "Seedvendor"
        } );
        NAME_POOLS.put( profKey, Arrays.asList(
                "farm", "garden", "soil", "seed", "plant", "crop", "green", "wheat", "carrot", "potato", "beet", "hoe"
        ) );

        profKey = getKey( VillagerProfession.FISHERMAN );
        CAREER_TITLES.put( profKey, new String[] {
                "Fisherman", "Fisherman", "Fisher", "Fisher", "Angler", "Angler", "Fishmonger", "Fishmonger",
                "Piscator", "Rodman", "Troller", "Trawler", "Fishman", "Skipper", "Captain", "Fishvendor"
        } );
        NAME_POOLS.put( profKey, Arrays.asList(
                "fish", "sea", "boat", "salt", "rod", "cast", "net", "gold", "treasure", "sunken", "oar", "plank", "beard"
        ) );

        profKey = getKey( VillagerProfession.SHEPHERD );
        CAREER_TITLES.put( profKey, new String[] {
                "Shepherd", "Shepherd", "Rancher", "Rancher", "Herder", "Herder",
                "Cowhand", "Herdsman", "Cowpuncher", "Sheepsman", "Sheeper", "Cooper", "Stockman"
        } );
        NAME_POOLS.put( profKey, Arrays.asList(
                "herd", "ranch", "brand", "cow", "sheep", "chicken", "pig", "horse", "fence", "gate", "post", "hay"
        ) );

        profKey = getKey( VillagerProfession.FLETCHER );
        CAREER_TITLES.put( profKey, new String[] {
                "Fletcher", "Fletcher", "Bowyer", "Bowyer", "Arrowvendor", "Arrowvendor",
                "Arrowman", "Bowman", "Arrowcrafter", "Bowcrafter", "Bowvendor", "Arrowmonger", "Bowmonger"
        } );
        NAME_POOLS.put( profKey, Arrays.asList(
                "arrow", "feather", "string", "bow", "archer", "shoot", "poke"
        ) );

        profKey = getKey( VillagerProfession.LIBRARIAN );
        CAREER_TITLES.put( profKey, new String[] {
                "Librarian", "Librarian", "Curator", "Curator", "Bookbinder", "Bookbinder", "Bookvendor", "Bookvendor",
                "Cataloger", "Researcher", "Bookman", "Bibliothecary", "Recordkeeper", "Bookmonger"
        } );
        NAME_POOLS.put( profKey, Arrays.asList(
                "book", "shelf", "wise", "read", "bind", "record", "biblio", "libro", "lib"
        ) );

        profKey = getKey( VillagerProfession.CARTOGRAPHER );
        CAREER_TITLES.put( profKey, new String[] {
                "Cartographer", "Cartographer", "Mapmaker", "Mapmaker", "Explorer", "Explorer", "Mapvendor", "Mapvendor",
                "Topographer", "Mapper", "Adventurer", "Pathfinder", "Pioneer", "Mapmonger", "Wayfinder"
        } );
        NAME_POOLS.put( profKey, Arrays.asList(
                "map", "dora", "venture", "carto", "topo", "way", "path", "find", "treasure"
        ) );

        profKey = getKey( VillagerProfession.CLERIC );
        CAREER_TITLES.put( profKey, new String[] {
                "Cleric", "Cleric", "Cleric", "Priest", "Priest", "Priest",
                "Father", "Mother", "Preacher", "Reverend", "Rabbi", "Chaplain", "Minister", "Churchman", "Healer", "Storyteller",
                "Mystic", "Enchanter", "Diviner", "Oracle", "Augur", "Prophet", "Seer", "Holyman", "Sage", "Wiseman", "Elder"
        } );
        NAME_POOLS.put( profKey, Arrays.asList(
                "holy", "wise", "mystic", "secret", "cleric", "priest", "div"
        ) );

        profKey = getKey( VillagerProfession.ARMORER );
        CAREER_TITLES.put( profKey, new String[] {
                "Armorsmith", "Armorsmith", "Armorer", "Armorer", "Armorworker", "Armorworker", "Armorvendor", "Armorvendor",
                "Armormaster", "Armorman", "Armorcrafter", "Plateworker", "Platesmith", "Platecrafter", "Shieldworker", "Shieldsmith", "Shieldcrafter",
                "Armormonger", "Platemonger", "Platevendor", "Shieldmonger", "Shieldvendor"
        } );
        NAME_POOLS.put( profKey, Arrays.asList(
                "armor", "plate", "shield", "smith", "forge", "fire", "anvil"
        ) );

        profKey = getKey( VillagerProfession.WEAPONSMITH );
        CAREER_TITLES.put( profKey, new String[] {
                "Weaponsmith", "Weaponsmith", "Swordsmith", "Swordsmith", "Weaponmonger", "Weaponmonger",
                "Weaponcrafter", "Weaponmaker", "Weaponmaster", "Weaponman", "Swordcrafter", "Swordmaker", "Swordmaster",
                "Weaponvendor", "Swordmonger", "Swordvendor"
        } );
        NAME_POOLS.put( profKey, Arrays.asList(
                "weapon", "sword", "poker", "smith", "forge", "fire", "anvil"
        ) );

        profKey = getKey( VillagerProfession.TOOLSMITH );
        CAREER_TITLES.put( profKey, new String[] {
                "Toolsmith", "Toolsmith", "Toolmaker", "Toolmaker", "Toolvendor", "Toolvendor",
                "Toolworker", "Toolcrafter", "Toolman", "Toolmaster", "Tooler", "Artisan", "Toolmonger"
        } );
        NAME_POOLS.put( profKey, Arrays.asList(
                "tool", "pick", "axe", "shovel", "hoe", "hammer", "smith", "forge", "fire", "anvil"
        ) );

        profKey = getKey( VillagerProfession.BUTCHER );
        CAREER_TITLES.put( profKey, new String[] {
                "Butcher", "Butcher", "Meatman", "Meatman", "Meatmonger", "Meatmonger",
                "Meatvendor", "Cowslayer", "Sheepslayer", "Chickenslayer", "Rabbitslayer"
        } );
        NAME_POOLS.put( profKey, Arrays.asList(
                "butcher", "blood", "meat", "chop", "cleave", "hook"
        ) );

        profKey = getKey( VillagerProfession.LEATHERWORKER );
        CAREER_TITLES.put( profKey, new String[] {
                "Tanner", "Tanner", "Leathermaker", "Leathermaker", "Leathermonger", "Leathermonger",
                "Leatherman", "Skinner", "Cowskinner", "Sheepskinner", "Hidetanner", "Leathertanner", "Leathervendor"
        } );
        NAME_POOLS.put( profKey, Arrays.asList(
                "tan", "leather", "hide", "skin", "strip", "chop", "cleave", "blood"
        ) );

        profKey = getKey( VillagerProfession.NITWIT );
        CAREER_TITLES.put( profKey, new String[] {
                "Nitwit", "Blockhead", "Fool", "Dimwit", "Dork", "Dunce", "Jerk", "Moron", "Pinhead", "Ninny", "Simpleton"
        } );
        NAME_POOLS.put( profKey, Arrays.asList(
                "fool", "dork", "jerk", "wit", "genius", "head", "smart", "wise"
        ) );

        profKey = getKey( VillagerProfession.NONE );
        CAREER_TITLES.put( profKey, new String[] {
                "Jobless", "Noob", "Lazy",
        } );
        NAME_POOLS.put( profKey, Arrays.asList(
                "unemployed"
        ) );
    }
    
    // This is a static-only helper class.
    private VillagerNameHelper() {}
}