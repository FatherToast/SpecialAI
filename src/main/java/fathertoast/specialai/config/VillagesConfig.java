package fathertoast.specialai.config;

import fathertoast.crust.api.config.common.AbstractConfigCategory;
import fathertoast.crust.api.config.common.AbstractConfigFile;
import fathertoast.crust.api.config.common.ConfigManager;
import fathertoast.crust.api.config.common.field.*;
import fathertoast.crust.api.config.common.field.collection.EntityMapField;
import fathertoast.crust.api.config.common.value.DefaultValueEntry;
import fathertoast.crust.api.config.common.value.RegistryEntryValueList;
import fathertoast.crust.api.config.common.value.RegistryValueEntry;
import fathertoast.crust.api.config.common.value.RegistryValueTagEntry;
import fathertoast.crust.api.config.common.value.collection.EntityMap;
import fathertoast.crust.api.config.common.value.collection.value.IntValueCodec;
import fathertoast.specialai.config.field.IntListField;
import fathertoast.specialai.config.field.ProfessionNameListField;
import fathertoast.specialai.util.VillagerNameHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Objects;

public class VillagesConfig extends AbstractConfigFile {
    
    public final BehaviorTweaks AI_TWEAKS;
    public final Reputation REPUTATION;
    public final VillagerNames VILLAGER_NAMES;
    
    
    /** Builds the config spec that should be used for this config. */
    VillagesConfig( ConfigManager cfgManager, String cfgName ) {
        super( cfgManager, cfgName,
                "This config contains various options to control village tweaks, villager behavior, and reputation." );
        
        SPEC.fileOnlyNewLine();
        SPEC.describeBlockList();
        SPEC.fileOnlyNewLine();
        SPEC.describeRegistryEntryValueList();
        SPEC.fileOnlyNewLine();
        SPEC.describeEntityList();
        SPEC.fileOnlyNewLine();
        
        AI_TWEAKS = new BehaviorTweaks( this );
        REPUTATION = new Reputation( this );
        VILLAGER_NAMES = new VillagerNames( this );
    }
    
    
    public static class BehaviorTweaks extends AbstractConfigCategory<VillagesConfig> {
        
        public final IntField refuseTradeRep;
        //public final IntField attackHooliganRep;
        
        public BehaviorTweaks( VillagesConfig parent ) {
            super( parent, "behavior_tweaks",
                    "Options for misc. AI tweaks to villagers." );
            
            refuseTradeRep = SPEC.define( new IntField( "refuse_trade_rep", -100, IntField.Range.ANY,
                    "If the player's reputation with a villager is equal to or lower than this value, " +
                            "the villager will refuse to trade." ) );
            
            // TODO - change description when attack AI works
            /*
            attackHooliganRep = SPEC.define( new IntField( "attack_hooligan_rep", -150, IntField.Range.ANY,
                    "If the player's reputation with a villager is equal to or lower than this value, " +
                            "the villager will attack the player on sight." ) );

             */
        }
    }
    
    
    public static class Reputation extends AbstractConfigCategory<VillagesConfig> {
        
        public final RegistryEntryValueListField<Block> repChangingBlocks;
        public final IntField breakBlockThreshold;
        
        public final EntityMapField<Integer> repChangingCreatures;
        public final IntField killCreatureThreshold;
        
        public final IntField trampleFarmlandAnger;
        
        Reputation( VillagesConfig parent ) {
            super( parent, "reputation",
                    "Options for reputation gain and reputation loss." );
            
            repChangingBlocks = SPEC.define( new RegistryEntryValueListField<>( "reputation_changing_blocks", createDefaultRepBlocks(),
                    "A list of blocks that may either anger or please nearby villagers inside villages when destroyed by a player.",
                    "The numeric value in each entry determines the reputation change.",
                    "Negative values mean bad reputation, positive means good reputation.",
                    "Note that breaking blocks will only affect 'minor negative' or 'minor positive' reputation, " +
                            "which both have an upper limit of 200." ) );
            
            breakBlockThreshold = SPEC.define( new IntField( "break_block_threshold", 100, IntField.Range.ANY,
                    "If a player's reputation is greater than or equal to this value with a villager, the observing villager will not " +
                            "be bothered if the player breaks a block in the village that would otherwise give bad reputation." ) );
            
            SPEC.newLine();
            
            repChangingCreatures = SPEC.define( new EntityMapField<>( "reputation_changing_creatures", createDefaultRepCreatures(),
                    "A list of entities that may either anger or please nearby villagers inside villages when killed by a player.",
                    "The numeric value in each entry determines the reputation change.",
                    "Negative values mean bad reputation, positive means good reputation.",
                    "Killing villagers already gives major bad reputation in vanilla, and cannot be overridden here.",
                    "Note that killing creatures will only affect 'minor negative' or 'minor positive' reputation, " +
                            "which both have an upper limit of 200." ) );
            
            killCreatureThreshold = SPEC.define( new IntField( "kill_creature_threshold", 190, IntField.Range.ANY,
                    "If a player's reputation is greater than or equal to this value with a villager, the observing villager will not " +
                            "be bothered if the player kills a creature in the village that would otherwise give bad reputation." ) );
            
            SPEC.newLine();
            
            trampleFarmlandAnger = SPEC.define( new IntField( "trample_farmland_anger", -4, -200, 0,
                    "If greater than 0, players will anger nearby farmer villagers if they trample farmland, losing the specified amount of reputation.",
                    "Note that farmer villagers take trampling personally, and will be displeased even if it happens outside a village." ) );
        }
        
