package fathertoast.specialai.util;

import fathertoast.crust.api.config.common.value.collection.RegistryMap;
import fathertoast.crust.api.config.common.value.collection.value.ArrayValueCodec;
import fathertoast.crust.api.config.common.value.collection.value.StringValueCodec;
import fathertoast.crust.api.lib.NBTHelper;
import fathertoast.specialai.core.GameEventHandler;
import fathertoast.specialai.core.SpecialAI;
import fathertoast.specialai.config.VillagesConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

import static fathertoast.specialai.config.Config.VILLAGES;

/**
 * This class handles random name generation for villagers, influenced by the villager's profession.
 */
@SuppressWarnings( { "SpellCheckingInspection", "UnstableApiUsage" } )
public final class VillagerNameHelper {
    /** Villager first name tag key. */
    private static final String TAG_FIRST_NAME = "FirstName";
    /** Villager full name tag key. In other words, both full name and job title. */
    private static final String TAG_FULL_NAME = "FullName";
    /** Mod data tag key. */
    private static final String TAG_MOD_DATA = SpecialAI.MOD_ID + "Data";
    
    /**
     * Generates a new, random name for the given villager and sets it as the entity's display name.<br>
     * If a first name already exists in the villager's NBT, we only generate a new job title and merge
     * it with the existing name, making sure the original first name never
     * changes even if a new profession is picked.
     *
     * @param random   The RandomSource fetched from the Villager entity.
     * @param villager The Villager to generate a name for.
     * @param data     The new VillagerData to be set. The old one can still be accessed.
     */
    public static void setVillagerName( RandomSource random, Villager villager, VillagerData data ) {
        // Randomly generated names are disabled
        if( !VILLAGES.VILLAGER_NAMES.masterToggle.get() ) return;
        
        try {
            // No components available, abort
            
            if( VILLAGES.VILLAGER_NAMES.componentList.isEmpty() )
                return;
            
            final CompoundTag modData = NBTHelper.getOrCreateCompound( NBTHelper.getForgeData( villager ), TAG_MOD_DATA );
            
            // Prevent generating new names or job titles for villagers that
            // haven't had their NBT loaded yet, unless it is the first time we name them.
            if( !villager.isAddedToWorld() && NBTHelper.containsString( modData, TAG_FIRST_NAME ) )
                return;
            
            // Begin building the name
            final StringBuilder nameBuilder = new StringBuilder( 32 );
            
            // No existing name in NBT, generate a fresh one
            if( !NBTHelper.containsString( modData, TAG_FIRST_NAME ) ) {
                final List<String> namePool = VILLAGES.VILLAGER_NAMES.componentList.get();
                
                // Build the main name out of a random number of parts and then capitalize it.
                // If the rolls list is empty, apply at least one guaranteed component.
                if( VILLAGES.VILLAGER_NAMES.componentRolls.isEmpty() ) {
                    nameBuilder.append( namePool.get( random.nextInt( namePool.size() ) ) );
                }
                else {
                    for( int chance : VILLAGES.VILLAGER_NAMES.componentRolls.entries() ) {
                        if( random.nextInt( chance ) == 0 ) {
                            nameBuilder.append( namePool.get( random.nextInt( namePool.size() ) ) );
                        }
                    }
                }
                nameBuilder.setCharAt( 0, Character.toUpperCase( nameBuilder.charAt( 0 ) ) );
                
                // Mc/Mac names
                if( !VILLAGES.VILLAGER_NAMES.namePrefixes.isEmpty() && VILLAGES.VILLAGER_NAMES.namePrefixChance.rollChance( random ) ) {
                    String prefix = VILLAGES.VILLAGER_NAMES.namePrefixes.get().get( random.nextInt( VILLAGES.VILLAGER_NAMES.namePrefixes.get().size() ) );
                    nameBuilder.insert( 0, prefix );
                }
                // Save this to NBT for later
                String firstName = nameBuilder.toString();
                modData.putString( TAG_FIRST_NAME, firstName );
            }
            else {
                // Append the existing name from NBT
                nameBuilder.append( modData.getString( TAG_FIRST_NAME ) );
            }
            final VillagerProfession prof = data.getProfession();
            final String profKey = getKey( prof );
            
            // Set name early if job title config is empty or if the profession is NONE.
            if( prof == VillagerProfession.NONE || VILLAGES.VILLAGER_NAMES.villagerJobTitles.isEmpty() ) {
                villager.setCustomName( Component.literal( nameBuilder.toString() ) );
                return;
            }
            final boolean sameProfession = data.getProfession() == villager.getVillagerData().getProfession();
            final boolean fullNameExists = modData.contains( TAG_FULL_NAME, Tag.TAG_STRING );
            
            if( !VILLAGES.VILLAGER_NAMES.villagerJobTitles.contains( prof ) ) {
                SpecialAI.LOG.warn( "Naming villager with unknown profession '{}'", profKey );
            }
            // Generate new job title unless the "new" profession is identical to the existing one,
            // or if a full name doesn't already exist in NBT. Also skip title if 'changeTitle' if false.
            else if( !sameProfession || !fullNameExists ) {
                final String[] availableTitles = VILLAGES.VILLAGER_NAMES.villagerJobTitles.get( prof );
                // noinspection ConstantConditions
                final String title = availableTitles[random.nextInt( availableTitles.length )];
                
                if( random.nextInt( 3 ) == 0 ) {
                    nameBuilder.append( " the " ).append( title );
                }
                else {
                    nameBuilder.insert( 0, " " ).insert( 0, title );
                }
                // Save full name to NBT and apply
                modData.putString( TAG_FULL_NAME, nameBuilder.toString() );
                villager.setCustomName( Component.literal( nameBuilder.toString() ) );
            }
            else {
                // Full name already exists and the villager's profession didn't change, apply old name
                villager.setCustomName( Component.literal( modData.getString( TAG_FULL_NAME ) ) );
            }
        }
        catch( Exception e ) {
            SpecialAI.LOG.warn( "Failed to generate and set randomly generated name for Villager!" );
            // noinspection CallToPrintStackTrace
            e.printStackTrace();
        }
    }
    
