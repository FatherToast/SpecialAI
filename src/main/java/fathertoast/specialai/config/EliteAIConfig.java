package fathertoast.specialai.config;

import fathertoast.specialai.ModCore;
import fathertoast.specialai.ai.elite.EliteAIType;
import fathertoast.specialai.ai.elite.ThiefEliteGoal;
import fathertoast.specialai.config.field.*;
import fathertoast.specialai.config.file.ToastConfigSpec;
import fathertoast.specialai.config.file.TomlHelper;
import fathertoast.specialai.config.util.*;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EntityType;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class EliteAIConfig extends Config.AbstractConfig {
    
    public final EliteGeneral GENERAL;
    
    public final Leap LEAP;
    public final Jump JUMP;
    public final Sprint SPRINT;
    public final Barrage BARRAGE;
    public final Charge CHARGE;
    public final Thief THIEF;
    public final Shaman SHAMAN;
    public final Spawner SPAWNER;
    public final ThrowAlly THROW_ALLY;
    public final ThrowEnemy THROW_ENEMY;
    
    /** Builds the config spec that should be used for this config. */
    EliteAIConfig( File dir, String fileName ) {
        super( dir, fileName,
                "This config contains options for elite AI patterns. Elite AI patterns bestow stat boosts, specific",
                "equipment, and allow these 'elite' mobs to perform actions in combat that can pose a great threat.",
                "See the appendix at the bottom of this file for more information on the elite AI patterns available.",
                "In general; changing options for elite AIs granted, attributes, and equipment only affects new entities.",
                "All other option changes affect existing entities (even in a running client) unless otherwise stated."
        );
        
        SPEC.newLine();
        SPEC.describeEntityList();
        
        GENERAL = new EliteGeneral( SPEC );
        LEAP = new Leap( SPEC );
        JUMP = new Jump( SPEC );
        SPRINT = new Sprint( SPEC );
        BARRAGE = new Barrage( SPEC );
        CHARGE = new Charge( SPEC );
        THIEF = new Thief( SPEC );
        SHAMAN = new Shaman( SPEC );
        SPAWNER = new Spawner( SPEC );
        THROW_ALLY = new ThrowAlly( SPEC );
        THROW_ENEMY = new ThrowEnemy( SPEC );
        
        // Print description for each elite AI pattern
        SPEC.decreaseIndent();
        SPEC.newLine( 2 );
        SPEC.comment( "Appendix:",
                "Below is a description for each elite AI pattern.",
                "Note that the qualitative descriptions (high, low, etc.) are based on default values and therefore may not be valid",
                "in an edited config. All equipment and attribute modifiers can be disabled and all attribute modifiers can be changed.",
                "Other values that can be changed by config are noted with an asterisk (*)." );
        SPEC.increaseIndent();
        for( EliteAIType ai : EliteAIType.values() ) {
            SPEC.newLine();
            SPEC.comment( String.format( "%s:", ai.getKey() ) );
            SPEC.increaseIndent();
            List<String> description = new ArrayList<>();
            ai.describe( description );
            SPEC.comment( description );
            SPEC.decreaseIndent();
        }
    }
    
    public static class EliteGeneral extends Config.AbstractCategory {
        
        public final EntityListField.Combined entityList;
        
        public final WeightedList<EliteAIType> eliteAIWeights;
        
        public final BooleanField enablePreferMelee;
        public final BooleanField enableAttributeMods;
        public final BooleanField enableEquipmentReplace;
        
        EliteGeneral( ToastConfigSpec parent ) {
            super( parent, "general",
                    "Options for customizing the elite AI system and options that affect all elite AI patterns." );
            
            entityList = new EntityListField.Combined(
                    SPEC.define( new EntityListField( "entities.whitelist", new EntityList(
                            new EntityEntry( EntityType.ZOMBIE, 0.04 ),
                            // Skeletons
                            new EntityEntry( EntityType.SKELETON, 0.1, 0.02 ), new EntityEntry( EntityType.STRAY, 0.1, 0.02 ),
                            new EntityEntry( EntityType.WITHER_SKELETON, 0.1, 0.02 ),
                            // Nether
                            new EntityEntry( EntityType.PIGLIN, 0.04, 0.04, 0.02 ), new EntityEntry( EntityType.ZOMBIFIED_PIGLIN, 0.04, 0.04, 0.02 ),
                            new EntityEntry( EntityType.PIGLIN_BRUTE, 0.5, 0.01, 0.01 )
                    ).setRange0to1(),
                            "List of mobs that can gain elite AI patterns and their chances to gain those patterns.",
                            "Additional values after the entity type are the chances (0.0 to 1.0) for entities of that type to spawn with elite AI.",
                            "You can specify multiple chances for each entity - each chance will be rolled and multiple AIs can stack." ) ),
                    SPEC.define( new EntityListField( "entities.blacklist", new EntityList().setNoValues() ) )
            );
            
            SPEC.newLine();
            
            eliteAIWeights = new WeightedList<>( SPEC, "weight", EliteAIType.values(),
                    "The following options are the weights for each elite AI pattern to be chosen when assigning an elite AI",
                    "to entities in the above list. The higher an AI's weight, the more common it will be compared to the others.",
                    "Elite AIs given a weight of 0 are effectively disabled (though they can still be NBT-edited onto mobs)." );
            
            SPEC.newLine();
            
            enablePreferMelee = SPEC.define( new BooleanField( "prefer_melee_enabled", true,
                    "When true, elite AI patterns that \"prefer melee\" will replace any shooting item held by the entity",
                    "with a golden sword, in the hopes they will use melee attacks (instead of bow/crossbow attacks).",
                    "Note that this can be enabled or disabled for each AI pattern individually (see categories below)." ) );
            enableAttributeMods = SPEC.define( new BooleanField( "attribute_modifiers_enabled", true,
                    "When true, elite AI patterns will be allowed to apply attribute modifiers to entities.",
                    "Note that each modifier can be disabled or changed individually (see categories below)." ) );
            enableEquipmentReplace = SPEC.define( new BooleanField( "equipment_replacement_enabled", true,
                    "When true, elite AI patterns will be allowed to overwrite existing entity equipment (aside from melee preference).",
                    "This equipment is designed to visually distinguish the elite AI pattern(s) on entities, so disabling",
                    "this may make it more difficult for players to understand what they are fighting.",
                    "Note that each equipment item can be disabled individually and some can be modified (see categories below)." ) );
        }
    }
    
    public static class Leap extends EliteAICategory {
        
        public final SqrDoubleField rangeSqrMin;
        public final SqrDoubleField rangeSqrMax;
        
        public final IntField.RandomRange cooldown;
        
        public final SpeedField jumpSpeedForward;
        public final SpeedField jumpSpeedUpward;
        
        Leap( ToastConfigSpec parent ) {
            super( parent, EliteAIType.LEAP, true, new AttributeMods().knockback( 1.0 ).speed( 0.1 ) );
            
            rangeSqrMin = SPEC.define( new SqrDoubleField( "range.min", 2.0, DoubleField.Range.POSITIVE,
                    "The minimum and maximum distance the entity must be from its target for this AI to activate, in blocks (meters)." ) );
            rangeSqrMax = SPEC.define( new SqrDoubleField( "range.max", 6.0, DoubleField.Range.POSITIVE ) );
            
            SPEC.newLine();
            
            cooldown = new IntField.RandomRange(
                    SPEC.define( new IntField( "cooldown.min", 20, IntField.Range.NON_NEGATIVE,
                            "The minimum and maximum (inclusive) time this AI is disabled for after",
                            "completion, in ticks (20 ticks = 1 second)." ) ),
                    SPEC.define( new IntField( "cooldown.max", 40, IntField.Range.NON_NEGATIVE ) )
            );
            
            SPEC.newLine();
            
            jumpSpeedForward = SPEC.define( new SpeedField( "jump_speed.forward", 16.0,
                    "The horizontal speed given to the entity in its target's direction when this AI activates, in blocks per second (m/s)." ) );
            jumpSpeedUpward = SPEC.define( new SpeedField( "jump_speed.upward", 8.0,
                    "The vertical speed given to the entity when this AI activates, in blocks per second (m/s).",
                    "For reference, normal jumping speed is 8.4 m/s." ) );
        }
    }
    
    public static class Jump extends EliteAICategory {
        
        public final SqrDoubleField rangeSqrMin;
        public final SqrDoubleField rangeSqrMax;
        
        public final IntField.RandomRange cooldown;
        
        public final SpeedField jumpSpeedForward;
        public final SpeedField jumpSpeedUpward;
        
        public final DoubleField featherBootsDropChance;
        public final IntField featherBootsEnchant;
        
        Jump( ToastConfigSpec parent ) {
            super( parent, EliteAIType.JUMP, false );
            
            rangeSqrMin = SPEC.define( new SqrDoubleField( "range.min", 6.0, DoubleField.Range.POSITIVE,
                    "The minimum and maximum distance the entity must be from its target for this AI to activate, in blocks (meters)." ) );
            rangeSqrMax = SPEC.define( new SqrDoubleField( "range.max", 12.0, DoubleField.Range.POSITIVE ) );
            
            SPEC.newLine();
            
            cooldown = new IntField.RandomRange(
                    SPEC.define( new IntField( "cooldown.min", 60, IntField.Range.NON_NEGATIVE,
                            "The minimum and maximum (inclusive) time this AI is disabled for after",
                            "completion, in ticks (20 ticks = 1 second)." ) ),
                    SPEC.define( new IntField( "cooldown.max", 100, IntField.Range.NON_NEGATIVE ) )
            );
            
            SPEC.newLine();
            
            jumpSpeedForward = SPEC.define( new SpeedField( "jump_speed.forward", 28.0,
                    "The horizontal speed given to the entity in its target's direction when this AI activates, in blocks per second (m/s)." ) );
            jumpSpeedUpward = SPEC.define( new SpeedField( "jump_speed.upward", 20.0,
                    "The vertical speed given to the entity when this AI activates, in blocks per second (m/s).",
                    "For reference, normal jumping speed is 8.4 m/s." ) );
            
            SPEC.newLine();
            
            featherBootsDropChance = SPEC.define( new DoubleField( "feather_boots.drop_chance", 0.085, DoubleField.Range.DROP_CHANCE,
                    "The drop chance for Feather Boots (0 to 1). The item will drop damaged randomly, like normal equipment.",
                    "A negative value disables the item entirely, while a value over 1 prevents the item from being damaged on drop." ) );
            featherBootsEnchant = SPEC.define( new IntField( "feather_boots.enchant", Enchantments.FALL_PROTECTION.getMaxLevel(), IntField.Range.NON_NEGATIVE,
                    "Level of the feather falling enchantment on Feather Boots. 0 disables the enchant." ) );
        }
    }
    
    public static class Sprint extends EliteAICategory {
        
        public final SqrDoubleField rangeSqrMin;
        
        public final DoubleField runSpeed;
        
        public final SqrDoubleField endRangeSqrMin;
        public final SqrDoubleField endRangeSqrMax;
        
        public final DoubleField runningBootsDropChance;
        public final DoubleField runningBootsModifier;
        
        Sprint( ToastConfigSpec parent ) {
            super( parent, EliteAIType.SPRINT, false );
            
            rangeSqrMin = SPEC.define( new SqrDoubleField( "range.min", 12.0, DoubleField.Range.POSITIVE,
                    "The minimum distance the entity must be from its target for this AI to activate, in blocks (meters)." ) );
            
            SPEC.newLine();
            
            runSpeed = SPEC.define( new DoubleField( "run_speed", 1.7, DoubleField.Range.POSITIVE,
                    "The speed multiplier while this AI is active. For reference, sprint-jumping gives players about a 1.65 speed multi." ) );
            
            SPEC.newLine();
            
            endRangeSqrMin = SPEC.define( new SqrDoubleField( "end_range.min", 3.0, DoubleField.Range.POSITIVE,
                    "This AI will deactivate when the entity is between the minimum and maximum distance from its target, in blocks (meters)." ) );
            endRangeSqrMax = SPEC.define( new SqrDoubleField( "end_range.max", 6.0, DoubleField.Range.POSITIVE ) );
            
            SPEC.newLine();
            
            runningBootsDropChance = SPEC.define( new DoubleField( "running_boots.drop_chance", 0.085, DoubleField.Range.DROP_CHANCE,
                    "The drop chance for Running Boots (0 to 1). The item will drop damaged randomly, like normal equipment.",
                    "A negative value disables the item entirely, while a value over 1 prevents the item from being damaged on drop." ) );
            runningBootsModifier = SPEC.define( new DoubleField( "running_boots.modifier", 0.15,
                    "The movement speed modifier on Running Boots (uses 'multiply base' operation). 0 disables the modifier." ) );
        }
    }
    
    public static class Barrage extends EliteAICategory {
        
        public final SqrDoubleField rangeSqrMin;
        public final SqrDoubleField rangeSqrMax;
        
        public final IntField.RandomRange cooldown;
        
        public final DoubleField arrowDamage;
        public final DoubleField arrowVariance;
        public final IntField shotTime;
        
        public final IntField chargeUpDuration;
        public final IntField shootingDuration;
        
        public final DoubleField dispenserDropChance;
        
        Barrage( ToastConfigSpec parent ) {
            super( parent, EliteAIType.BARRAGE, false, new AttributeMods().addedMaxHealth( 20.0 ) );
            
            rangeSqrMin = SPEC.define( new SqrDoubleField( "range.min", 5.0, DoubleField.Range.POSITIVE,
                    "The minimum and maximum distance the entity must be from its target for this AI to activate, in blocks (meters)." ) );
            rangeSqrMax = SPEC.define( new SqrDoubleField( "range.max", 16.0, DoubleField.Range.POSITIVE ) );
            
            SPEC.newLine();
            
            cooldown = new IntField.RandomRange(
                    SPEC.define( new IntField( "cooldown.min", 100, IntField.Range.NON_NEGATIVE,
                            "The minimum and maximum (inclusive) time this AI is disabled for after",
                            "completion, in ticks (20 ticks = 1 second)." ) ),
                    SPEC.define( new IntField( "cooldown.max", 160, IntField.Range.NON_NEGATIVE ) )
            );
            
            SPEC.newLine();
            
            arrowDamage = SPEC.define( new DoubleField( "arrow_damage", 3.0, 0.25, Double.POSITIVE_INFINITY,
                    "The base damage dealt by arrows. Note this varies \u00b10.25, gains +0.11 per difficulty level,",
                    "and final damage is scaled by total velocity (this is the normal behavior for monster-fired arrows)." ) );
            arrowVariance = SPEC.define( new DoubleField( "arrow_variance", 20.0, DoubleField.Range.POSITIVE,
                    "The direction variance for fired arrows. The higher this value, the less accurate arrows are." ) );
            shotTime = SPEC.define( new IntField( "shot_time", 5, IntField.Range.POSITIVE,
                    "The time between each arrow shot during the barrage (20 ticks = 1 second)." ) );
            
            SPEC.newLine();
            
            chargeUpDuration = SPEC.define( new IntField( "charge_up_duration", 30, IntField.Range.POSITIVE,
                    "The time the entity pauses for after this AI activates before starting the barrage (20 ticks = 1 second)." ) );
            shootingDuration = SPEC.define( new IntField( "shooting_duration", 60, IntField.Range.POSITIVE,
                    "The time the entity fires the barrage of arrows for (20 ticks = 1 second)." ) );
            
            SPEC.newLine();
            
            dispenserDropChance = SPEC.define( new DoubleField( "dispenser.drop_chance", 0.0, DoubleField.Range.DROP_CHANCE,
                    "The drop chance for the dispenser 'hat' (0 to 1). A negative value disables the item entirely." ) );
        }
    }
    
    public static class Charge extends EliteAICategory {
        
        public final SqrDoubleField rangeSqrMin;
        public final SqrDoubleField rangeSqrMax;
        
        public final IntField.RandomRange cooldown;
        
        public final SpeedField chargingSpeed;
        public final SpeedField knockbackSpeed;
        public final DoubleField selfDamage;
        
        public final IntField chargeUpDuration;
        public final IntField chargingDuration;
        public final IntField stunnedDuration;
        
        public final DoubleField chargersHelmetDropChance;
        public final IntField chargersHelmetEnchantLevel;
        public final BooleanField chargersHelmetAllowTreasure;
        
        Charge( ToastConfigSpec parent ) {
            super( parent, EliteAIType.CHARGE, false, new AttributeMods().addedMaxHealth( 20.0 ).knockbackResist( 0.5 ) );
            
            rangeSqrMin = SPEC.define( new SqrDoubleField( "range.min", 6.0, DoubleField.Range.POSITIVE,
                    "The minimum and maximum distance the entity must be from its target for this AI to activate, in blocks (meters)." ) );
            rangeSqrMax = SPEC.define( new SqrDoubleField( "range.max", 16.0, DoubleField.Range.POSITIVE ) );
            
            SPEC.newLine();
            
            cooldown = new IntField.RandomRange(
                    SPEC.define( new IntField( "cooldown.min", 80, IntField.Range.NON_NEGATIVE,
                            "The minimum and maximum (inclusive) time this AI is disabled for after",
                            "activation, in ticks (20 ticks = 1 second)." ) ),
                    SPEC.define( new IntField( "cooldown.max", 140, IntField.Range.NON_NEGATIVE ) )
            );
            
            SPEC.newLine();
            
            chargingSpeed = SPEC.define( new SpeedField( "charging_speed", 28.0, DoubleField.Range.POSITIVE,
                    "The horizontal speed given to the entity in its target's direction while charging, in blocks per second (m/s)." ) );
            knockbackSpeed = SPEC.define( new SpeedField( "knockback_speed", 120.0, DoubleField.Range.POSITIVE,
                    "The horizontal speed given to the target on hit while charging, in blocks per second (m/s)." ) );
            selfDamage = SPEC.define( new DoubleField( "self_damage", 5.0, DoubleField.Range.POSITIVE,
                    "The horizontal speed given to the entity in its target's direction while charging, in blocks per second (m/s)." ) );
            
            SPEC.newLine();
            
            chargeUpDuration = SPEC.define( new IntField( "charge_up_duration", 30, IntField.Range.POSITIVE,
                    "The time the entity pauses for after this AI activates before starting the charge attack (20 ticks = 1 second)." ) );
            chargingDuration = SPEC.define( new IntField( "charging_duration", 20, IntField.Range.POSITIVE,
                    "The time the entity rapidly moves forward for during the charge attack (20 ticks = 1 second)." ) );
            stunnedDuration = SPEC.define( new IntField( "stunned_duration", 60, IntField.Range.POSITIVE,
                    "The time the entity is inactive for if it hits a wall during the charge attack (20 ticks = 1 second)." ) );
            
            SPEC.newLine();
            
            chargersHelmetDropChance = SPEC.define( new DoubleField( "charger_helmet.drop_chance", 0.085, DoubleField.Range.DROP_CHANCE,
                    "The drop chance for Charger's Helmets (0 to 1). The item will drop damaged randomly, like normal equipment.",
                    "A negative value disables the item entirely, while a value over 1 prevents the item from being damaged on drop." ) );
            chargersHelmetEnchantLevel = SPEC.define( new IntField( "charger_helmet.enchant_level", 30, IntField.Range.NON_NEGATIVE,
                    "Levels to enchant Charger's Helmets. 0 disables the enchant." ) );
            chargersHelmetAllowTreasure = SPEC.define( new BooleanField( "charger_helmet.allow_treasure_enchants", true,
                    "If true, treasure enchantments will be able to appear on Charger's Helmets." ) );
        }
    }
    
    public static class Thief extends EliteAICategory {
        
        public final DoubleField moveSpeed;
        
        public final DoubleField avoidRange;
        public final DoubleField avoidWalkSpeed;
        public final DoubleField avoidRunSpeed;
        
        public final EnumField<ThiefEliteGoal.ValidSlots> validSlots;
        public final DoubleField stealDamage;
        public final IntField invisibilityDuration;
        
        public final BooleanField emptyHand;
        public final DoubleField thiefCapDropChance;
        public final DoubleField thiefCapModifier;
        
        Thief( ToastConfigSpec parent ) {
            super( parent, EliteAIType.THIEF, false, new AttributeMods().speed( 0.1 ) );
            
            moveSpeed = SPEC.define( new DoubleField( "move_speed", 1.2, DoubleField.Range.POSITIVE,
                    "The speed multiplier while moving to the target player to steal an item." ) );
            
            SPEC.newLine();
            
            avoidRange = SPEC.define( new DoubleField( "avoid_range", 16.0, DoubleField.Range.POSITIVE,
                    "The maximum range to avoid players in.",
                    "Note that all changes to the avoidance behavior require reload to take effect on existing entities." ) );
            avoidWalkSpeed = SPEC.define( new DoubleField( "avoid_speed.walk", 1.0, DoubleField.Range.POSITIVE,
                    "The speed multiplier while avoiding far away players (" + ModCore.GREATER_OR_EQUAL + " 7 blocks away)." ) );
            avoidRunSpeed = SPEC.define( new DoubleField( "avoid_speed.run", 1.2, DoubleField.Range.POSITIVE,
                    "The speed multiplier while avoiding nearby players (< 7 blocks away)." ) );
            
            SPEC.newLine();
            
            validSlots = SPEC.define( new EnumField<>( "valid_slots", ThiefEliteGoal.ValidSlots.HOTBAR_AND_ARMOR,
                    "The inventory slots that can be stolen from by entities with this AI.",
                    "For this purpose, the armor inventory is your head, chest, legs, and feet slots, the main inventory is",
                    "the big 3x9 inventory space, and the hotbar inventory is everything else (bottom 9 + offhand slots)." ) );
            stealDamage = SPEC.define( new DoubleField( "steal_damage", 1.0, DoubleField.Range.POSITIVE,
                    "The damage dealt to the player when stealing an item." ) );
            invisibilityDuration = SPEC.define( new IntField( "invisibility_duration", 60, IntField.Range.NON_NEGATIVE,
                    "The time the entity turns invisible for after stealing an item (20 ticks = 1 second)." ) );
            
            SPEC.newLine();
            
            emptyHand = SPEC.define( new BooleanField( "empty_hand", true,
                    "When true, the mob will always have its main hand item cleared when given this AI.",
                    "Keep in mind, if this is disabled and the mob spawns with a held item, it will just avoid players." ) );
            thiefCapDropChance = SPEC.define( new DoubleField( "thief_helmet.drop_chance", 0.085, DoubleField.Range.DROP_CHANCE,
                    "The drop chance for Thief's Caps (0 to 1). The item will drop damaged randomly, like normal equipment.",
                    "A negative value disables the item entirely, while a value over 1 prevents the item from being damaged on drop." ) );
            thiefCapModifier = SPEC.define( new DoubleField( "thief_helmet.modifier", 0.15,
                    "The movement speed modifier on Thief's Caps (uses 'multiply base' operation). 0 disables the modifier." ) );
        }
    }
    
    public static class Shaman extends EliteAICategory {
        
        public final SqrDoubleField auraRangeSqr;
        public final DoubleField healAmount;
        public final BooleanField extinguish;
        
        public final IntField strengthPotency;
        public final IntField resistancePotency;
        public final IntField speedPotency;
        public final BooleanField slowFalling;
        public final BooleanField fireResistance;
        public final BooleanField waterBreathing;
        
        public final DoubleField boneDropChance;
        public final DoubleField jackOLanternDropChance;
        
        Shaman( ToastConfigSpec parent ) {
            super( parent, EliteAIType.SHAMAN, false );
            
            auraRangeSqr = SPEC.define( new SqrDoubleField( "aura_range", 16.0, DoubleField.Range.POSITIVE,
                    "The range for the entity's aura (in blocks), which pulses healing and effects every two seconds to allies." ) );
            healAmount = SPEC.define( new DoubleField( "heal_amount", 2.0, DoubleField.Range.POSITIVE,
                    "The amount of health restored with each aura pulse (in half-hearts)." ) );
            extinguish = SPEC.define( new BooleanField( "extinguish", true,
                    "If true, the entity will extinguish burning with each aura pulse." ) );
            
            SPEC.newLine();
            
            strengthPotency = SPEC.define( new IntField( "strength_potency", 0, IntField.Range.TOKEN_NEGATIVE,
                    "The potency of the Strength effect to apply with each aura pulse. A negative value disables the effect entirely." ) );
            resistancePotency = SPEC.define( new IntField( "resistance_potency", 0, IntField.Range.TOKEN_NEGATIVE,
                    "The potency of the Resistance effect to apply with each aura pulse. A negative value disables the effect entirely." ) );
            speedPotency = SPEC.define( new IntField( "speed_potency", -1, IntField.Range.TOKEN_NEGATIVE,
                    "The potency of the Speed effect to apply with each aura pulse. A negative value disables the effect entirely." ) );
            slowFalling = SPEC.define( new BooleanField( "slow_falling", false,
                    "If true, the entity will apply the Slow Falling effect with each aura pulse." ) );
            fireResistance = SPEC.define( new BooleanField( "fire_resistance", true,
                    "If true, the entity will apply the Fire Resistance effect with each aura pulse." ) );
            waterBreathing = SPEC.define( new BooleanField( "water_breathing", false,
                    "If true, the entity will apply the Water Breathing effect with each aura pulse." ) );
            
            SPEC.newLine();
            
            boneDropChance = SPEC.define( new DoubleField( "bone.drop_chance", 0.085, DoubleField.Range.DROP_CHANCE,
                    "The drop chance for the bone 'club' (0 to 1). A negative value disables the item entirely." ) );
            jackOLanternDropChance = SPEC.define( new DoubleField( "jack_o_lantern.drop_chance", 0.085, DoubleField.Range.DROP_CHANCE,
                    "The drop chance for the jack o lantern 'hat' (0 to 1). A negative value disables the item entirely." ) );
        }
    }
    
    public static class Spawner extends EliteAICategory {
        
        public final IntField range;
        
        public final IntField initialCooldown;
        public final IntField.RandomRange cooldown;
        
        public final IntField spawnCount;
        public final IntField spawnRange;
        public final IntField maxNearby;
        
        public final BooleanField spawnerHelmet;
        
        Spawner( ToastConfigSpec parent ) {
            super( parent, EliteAIType.SPAWNER, false, new AttributeMods().addedMaxHealth( 20.0 ).knockbackResist( 0.5 )
                    .armor( 10.0 ).armorToughness( 10.0 ).speed( -0.3 ) );
            
            SPEC.comment( "NOTE: Changes to this particular AI will NOT update existing entities (even on reload)!",
                    "These config options are saved as NBT to mobs with the same structure as vanilla spawner NBT." );
            
            SPEC.newLine();
            
            range = SPEC.define( new IntField( "range", 16, IntField.Range.POSITIVE,
                    "The maximum distance the entity can be from its target for this AI to activate, in blocks (meters)." ) );
            
            SPEC.newLine();
            
            initialCooldown = SPEC.define( new IntField( "cooldown.initial", 20, IntField.Range.TOKEN_NEGATIVE,
                    "Time before the first spawn 'wave', in ticks. If set to -1, the initial cooldown is randomized between the min and max." ) );
            cooldown = new IntField.RandomRange(
                    SPEC.define( new IntField( "cooldown.min", 200, IntField.Range.NON_NEGATIVE,
                            "The minimum and maximum (inclusive) time between spawn 'waves', in ticks (20 ticks = 1 second)." ) ),
                    SPEC.define( new IntField( "cooldown.max", 800, IntField.Range.NON_NEGATIVE ) )
            );
            
            SPEC.newLine();
            
            spawnCount = SPEC.define( new IntField( "spawn_count", 4, IntField.Range.POSITIVE,
                    "The number of entities to try to spawn with each spawn 'wave'." ) );
            spawnRange = SPEC.define( new IntField( "spawn_range", 4, IntField.Range.NON_NEGATIVE,
                    "The maximum distance entities can spawn from the spawner entity, in blocks (meters)." ) );
            maxNearby = SPEC.define( new IntField( "max_nearby_entities", 6, IntField.Range.POSITIVE,
                    "The spawner will not create any new entities while the number of similar entities within spawn range",
                    "is at or above this limit." ) );
            
            SPEC.newLine();
            
            spawnerHelmet = SPEC.define( new BooleanField( "spawner_helmet", true,
                    "If true, the entity will have a spawner equipped on its head (does not have a drop chance)." ) );
        }
    }
    
    public static class ThrowAlly extends EliteAICategory {
        
        public final SqrDoubleField throwRangeSqrMin;
        public final SqrDoubleField throwRangeSqrMax;
        
        public final SqrDoubleField allyRangeSqrMin;
        
        public final IntField.RandomRange cooldown;
        
        public final DoubleField speedToAlly;
        public final DoubleField speedToTarget;
        
        public final SpeedField throwSpeedForward;
        public final SpeedField throwSpeedUpward;
        
        ThrowAlly( ToastConfigSpec parent ) {
            super( parent, EliteAIType.THROW_ALLY, false, new AttributeMods().speed( 0.1 ) );
            
            throwRangeSqrMin = SPEC.define( new SqrDoubleField( "throw_range.min", 3.0, DoubleField.Range.POSITIVE,
                    "The minimum and maximum distance the entity must be from its target for this AI to activate, in blocks (meters)." ) );
            throwRangeSqrMax = SPEC.define( new SqrDoubleField( "throw_range.max", 10.0, DoubleField.Range.POSITIVE ) );
            
            SPEC.newLine();
            
            allyRangeSqrMin = SPEC.define( new SqrDoubleField( "ally_range.min", 6.0, DoubleField.Range.POSITIVE,
                    "This AI will only attempt to throw allies farther than this distance from the target, in blocks (meters)." ) );
            
            SPEC.newLine();
            
            cooldown = new IntField.RandomRange(
                    SPEC.define( new IntField( "cooldown.min", 60, IntField.Range.NON_NEGATIVE,
                            "The minimum and maximum (inclusive) time this AI is disabled for after",
                            "activation, in ticks (20 ticks = 1 second)." ) ),
                    SPEC.define( new IntField( "cooldown.max", 100, IntField.Range.NON_NEGATIVE ) )
            );
            
            SPEC.newLine();
            
            speedToAlly = SPEC.define( new DoubleField( "speed_to_ally", 1.3, DoubleField.Range.POSITIVE,
                    "The speed multiplier while moving to pick up the ally to throw." ) );
            speedToTarget = SPEC.define( new DoubleField( "speed_to_target", 1.1, DoubleField.Range.POSITIVE,
                    "The speed multiplier while moving to the target (carrying the ally to throw)." ) );
            
            SPEC.newLine();
            
            throwSpeedForward = SPEC.define( new SpeedField( "throw_speed.forward", 20.0,
                    "The horizontal speed given to the thrown entity in its target's direction, in blocks per second (m/s)." ) );
            throwSpeedUpward = SPEC.define( new SpeedField( "throw_speed.upward", 8.4,
                    "The vertical speed given to the thrown entity, in blocks per second (m/s).",
                    "For reference, normal jumping speed is 8.4 m/s." ) );
        }
    }
    
    public static class ThrowEnemy extends EliteAICategory {
        
        public final SqrDoubleField throwRangeSqrMin;
        public final SqrDoubleField throwRangeSqrMax;
        public final DoubleField carryRange;
        
        public final IntField.RandomRange cooldown;
        
        public final DoubleField speedToTarget;
        public final DoubleField speedToAlly;
        
        public final IntField.RandomRange reGrabs;
        
        public final SpeedField throwSpeedForward;
        public final SpeedField throwSpeedUpward;
        
        public final DoubleField strengthHelmetDropChance;
        public final DoubleField strengthHelmetModifier;
        
        ThrowEnemy( ToastConfigSpec parent ) {
            super( parent, EliteAIType.THROW_ENEMY, false, new AttributeMods().knockbackResist( 0.5 ) );
            
            throwRangeSqrMin = SPEC.define( new SqrDoubleField( "throw_range.min", 3.0, DoubleField.Range.POSITIVE,
                    "The minimum and maximum distance the entity must be from its target for this AI to activate, in blocks (meters)." ) );
            throwRangeSqrMax = SPEC.define( new SqrDoubleField( "throw_range.max", 10.0, DoubleField.Range.POSITIVE ) );
            carryRange = SPEC.define( new DoubleField( "carry_range", 8.0, DoubleField.Range.POSITIVE,
                    "The maximum range the entity will plan on carrying its target before throwing.",
                    "Note that this only affects the search radius when finding allies to throw to." ) );
            
            SPEC.newLine();
            
            cooldown = new IntField.RandomRange(
                    SPEC.define( new IntField( "cooldown.min", 100, IntField.Range.NON_NEGATIVE,
                            "The minimum and maximum (inclusive) time this AI is disabled for after",
                            "activation, in ticks (20 ticks = 1 second)." ) ),
                    SPEC.define( new IntField( "cooldown.max", 160, IntField.Range.NON_NEGATIVE ) )
            );
            
            SPEC.newLine();
            
            speedToTarget = SPEC.define( new DoubleField( "speed_to_target", 1.3, DoubleField.Range.POSITIVE,
                    "The speed multiplier while moving to pick up the enemy/player to throw." ) );
            speedToAlly = SPEC.define( new DoubleField( "speed_to_ally", 1.1, DoubleField.Range.POSITIVE,
                    "The speed multiplier while moving to allies (carrying the enemy/player to throw)." ) );
            
            SPEC.newLine();
            
            reGrabs = new IntField.RandomRange(
                    SPEC.define( new IntField( "re_grabs.min", 0, IntField.Range.NON_NEGATIVE,
                            "The minimum and maximum (inclusive) number of times an entity will attempt to re-grab its",
                            "target if it escapes the grab before being thrown. Re-rolls each time the AI activates.",
                            "This AI goes into cooldown if the target escapes and the entity has no remaining re-grabs." ) ),
                    SPEC.define( new IntField( "re_grabs.max", 3, IntField.Range.NON_NEGATIVE ) )
            );
            
            SPEC.newLine();
            
            throwSpeedForward = SPEC.define( new SpeedField( "throw_speed.forward", 20.0,
                    "The horizontal speed given to the thrown entity in its target's direction, in blocks per second (m/s)." ) );
            throwSpeedUpward = SPEC.define( new SpeedField( "throw_speed.upward", 8.4,
                    "The vertical speed given to the thrown entity, in blocks per second (m/s).",
                    "For reference, normal jumping speed is 8.4 m/s." ) );
            
            SPEC.newLine();
            
            strengthHelmetDropChance = SPEC.define( new DoubleField( "helmet_of_strength.drop_chance", 0.085, DoubleField.Range.DROP_CHANCE,
                    "The drop chance for Helmets of Strength (0 to 1). The item will drop damaged randomly, like normal equipment.",
                    "A negative value disables the item entirely, while a value over 1 prevents the item from being damaged on drop." ) );
            strengthHelmetModifier = SPEC.define( new DoubleField( "helmet_of_strength.modifier", 1.0,
                    "The attack damage modifier on Helmets of Strength (uses 'addition' operation). 0 disables the modifier." ) );
        }
    }
    
    /**
     * The category extended by each elite AI pattern's category to provide some common structure.
     */
    public abstract static class EliteAICategory extends Config.AbstractCategory {
        
        public final BooleanField preferMelee;
        
        public final DoubleField followRange;
        public final DoubleField addedMaxHealth;
        public final DoubleField increasedMaxHealth;
        public final DoubleField knockbackResist;
        public final DoubleField armor;
        public final DoubleField armorToughness;
        public final DoubleField addedDamage;
        public final DoubleField increasedDamage;
        public final DoubleField knockback;
        public final DoubleField speed;
        
        /** Creates a category that has no attribute modifiers by default. */
        EliteAICategory( ToastConfigSpec parent, EliteAIType ai, boolean defaultPreferMelee ) {
            this( parent, ai, defaultPreferMelee, new AttributeMods() );
        }
        
        /** Creates a category that has the specified attribute modifiers by default. */
        EliteAICategory( ToastConfigSpec parent, EliteAIType ai, boolean defaultPreferMelee, AttributeMods attributes ) {
            super( parent, ai.getKey(),
                    "Options for customizing the " + ai.getDisplayName() + " elite AI pattern." );
            
            preferMelee = SPEC.define( new BooleanField( "prefer_melee", defaultPreferMelee,
                    "When true, entities given this AI will prefer melee weapons. See the general category for more info." ) );
            
            SPEC.newLine();
            
            SPEC.comment( "Attribute modifiers applied to entities with this AI on spawn, if possible. Modifiers are disabled if set to 0.",
                    "Notably, most passive mobs do not have damage or knockback attributes, so those modifiers cannot apply to them.",
                    "Added modifiers use the 'addition' operation and increased modifiers use the 'multiply base' operation.",
                    TomlHelper.multiFieldInfo( DoubleField.Range.ANY ) );
            followRange = SPEC.define( new DoubleField( "modifier.added_follow_range", attributes.followRange, (String[]) null ) );
            addedMaxHealth = SPEC.define( new DoubleField( "modifier.added_max_health", attributes.addedMaxHealth, (String[]) null ) );
            increasedMaxHealth = SPEC.define( new DoubleField( "modifier.increased_max_health", attributes.increasedMaxHealth, (String[]) null ) );
            knockbackResist = SPEC.define( new DoubleField( "modifier.added_knockback_resistance", attributes.knockbackResist, (String[]) null ) );
            armor = SPEC.define( new DoubleField( "modifier.added_armor", attributes.armor, (String[]) null ) );
            armorToughness = SPEC.define( new DoubleField( "modifier.added_armor_toughness", attributes.armorToughness, (String[]) null ) );
            addedDamage = SPEC.define( new DoubleField( "modifier.added_damage", attributes.addedDamage, (String[]) null ) );
            increasedDamage = SPEC.define( new DoubleField( "modifier.increased_damage", attributes.increasedDamage, (String[]) null ) );
            knockback = SPEC.define( new DoubleField( "modifier.added_knockback", attributes.knockback, (String[]) null ) );
            speed = SPEC.define( new DoubleField( "modifier.increased_speed", attributes.speed, (String[]) null ) );
            
            SPEC.newLine();
        }
    }
    
    /**
     * A builder-like class used to conveniently describe many optional parameters for the base elite AI type config category.
     */
    @SuppressWarnings( { "unused", "SameParameterValue" } )
    private static class AttributeMods {
        double followRange;
        double addedMaxHealth;
        double increasedMaxHealth;
        double knockbackResist;
        double armor;
        double armorToughness;
        double addedDamage;
        double increasedDamage;
        double knockback;
        double speed;
        
        AttributeMods followRange( double value ) {
            followRange = value;
            return this;
        }
        
        AttributeMods addedMaxHealth( double value ) {
            addedMaxHealth = value;
            return this;
        }
        
        AttributeMods increasedMaxHealth( double value ) {
            increasedMaxHealth = value;
            return this;
        }
        
        AttributeMods knockbackResist( double value ) {
            knockbackResist = value;
            return this;
        }
        
        AttributeMods armor( double value ) {
            armor = value;
            return this;
        }
        
        AttributeMods armorToughness( double value ) {
            armorToughness = value;
            return this;
        }
        
        AttributeMods addedDamage( double value ) {
            addedDamage = value;
            return this;
        }
        
        AttributeMods increasedDamage( double value ) {
            increasedDamage = value;
            return this;
        }
        
        AttributeMods knockback( double value ) {
            knockback = value;
            return this;
        }
        
        AttributeMods speed( double value ) {
            speed = value;
            return this;
        }
    }
}