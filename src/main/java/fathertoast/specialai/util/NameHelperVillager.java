package fathertoast.specialai.util;

import fathertoast.specialai.*;
import fathertoast.specialai.config.*;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.registry.VillagerRegistry.VillagerCareer;
import net.minecraftforge.fml.common.registry.VillagerRegistry.VillagerProfession;

import java.util.*;

public
class NameHelperVillager
{
	private static final String TAG_CAREER = "Career";
	
	// 1-in-chances for each name part to be added.
	private static final int[] NAME_PART_CHANCES = { 1, 1, 4, 16, 64 };
	
	// TODO: Expose these in configs (mainly to allow name support for mod-added professions)
	private static final Map< String, String[] >       CAREER_TITLES = new HashMap<>( );
	private static final Map< String, List< String > > NAME_POOLS    = new HashMap<>( );
	
	private static
	String getKey( VillagerProfession profession )
	{
		return profession.getRegistryName( ).toString( );
	}
	
	private static
	String getKey( VillagerCareer career, VillagerProfession profession /* because parent profession is private in career */ )
	{
		return getKey( getKey( profession ), career.getName( ) );
	}
	
	private static
	String getKey( String profKey, String careerName )
	{
		return profKey + "#" + careerName;
	}
	
	public static
	void setVillagerName( Random random, EntityVillager entity )
	{
		// Get the villager's profession and career (sub-profession)
		NBTTagCompound tag = new NBTTagCompound( );
		entity.writeEntityToNBT( tag );
		
		VillagerProfession prof   = entity.getProfessionForge( );
		VillagerCareer     career = prof.getCareer( tag.getInteger( TAG_CAREER ) - 1 );
		
		String profKey   = getKey( prof );
		String careerKey = getKey( career, prof );
		
		// Build the pool of appropriate name parts
		List< String > namePool = new ArrayList<>( NAME_POOLS.get( "" ) );
		if( NAME_POOLS.containsKey( profKey ) ) {
			namePool.addAll( NAME_POOLS.get( profKey ) );
		}
		if( NAME_POOLS.containsKey( careerKey ) ) {
			namePool.addAll( NAME_POOLS.get( careerKey ) );
		}
		
		// Get the pool of job titles
		String[] titles = CAREER_TITLES.get( careerKey );
		if( titles == null ) {
			if( Config.get( ).GENERAL.DEBUG ) {
				SpecialAIMod.log( ).warn( "Naming villager with unknown profession#career '{}'", careerKey );
			}
			List< String[] > values = new ArrayList<>( CAREER_TITLES.values( ) );
			titles = values.get( random.nextInt( values.size( ) ) );
		}
		
		// Begin building the name
		StringBuilder name = new StringBuilder( 32 );
		
		// Build the main name out of a random number of parts and then capitalize it
		for( int chance : NAME_PART_CHANCES ) {
			if( random.nextInt( chance ) == 0 ) {
				name.append( namePool.get( random.nextInt( namePool.size( ) ) ) );
			}
		}
		name.setCharAt( 0, Character.toUpperCase( name.charAt( 0 ) ) );
		
		// Mc/Mac names
		if( random.nextInt( 100 ) == 0 ) {
			name.insert( 0, random.nextBoolean( ) ? "Mc" : "Mac" );
		}
		
		// Add job title
		String title = titles[ random.nextInt( titles.length ) ];
		if( random.nextInt( 3 ) == 0 ) {
			name.append( " the " ).append( title );
		}
		else {
			name.insert( 0, " " ).insert( 0, title );
		}
		
		// Apply the name
		entity.setCustomNameTag( name.toString( ) );
	}
	