    /**
     * Called from {@link GameEventHandler#onRightClickEntity(PlayerInteractEvent.EntityInteract)} when
     * a player right-clicks a villager with a name tag.<br><br>
     * Sets the target villager's first name to the display name of the name tag
     * and then refreshes the villager's full name.
     */
    public static void handleNameTagUse( PlayerInteractEvent.EntityInteract event, Villager villager, ItemStack nameTag ) {
        if( event.getEntity().level().isClientSide || !VILLAGES.VILLAGER_NAMES.masterToggle.get() ) return;
        
        // Name tag is "empty", abort
        if( !nameTag.hasCustomHoverName() ) return;
        
        final String name = nameTag.getHoverName().getString();
        final String existingName = villager.getCustomName() != null ? villager.getCustomName().getString() : null;
        final CompoundTag modData = NBTHelper.getOrCreateCompound( NBTHelper.getForgeData( villager ), TAG_MOD_DATA );
        
        if( existingName != null && NBTHelper.containsString( modData, TAG_FIRST_NAME ) ) {
            String firstName = modData.getString( TAG_FIRST_NAME );
            villager.setCustomName( Component.literal( existingName.replace( firstName, name ) ) );
        }
        else {
            villager.setCustomName( Component.literal( name ) );
        }
        // Save to NBT so we can use the first name later
        // if the villager's profession changes.
        modData.putString( TAG_FIRST_NAME, name );
        
        // Cancel event, set the right result and shrink the used itemstack.
        event.setCanceled( true );
        event.setCancellationResult( InteractionResult.CONSUME );
        
        if( !event.getEntity().isCreative() ) {
            event.getItemStack().shrink( 1 );
        }
    }
    
    /**
     * @return The registry name of the given profession as a String,
     * if it exists in the Forge registry. Throws an exception otherwise.
     */
    private static String getKey( VillagerProfession profession ) {
        return SpecialAI.toString( ForgeRegistries.VILLAGER_PROFESSIONS.getKey( profession ) );
    }
    
