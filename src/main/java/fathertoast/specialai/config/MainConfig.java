package fathertoast.specialai.config;

import fathertoast.specialai.config.field.BooleanField;
import fathertoast.specialai.config.field.DoubleField;
import fathertoast.specialai.config.field.EntityListField;
import fathertoast.specialai.config.util.EntityEntry;
import fathertoast.specialai.config.util.EntityList;
import net.minecraft.entity.EntityType;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * The main config file for this mod.
 */
public class MainConfig extends Config.AbstractConfig {
    
    public final General GENERAL;
    public final Reactions REACTIONS;
    
    MainConfig( ForgeConfigSpec.Builder builder ) {
        super( "main" );
        
        GENERAL = new General( builder, this );
        REACTIONS = new Reactions( builder, this );
    }
    
    public static class General extends Config.AbstractCategory {
        
        public final EntityListField depacifyList;
        public final DoubleField aggressiveChance;
        
        public final BooleanField eatBreedingItems;
        public final BooleanField eatingHeals;
        
        General( ForgeConfigSpec.Builder builder, Config.AbstractConfig parent ) {
            super( builder, parent, "general",
                    "Options that are either miscellaneous or apply to the mod as a whole." );
            
            //TODO Decide whether horses should be on this list, and verify each of these work
            depacifyList = entry( builder, "depacify_list", new EntityList(
                            new EntityEntry( EntityType.CHICKEN, 1.0 ), new EntityEntry( EntityType.COW, 1.0 ),
                            new EntityEntry( EntityType.PIG, 1.0 ), new EntityEntry( EntityType.SHEEP, 1.0 ),
                            new EntityEntry( EntityType.RABBIT, 1.0 ), new EntityEntry( EntityType.SQUID, 1.0 ),
                            new EntityEntry( EntityType.STRIDER, 1.0 ), new EntityEntry( EntityType.TURTLE, 1.0 )
                    ).setSinglePercent(),
                    "List of passive mobs (by entity id) that are made \"neutral\" like wolves.",
                    "You may put a tilde (~) in front of any entity id to make it specific; specific entity ids",
                    "will not count any entities extending (i.e., based on) the specified entity.",
                    "Additional number after the entity id is the chance (0.0 to 1.0) for entities of that type to spawn with the AI.",
                    "May or may not work on mod mobs." );
            aggressiveChance = entry0to1( builder, "aggressive_chance", 0.01,
                    "Chance for an entity on the depacify list to spawn aggressive, instead of just neutral." );
            
            lineBreak();
            
            eatBreedingItems = entry( builder, "eat_breeding_items", true,
                    "If true, passive mobs will seek out and eat the items used to breed them laying on the floor." );
            eatingHeals = entry( builder, "eating_heals", true,
                    "If true, when mobs eat breeding items off the floor, they will regain health (like wolves).",
                    "The option \"eat_breeding_items\" needs to be enabled for this to have any effect." );
        }
    }
    
    public static class Reactions extends Config.AbstractCategory {
        
        public final BooleanField avoidExplosions;
        
        public final BooleanField callForHelp;
        public final DoubleField callForHelpOnDeath;
        
        public final DoubleField dodgeArrowChance;
        //TODO dodge arrows list with chances
        
        Reactions( ForgeConfigSpec.Builder builder, Config.AbstractConfig parent ) {
            super( builder, parent, "reaction_ai",
                    "Options to customize reactive behaviors." );
            
            avoidExplosions = entry( builder, "avoid_explosions", true,
                    "If true, all mobs will try to avoid TNT and creepers that are about to explode." );
            
            lineBreak();
            
            callForHelp = entry( builder, "call_for_help", true,
                    "If true, all mobs will call for help from nearby mobs of the same type when struck.",
                    "This does not trigger from killing blows (see below)." );
            callForHelpOnDeath = entry0to1( builder, "call_for_help_on_death", 0.2,
                    "Chance for mobs to call for help when dealt a killing blow." );
            
            lineBreak();
            
            dodgeArrowChance = entry0to1( builder, "dodge_arrow_chance", 0.4,
                    "The chance any mob will try to sidestep an arrow fired in their direction." );
        }
    }
}