        private RegistryEntryValueList<Block> createDefaultRepBlocks() {
            return new RegistryEntryValueList<>( new DefaultValueEntry( -1 ), () -> ForgeRegistries.BLOCKS,
                    new RegistryValueEntry<>( id( Blocks.HAY_BLOCK ), -2 ), new RegistryValueEntry<>( id( Blocks.CHEST ), -5 ),
                    new RegistryValueEntry<>( id( Blocks.FURNACE ), -4 ), new RegistryValueEntry<>( id( Blocks.BARREL ), -4 ),
                    new RegistryValueEntry<>( id( Blocks.TALL_GRASS ), 0 ), new RegistryValueEntry<>( id( Blocks.GRASS ), 0 ),
                    new RegistryValueEntry<>( id( Blocks.FERN ), 0 )
            
            ).addTagEntries( List.of(
                    new RegistryValueTagEntry<>( BlockTags.DOORS, -3 ),
                    new RegistryValueTagEntry<>( BlockTags.FLOWERS, 0 )
            ) );
        }
        
        @SuppressWarnings( "UnstableApiUsage" )
        private EntityMap<Integer> createDefaultRepCreatures() {
            return new EntityMap.Builder<>( IntValueCodec.ANY )
                    .put( EntityType.CAT, -20 ).put( EntityType.COW, -5 )
                    .put( EntityType.SHEEP, -5 ).put( EntityType.PIG, -5 )
                    .put( EntityType.CHICKEN, -3 ).put( EntityType.HORSE, -20 )
                    .put( EntityType.IRON_GOLEM, -50 ).put( EntityType.WANDERING_TRADER, -40 )
                    .put( EntityType.VEX, 1 )
                    .putExtends( EntityType.ZOMBIE, 4 ).putExtends( EntityType.SKELETON, 3 )
                    .putExtends( EntityType.CREEPER, 5 ).putExtends( EntityType.ENDERMAN, 2 )
                    .putExtends( EntityType.SPIDER, 2 ).putExtends( EntityType.SLIME, 1 )
                    .putTag( EntityTypeTags.RAIDERS, 15 )
                    .build();
        }
        
        private static ResourceLocation id( Block block ) {
            return Objects.requireNonNull( ForgeRegistries.BLOCKS.getKey( block ) );
        }
    }
    
    
    public static class VillagerNames extends AbstractConfigCategory<VillagesConfig> {
        
        public final BooleanField masterToggle;
        
        public final StringListField baseNameComponents;
        public final IntListField componentRolls;
        
        public final StringListField namePrefixes;
        public final DoubleField namePrefixChance;
        
        public final ProfessionNameListField villagerJobTitles;
        
        public VillagerNames( VillagesConfig parent ) {
            super( parent, "villager_names",
                    "Name and job title components for randomly generated villager names." );
            
            masterToggle = SPEC.define( new BooleanField( "master_toggle", true,
                    "If false, none of the below options will do anything and SpecialAI will not generate random "
                            + "names for villagers." ) );
            
            SPEC.newLine();
            
            baseNameComponents = SPEC.define( new StringListField( "base_name_components", createDefaultNameParts(),
                    "A list of name components to pick from when generating a random name for a villager." ) );
            
            componentRolls = SPEC.define( new IntListField( "component_rolls", List.of( "1", "1", "4", "16", "64" ), IntField.Range.POSITIVE,
                    "A list of rolls to determine how many name components can be picked when generating a villager name.",
                    "For example, if the list contains 5 numbers, a villager name may generate with 1-5 total components.",
                    "The value of each number determines the chance to add a component, so 1 would equal a 1/1 chance, and 64 would equal a 1/64 chance." ) );
            
            SPEC.newLine();
            
            namePrefixes = SPEC.define( new StringListField( "name_prefixes", createDefaultNamePrefixes(),
                    "A list of prefixes that may be added to a villager's first name." ) );
            
            namePrefixChance = SPEC.define( new DoubleField( "name_prefix_chance", 0.01, DoubleField.Range.PERCENT,
                    "The chance for a prefix from the above list to be added to the name of a villager." ) );
            
            SPEC.newLine();
            
            villagerJobTitles = SPEC.define( new ProfessionNameListField( "villager_job_titles", VillagerNameHelper.getDefaultJobTitles(),
                    "A list of villager professions and all job titles associated with them.",
                    "Existing titles can be modified here, and new ones can be added for professions from other mods.",
                    "When a villager picks up a new profession, a random job title for that profession is picked from this list.",
                    "Adding the same title multiple times essentially just gets it picked more commonly." ) );
        }
        
        private List<String> createDefaultNameParts() {
            return List.of(
                    "grab", "thar", "ger", "ald", "mas", "on", "o", "din", "thor", "jon", "ath", "an", "burb", "en",
                    "a", "e", "i", "u", "hab", "bloo", "ena", "dit", "aph", "ern", "bor", "dav", "id", "toast", "son", "dottir",
                    "for", "wen", "lob", "ed", "die", "van", "y", "zap", "ear", "ben", "don", "bran", "gro", "jen", "bob",
                    "ette", "ere", "man", "qua", "bro", "cree", "per", "skel", "ton", "zom", "bie", "wolf", "end", "er", "pig",
                    "sil", "ver", "fish", "cow", "chic", "ken", "sheep", "lla", "rab", "bit", "squid", "hell", "scrub", "loaf",
                    "bonk", "clonk", "bink", "guy", "gal", "wool", "flo", "fee", "fi", "fo", "fum", "green", "blue", "red",
                    "seed", "wheat", "boat", "rod", "poke", "bow" );
        }
        
        private List<String> createDefaultNamePrefixes() {
            return List.of(
                    "Mc", "Mac"
            );
        }
    }
}