    /**
     * Constructs the default config map of first name and profession/career names for
     * all vanilla villager professions.
     * <br><br>
     * Called from {@link fathertoast.specialai.config.VillagesConfig.VillagerNames#VillagerNames(VillagesConfig)}.
     */
    public static RegistryMap<VillagerProfession, String[]> createDefaultJobTitles() {
        final RegistryMap.Builder<VillagerProfession, String[], ?> builder = new RegistryMap.Builder<>(
                ForgeRegistries.VILLAGER_PROFESSIONS,
                // Max length of entity display names are 256, as limited by NBT
                ArrayValueCodec.of( 0, String.class, StringValueCodec.of( 256 ) )
        );
        builder.put( VillagerProfession.FARMER, new String[] {
                "Farmer", "Farmer", "Gardener", "Gardener", "Seedmonger", "Seedmonger",
                "Cropper", "Greenskeeper", "Agriculturalist", "Horticulturist", "Seedsman", "Landscaper", "Seedvendor"
        } );
        builder.put( VillagerProfession.FISHERMAN, new String[] {
                "Fisherman", "Fisherman", "Fisher", "Fisher", "Angler", "Angler", "Fishmonger", "Fishmonger",
                "Piscator", "Rodman", "Troller", "Trawler", "Fishman", "Skipper", "Captain", "Fishvendor"
        } );
        builder.put( VillagerProfession.SHEPHERD, new String[] {
                "Shepherd", "Shepherd", "Rancher", "Rancher", "Herder", "Herder",
                "Cowhand", "Herdsman", "Cowpuncher", "Sheepsman", "Sheeper", "Cooper", "Stockman"
        } );
        builder.put( VillagerProfession.FLETCHER, new String[] {
                "Fletcher", "Fletcher", "Bowyer", "Bowyer", "Arrowvendor", "Arrowvendor",
                "Arrowman", "Bowman", "Arrowcrafter", "Bowcrafter", "Bowvendor", "Arrowmonger", "Bowmonger"
        } );
        builder.put( VillagerProfession.LIBRARIAN, new String[] {
                "Librarian", "Librarian", "Curator", "Curator", "Bookbinder", "Bookbinder", "Bookvendor", "Bookvendor",
                "Cataloger", "Researcher", "Bookman", "Bibliothecary", "Recordkeeper", "Bookmonger"
        } );
        builder.put( VillagerProfession.CARTOGRAPHER, new String[] {
                "Cartographer", "Cartographer", "Mapmaker", "Mapmaker", "Explorer", "Explorer", "Mapvendor", "Mapvendor",
                "Topographer", "Mapper", "Adventurer", "Pathfinder", "Pioneer", "Mapmonger", "Wayfinder"
        } );
        builder.put( VillagerProfession.CLERIC, new String[] {
                "Cleric", "Cleric", "Cleric", "Priest", "Priest", "Priest",
                "Father", "Mother", "Preacher", "Reverend", "Rabbi", "Chaplain", "Minister", "Churchman", "Healer", "Storyteller",
                "Mystic", "Enchanter", "Diviner", "Oracle", "Augur", "Prophet", "Seer", "Holyman", "Sage", "Wiseman", "Elder"
        } );
        builder.put( VillagerProfession.ARMORER, new String[] {
                "Armorsmith", "Armorsmith", "Armorer", "Armorer", "Armorworker", "Armorworker", "Armorvendor", "Armorvendor",
                "Armormaster", "Armorman", "Armorcrafter", "Plateworker", "Platesmith", "Platecrafter", "Shieldworker", "Shieldsmith", "Shieldcrafter",
                "Armormonger", "Platemonger", "Platevendor", "Shieldmonger", "Shieldvendor"
        } );
        builder.put( VillagerProfession.WEAPONSMITH, new String[] {
                "Weaponsmith", "Weaponsmith", "Swordsmith", "Swordsmith", "Weaponmonger", "Weaponmonger",
                "Weaponcrafter", "Weaponmaker", "Weaponmaster", "Weaponman", "Swordcrafter", "Swordmaker", "Swordmaster",
                "Weaponvendor", "Swordmonger", "Swordvendor"
        } );
        builder.put( VillagerProfession.TOOLSMITH, new String[] {
                "Toolsmith", "Toolsmith", "Toolmaker", "Toolmaker", "Toolvendor", "Toolvendor",
                "Toolworker", "Toolcrafter", "Toolman", "Toolmaster", "Tooler", "Artisan", "Toolmonger"
        } );
        builder.put( VillagerProfession.BUTCHER, new String[] {
                "Butcher", "Butcher", "Meatman", "Meatman", "Meatmonger", "Meatmonger",
                "Meatvendor", "Cowslayer", "Sheepslayer", "Chickenslayer", "Rabbitslayer"
        } );
        builder.put( VillagerProfession.LEATHERWORKER, new String[] {
                "Tanner", "Tanner", "Leathermaker", "Leathermaker", "Leathermonger", "Leathermonger",
                "Leatherman", "Skinner", "Cowskinner", "Sheepskinner", "Hidetanner", "Leathertanner", "Leathervendor"
        } );
        builder.put( VillagerProfession.NITWIT, new String[] {
                "Nitwit", "Blockhead", "Fool", "Dimwit", "Dork", "Dunce", "Jerk", "Moron", "Pinhead", "Ninny", "Simpleton",
                "Philosopher", "Daydreamer", "Roamer", "Nincompoop"
        } );
        
        return builder.build();
    }
    
    // This is a static-only helper class.
    private VillagerNameHelper() {}
}