	static {
		String prof, careerKey;
		
		NAME_POOLS.put( "", Arrays.asList(
			"grab", "thar", "ger", "ald", "mas", "on", "o", "din", "thor", "jon", "ath", "an", "burb", "en",
			"a", "e", "i", "u", "hab", "bloo", "ena", "dit", "aph", "ern", "bor", "dav", "id", "toast", "son", "dottir",
			"for", "wen", "lob", "ed", "die", "van", "y", "zap", "ear", "ben", "don", "bran", "gro", "jen", "bob",
			"ette", "ere", "man", "qua", "bro", "cree", "per", "skel", "ton", "zom", "bie", "wolf", "end", "er", "pig",
			"sil", "ver", "fish", "cow", "chic", "ken", "sheep", "lla", "rab", "bit", "squid", "hell"
		) );
		
		prof = "minecraft:farmer";
		careerKey = getKey( prof, "farmer" );
		CAREER_TITLES.put( careerKey, new String[] {
			"Farmer", "Farmer", "Gardener", "Gardener", "Seedmonger", "Seedmonger",
			"Cropper", "Greenskeeper", "Agriculturalist", "Horticulturist", "Seedsman", "Landscaper", "Seedvendor"
		} );
		NAME_POOLS.put( careerKey, Arrays.asList(
			"farm", "garden", "soil", "seed", "plant", "crop", "green", "wheat", "carrot", "potato", "beet", "hoe"
		) );
		careerKey = getKey( prof, "fisherman" );
		CAREER_TITLES.put( careerKey, new String[] {
			"Fisherman", "Fisherman", "Fisher", "Fisher", "Angler", "Angler", "Fishmonger", "Fishmonger",
			"Piscator", "Rodman", "Troller", "Trawler", "Fishman", "Skipper", "Captain", "Fishvendor"
		} );
		NAME_POOLS.put( careerKey, Arrays.asList(
			"fish", "sea", "boat", "salt", "rod", "cast", "net", "gold", "treasure", "sunken", "oar", "plank", "beard"
		) );
		careerKey = getKey( prof, "shepherd" );
		CAREER_TITLES.put( careerKey, new String[] {
			"Shepherd", "Shepherd", "Rancher", "Rancher", "Herder", "Herder",
			"Cowhand", "Herdsman", "Cowpuncher", "Sheepsman", "Sheeper", "Cooper", "Stockman"
		} );
		NAME_POOLS.put( careerKey, Arrays.asList(
			"herd", "ranch", "brand", "cow", "sheep", "chicken", "pig", "horse", "fence", "gate", "post", "hay"
		) );
		careerKey = getKey( prof, "fletcher" );
		CAREER_TITLES.put( careerKey, new String[] {
			"Fletcher", "Fletcher", "Bowyer", "Bowyer", "Arrowvendor", "Arrowvendor",
			"Arrowman", "Bowman", "Arrowcrafter", "Bowcrafter", "Bowvendor", "Arrowmonger", "Bowmonger"
		} );
		NAME_POOLS.put( careerKey, Arrays.asList(
			"arrow", "feather", "string", "bow", "archer", "shoot", "poke"
		) );
		
		prof = "minecraft:librarian";
		careerKey = getKey( prof, "librarian" );
		CAREER_TITLES.put( careerKey, new String[] {
			"Librarian", "Librarian", "Curator", "Curator", "Bookbinder", "Bookbinder", "Bookvendor", "Bookvendor",
			"Cataloger", "Researcher", "Bookman", "Bibliothecary", "Recordkeeper", "Bookmonger"
		} );
		NAME_POOLS.put( careerKey, Arrays.asList(
			"book", "shelf", "wise", "read", "bind", "record", "biblio", "libro", "lib"
		) );
		careerKey = getKey( prof, "cartographer" );
		CAREER_TITLES.put( careerKey, new String[] {
			"Cartographer", "Cartographer", "Mapmaker", "Mapmaker", "Explorer", "Explorer", "Mapvendor", "Mapvendor",
			"Topographer", "Mapper", "Adventurer", "Pathfinder", "Pioneer", "Mapmonger", "Wayfinder"
		} );
		NAME_POOLS.put( careerKey, Arrays.asList(
			"map", "dora", "venture", "carto", "topo", "way", "path", "find", "treasure"
		) );
		
		prof = "minecraft:priest";
		careerKey = getKey( prof, "cleric" );
		CAREER_TITLES.put( careerKey, new String[] {
			"Cleric", "Cleric", "Cleric", "Priest", "Priest", "Priest",
			"Father", "Mother", "Preacher", "Reverend", "Rabbi", "Chaplain", "Minister", "Churchman", "Healer", "Storyteller",
			"Mystic", "Enchanter", "Diviner", "Oracle", "Augur", "Prophet", "Seer", "Holyman", "Sage", "Wiseman", "Elder"
		} );
		NAME_POOLS.put( careerKey, Arrays.asList(
			"holy", "wise", "mystic", "secret", "cleric", "priest", "div"
		) );
		
		prof = "minecraft:smith";
		careerKey = getKey( prof, "armor" );
		CAREER_TITLES.put( careerKey, new String[] {
			"Armorsmith", "Armorsmith", "Armorer", "Armorer", "Armorworker", "Armorworker", "Armorvendor", "Armorvendor",
			"Armormaster", "Armorman", "Armorcrafter", "Plateworker", "Platesmith", "Platecrafter", "Shieldworker", "Shieldsmith", "Shieldcrafter",
			"Armormonger", "Platemonger", "Platevendor", "Shieldmonger", "Shieldvendor"
		} );
		NAME_POOLS.put( careerKey, Arrays.asList(
			"armor", "plate", "shield", "smith", "forge", "fire", "anvil"
		) );
		careerKey = getKey( prof, "weapon" );
		CAREER_TITLES.put( careerKey, new String[] {
			"Weaponsmith", "Weaponsmith", "Swordsmith", "Swordsmith", "Weaponmonger", "Weaponmonger",
			"Weaponcrafter", "Weaponmaker", "Weaponmaster", "Weaponman", "Swordcrafter", "Swordmaker", "Swordmaster",
			"Weaponvendor", "Swordmonger", "Swordvendor"
		} );
		NAME_POOLS.put( careerKey, Arrays.asList(
			"weapon", "sword", "poker", "smith", "forge", "fire", "anvil"
		) );
		careerKey = getKey( prof, "tool" );
		CAREER_TITLES.put( careerKey, new String[] {
			"Toolsmith", "Toolsmith", "Toolmaker", "Toolmaker", "Toolvendor", "Toolvendor",
			"Toolworker", "Toolcrafter", "Toolman", "Toolmaster", "Tooler", "Artisan", "Toolmonger"
		} );
		NAME_POOLS.put( careerKey, Arrays.asList(
			"tool", "pick", "axe", "shovel", "hoe", "hammer", "smith", "forge", "fire", "anvil"
		) );
		
		prof = "minecraft:butcher";
		careerKey = getKey( prof, "butcher" );
		CAREER_TITLES.put( careerKey, new String[] {
			"Butcher", "Butcher", "Meatman", "Meatman", "Meatmonger", "Meatmonger",
			"Meatvendor", "Cowslayer", "Sheepslayer", "Chickenslayer", "Rabbitslayer"
		} );
		NAME_POOLS.put( careerKey, Arrays.asList(
			"butcher", "blood", "meat", "chop", "cleave", "hook"
		) );
		careerKey = getKey( prof, "leather" );
		CAREER_TITLES.put( careerKey, new String[] {
			"Tanner", "Tanner", "Leathermaker", "Leathermaker", "Leathermonger", "Leathermonger",
			"Leatherman", "Skinner", "Cowskinner", "Sheepskinner", "Hidetanner", "Leathertanner", "Leathervendor"
		} );
		NAME_POOLS.put( careerKey, Arrays.asList(
			"tan", "leather", "hide", "skin", "strip", "chop", "cleave", "blood"
		) );
		
		prof = "minecraft:nitwit";
		careerKey = getKey( prof, "nitwit" );
		CAREER_TITLES.put( careerKey, new String[] {
			"Nitwit", "Blockhead", "Fool", "Dimwit", "Dork", "Dunce", "Jerk", "Moron", "Pinhead", "Ninny", "Simpleton"
		} );
		NAME_POOLS.put( careerKey, Arrays.asList(
			"fool", "dork", "jerk", "wit", "genius", "head", "smart", "wise"
		) );
	}
}
