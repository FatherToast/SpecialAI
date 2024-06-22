package fathertoast.specialai.ai.elite;

import fathertoast.crust.api.config.common.value.WeightedList;
import fathertoast.crust.api.lib.NBTHelper;
import fathertoast.specialai.config.Config;
import fathertoast.specialai.config.EliteAIConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.DyeableArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.List;

/**
 * This represents an elite AI. Any number of these additional AIs may be saved/loaded to entities.
 */
public enum EliteAIType implements WeightedList.Value {
    
    LEAP( "Leap", 200, LeapEliteGoal::new ) {
        /** @return Returns this AI type's config category. */
        @Override
        public EliteAIConfig.EliteAICategory getConfigCategory() { return Config.ELITE_AI.LEAP; }
        
        /** Adds a description of what this AI type does to the list, using default values. */
        @Override
        public void describe( List<String> list ) {
            list.add( "The " + getDisplayName() + " elite AI causes an entity to make short, quick jumps at their target, similar to a spider." );
            list.add( ChatFormatting.GRAY + "Activation Range: Short*, Cooldown: Short*, Jump Power: Low*" );
            list.add( ChatFormatting.GRAY + "Attributes: Added Knockback, Increased Movement Speed" );
            list.add( ChatFormatting.GRAY + "Equipment: Prefer Melee" );
        }
    },
    
    JUMP( "Jump", 100, JumpEliteGoal::new ) {
        /** @return Returns this AI type's config category. */
        @Override
        public EliteAIConfig.EliteAICategory getConfigCategory() { return Config.ELITE_AI.JUMP; }
        
        /** Adds a description of what this AI type does to the list, using default values. */
        @Override
        public void describe( List<String> list ) {
            list.add( "The " + getDisplayName() + " elite AI causes an entity to make long, high jumps at their target when at range." );
            list.add( ChatFormatting.GRAY + "Activation Range: Medium*, Cooldown: Medium*, Jump Power: High* (ignores fall damage)" );
            list.add( ChatFormatting.GRAY + "Attributes: n/a" );
            list.add( ChatFormatting.GRAY + "Equipment: Feather Boots (lavender leather boots enchanted with Feather Falling IV*)" );
        }
        
        /** Initializes one-time effects on the entity specific to this AI type, such as unique equipment. Called before the first load. */
        @Override
        public void initialize( Mob entity, CompoundTag aiTag ) {
            // Feather boots
            ItemStack boots = new ItemStack( Items.LEATHER_BOOTS );
            if( Config.ELITE_AI.JUMP.featherBootsEnchant.get() > 0 ) {
                boots.enchant( Enchantments.FALL_PROTECTION, Config.ELITE_AI.JUMP.featherBootsEnchant.get() );
            }
            boots.setHoverName( EliteAIHelper.getText( this, "boots" ) );
            ((DyeableArmorItem) boots.getItem()).setColor( boots, 0x9664B4 );
            EliteAIHelper.equip( entity, boots, Config.ELITE_AI.JUMP.featherBootsDropChance.get() );
        }
    },
    
    SPRINT( "Sprint", 200, SprintEliteGoal::new ) {
        /** @return Returns this AI type's config category. */
        @Override
        public EliteAIConfig.EliteAICategory getConfigCategory() { return Config.ELITE_AI.SPRINT; }
        
        /** Adds a description of what this AI type does to the list, using default values. */
        @Override
        public void describe( List<String> list ) {
            list.add( "The " + getDisplayName() + " elite AI causes an entity to sprint to their target when far away." );
            list.add( ChatFormatting.GRAY + "Activation Range: Long*, Cooldown: Equal to Time Active, Deactivation Range: Short*" );
            list.add( ChatFormatting.GRAY + "Attributes: n/a (though boots grant increased movement speed*)" );
            list.add( ChatFormatting.GRAY + "Equipment: Running Boots (red leather boots with increased movement speed*)" );
        }
        
        /** Initializes one-time effects on the entity specific to this AI type, such as unique equipment. Called before the first load. */
        @Override
        public void initialize( Mob entity, CompoundTag aiTag ) {
            // Running boots
            ItemStack boots = new ItemStack( Items.LEATHER_BOOTS );
            EliteAIHelper.addModifier( this, boots, Attributes.MOVEMENT_SPEED, Config.ELITE_AI.SPRINT.runningBootsModifier.get(),
                    AttributeModifier.Operation.MULTIPLY_BASE );
            boots.setHoverName( EliteAIHelper.getText( this, "boots" ) );
            ((DyeableArmorItem) boots.getItem()).setColor( boots, 0xFF0000 );
            EliteAIHelper.equip( entity, boots, Config.ELITE_AI.SPRINT.runningBootsDropChance.get() );
        }
    },
    
    SLAM( "Slam", 50, SlamEliteGoal::new ) {
        /** @return Returns this AI type's config category. */
        @Override
        public EliteAIConfig.EliteAICategory getConfigCategory() { return Config.ELITE_AI.SLAM; }
        
        /** Adds a description of what this AI type does to the list, using default values. */
        @Override
        public void describe( List<String> list ) {
            list.add( "The " + getDisplayName() + " elite AI causes an entity to periodically retaliate with an explosive slam attack." );
            list.add( ChatFormatting.GRAY + "Activation Range: Short*, Cooldown: Medium*, Damage: Explosion" );
            list.add( ChatFormatting.GRAY + "Attributes: Added Knockback Resistance, Added Armor" );
            list.add( ChatFormatting.GRAY + "Equipment: Axe" );
        }
        
        /** Initializes one-time effects on the entity specific to this AI type, such as unique equipment. Called before the first load. */
        @Override
        public void initialize( Mob entity, CompoundTag aiTag ) {
            // Axe
            ItemStack axe = new ItemStack( Config.ELITE_AI.SLAM.axeType.get().ITEM );
            if( Config.ELITE_AI.SLAM.axeEnchantLevel.get() > 0 && Config.ELITE_AI.SLAM.axeEnchantChance.rollChance( entity.getRandom() ) ) {
                EnchantmentHelper.enchantItem( entity.getRandom(), axe, Config.ELITE_AI.SLAM.axeEnchantLevel.get(),
                        Config.ELITE_AI.SLAM.axeAllowTreasure.get() );
            }
            EliteAIHelper.equip( entity, axe, Config.ELITE_AI.SLAM.axeDropChance.get() );
        }
    },
    
    BARRAGE( "Barrage", 50, BarrageEliteGoal::new ) {
        /** @return Returns this AI type's config category. */
        @Override
        public EliteAIConfig.EliteAICategory getConfigCategory() { return Config.ELITE_AI.BARRAGE; }
        
        /** Adds a description of what this AI type does to the list, using default values. */
        @Override
        public void describe( List<String> list ) {
            list.add( "The " + getDisplayName() + " elite AI causes an entity to periodically charge up and fire a stream of arrows." );
            list.add( ChatFormatting.GRAY + "Activation Range: Medium*, Cooldown: Long*, Damage: Low*, Fire Rate: High*, Duration: Medium*" );
            list.add( ChatFormatting.GRAY + "Attributes: Added Max Health" );
            list.add( ChatFormatting.GRAY + "Equipment: Dispenser Head" );
        }
        
        /** Initializes one-time effects on the entity specific to this AI type, such as unique equipment. Called before the first load. */
        @Override
        public void initialize( Mob entity, CompoundTag aiTag ) {
            // Dispenser head
            EliteAIHelper.equip( entity, new ItemStack( Items.DISPENSER ), Config.ELITE_AI.BARRAGE.dispenserDropChance.get(),
                    EquipmentSlot.HEAD );
        }
    },
    
    CHARGE( "Charge", 100, ChargeEliteGoal::new ) {
        /** @return Returns this AI type's config category. */
        @Override
        public EliteAIConfig.EliteAICategory getConfigCategory() { return Config.ELITE_AI.CHARGE; }
        
        /** Adds a description of what this AI type does to the list, using default values. */
        @Override
        public void describe( List<String> list ) {
            list.add( "The " + getDisplayName() + " elite AI causes an entity to periodically charge up and then quickly " +
                    "run forward, dealing attack damage with bonus knockback. If an entity hits a wall while charging, they " +
                    "will take damage and enter a 'stunned' state for a short time, rendering them unable to act." );
            list.add( ChatFormatting.GRAY + "Activation Range: Medium*, Cooldown: Long*, Damage: Melee Attack Damage, Knockback: High*" );
            list.add( ChatFormatting.GRAY + "Attributes: Added Max Health, Added Knockback Resistance" );
            list.add( ChatFormatting.GRAY + "Equipment: Charger's Helmet (yellow leather helmet with random enchantments*)" );
        }
        
        /** Initializes one-time effects on the entity specific to this AI type, such as unique equipment. Called before the first load. */
        @Override
        public void initialize( Mob entity, CompoundTag aiTag ) {
            // Charger's Helmet
            ItemStack helmet = new ItemStack( Items.LEATHER_HELMET );
            if( Config.ELITE_AI.CHARGE.chargersHelmetEnchantLevel.get() > 0 ) {
                EnchantmentHelper.enchantItem( entity.getRandom(), helmet, Config.ELITE_AI.CHARGE.chargersHelmetEnchantLevel.get(),
                        Config.ELITE_AI.CHARGE.chargersHelmetAllowTreasure.get() );
            }
            helmet.setHoverName( EliteAIHelper.getText( this, "helmet" ) );
            ((DyeableArmorItem) helmet.getItem()).setColor( helmet, 0xFFFF00 );
            EliteAIHelper.equip( entity, helmet, Config.ELITE_AI.CHARGE.chargersHelmetDropChance.get() );
        }
    },
    
    THIEF( "Thief", 25, ThiefEliteGoal::new ) {
        /** @return Returns this AI type's config category. */
        @Override
        public EliteAIConfig.EliteAICategory getConfigCategory() { return Config.ELITE_AI.THIEF; }
        
        /** Adds a description of what this AI type does to the list, using default values. */
        @Override
        public void describe( List<String> list ) {
            list.add( "The " + getDisplayName() + " elite AI causes an entity to steal a random item from a player, " +
                    "briefly turn invisible, and then just run away. Overrides most normal mob behavior." );
            list.add( ChatFormatting.GRAY + "Damage: Very Low*, Invisibility Duration: Short*, Avoid Speed: Medium*" );
            list.add( ChatFormatting.GRAY + "Attributes: Increased Movement Speed" );
            list.add( ChatFormatting.GRAY + "Equipment: No Held Item, Thief's Cap (black leather helmet with increased movement speed*)" );
        }
        
        /** Initializes one-time effects on the entity specific to this AI type, such as unique equipment. Called before the first load. */
        @Override
        public void initialize( Mob entity, CompoundTag aiTag ) {
            // Disable loot pickup
            entity.setCanPickUpLoot( false );
            // Remove held item
            if( Config.ELITE_AI.THIEF.emptyHand.get() ) {
                EliteAIHelper.unequip( entity, EquipmentSlot.MAINHAND );
            }
            // Thief's Cap
            ItemStack helmet = new ItemStack( Items.LEATHER_HELMET );
            EliteAIHelper.addModifier( this, helmet, Attributes.MOVEMENT_SPEED, Config.ELITE_AI.THIEF.thiefCapModifier.get(),
                    AttributeModifier.Operation.MULTIPLY_BASE );
            helmet.setHoverName( EliteAIHelper.getText( this, "helmet" ) );
            ((DyeableArmorItem) helmet.getItem()).setColor( helmet, 0x102024 );
            EliteAIHelper.equip( entity, helmet, Config.ELITE_AI.THIEF.thiefCapDropChance.get() );
        }
    },
    
    SHAMAN( "Shaman", 50, ShamanEliteGoal::new ) {
        /** @return Returns this AI type's config category. */
        @Override
        public EliteAIConfig.EliteAICategory getConfigCategory() { return Config.ELITE_AI.SHAMAN; }
        
        /** Adds a description of what this AI type does to the list, using default values. */
        @Override
        public void describe( List<String> list ) {
            list.add( "The " + getDisplayName() + " elite AI causes an entity to heal and buff nearby allies, and " +
                    "prefer to follow allies in the back-lines instead of attacking." );
            list.add( ChatFormatting.GRAY + "Aura Range: Short-Long*, Aura Pulse Cooldown: 2 seconds" );
            list.add( ChatFormatting.GRAY + "Aura Healing: Low*, Aura Effects*: Strength, Resistance, Fire Resistance" );
            list.add( ChatFormatting.GRAY + "Attributes: n/a" );
            list.add( ChatFormatting.GRAY + "Equipment: Bone 'Club', Jack o'Lantern Mask" );
        }
        
        /** Initializes one-time effects on the entity specific to this AI type, such as unique equipment. Called before the first load. */
        @Override
        public void initialize( Mob entity, CompoundTag aiTag ) {
            // Bone club
            EliteAIHelper.equip( entity, new ItemStack( Items.BONE ), Config.ELITE_AI.SHAMAN.boneDropChance.get(),
                    EquipmentSlot.MAINHAND );
            // Jack o lantern hat
            EliteAIHelper.equip( entity, new ItemStack( Items.JACK_O_LANTERN ), Config.ELITE_AI.SHAMAN.jackOLanternDropChance.get(),
                    EquipmentSlot.HEAD );
        }
    },
    
    SPAWNER( "Spawner", 25, SpawnerEliteGoal::new ) {
        /** @return Returns this AI type's config category. */
        @Override
        public EliteAIConfig.EliteAICategory getConfigCategory() { return Config.ELITE_AI.SPAWNER; }
        
        /** Adds a description of what this AI type does to the list, using default values. */
        @Override
        public void describe( List<String> list ) {
            list.add( "The " + getDisplayName() + " elite AI causes an entity to act like a dungeon spawner block." );
            list.add( "Entities spawned by this elite AI will never get any elite AIs of their own." );
            list.add( ChatFormatting.GRAY + "Entity Spawned: Same Entity Type, Spawner Stats: Vanilla* (can be NBT edited)" );
            list.add( ChatFormatting.GRAY + "Attributes: Added Max Health, Added Knockback Resistance, Added Armor/Toughness, Reduced Movement Speed" );
            list.add( ChatFormatting.GRAY + "Equipment: Spawner Head" );
        }
        
        /** Initializes one-time effects on the entity specific to this AI type, such as unique equipment. Called before the first load. */
        @Override
        public void initialize( Mob entity, CompoundTag aiTag ) {
            // Spawner head
            if( Config.ELITE_AI.SPAWNER.spawnerHelmet.get() ) {
                EliteAIHelper.equip( entity, new ItemStack( Items.SPAWNER ), 0.0, EquipmentSlot.HEAD );
            }
        }
    },
    
    THROW_ALLY( "Throw Ally", 100, ThrowAllyEliteGoal::new ) {
        /** @return Returns this AI type's config category. */
        @Override
        public EliteAIConfig.EliteAICategory getConfigCategory() { return Config.ELITE_AI.THROW_ALLY; }
        
        /** Adds a description of what this AI type does to the list, using default values. */
        @Override
        public void describe( List<String> list ) {
            list.add( "The " + getDisplayName() + " elite AI causes an entity to throw nearby allies at its target when at range." );
            list.add( ChatFormatting.GRAY + "Activation Range: Long*, Cooldown: Medium*, Throw Power: Medium*" );
            list.add( ChatFormatting.GRAY + "Attributes: Increased Movement Speed" );
            list.add( ChatFormatting.GRAY + "Equipment: n/a" );
        }
    },
    
    THROW_ENEMY( "Throw Enemy", 25, ThrowEnemyEliteGoal::new ) {
        /** @return Returns this AI type's config category. */
        @Override
        public EliteAIConfig.EliteAICategory getConfigCategory() { return Config.ELITE_AI.THROW_ENEMY; }
        
        /** Adds a description of what this AI type does to the list, using default values. */
        @Override
        public void describe( List<String> list ) {
            list.add( "The " + getDisplayName() + " elite AI causes an entity to throw its target at nearby allies if they are far." );
            list.add( ChatFormatting.GRAY + "Activation Range: Short-Long*, Cooldown: Long*, Throw Power: Medium*" );
            list.add( ChatFormatting.GRAY + "Attributes: Added Knockback Resistance" );
            list.add( ChatFormatting.GRAY + "Equipment: Helmet of Strength (red leather helmet with added attack damage*)" );
        }
        
        /** Initializes one-time effects on the entity specific to this AI type, such as unique equipment. Called before the first load. */
        @Override
        public void initialize( Mob entity, CompoundTag aiTag ) {
            // Helmet of Strength
            ItemStack helmet = new ItemStack( Items.LEATHER_HELMET );
            EliteAIHelper.addModifier( this, helmet, Attributes.ATTACK_DAMAGE, Config.ELITE_AI.THROW_ENEMY.strengthHelmetModifier.get(),
                    AttributeModifier.Operation.ADDITION );
            helmet.setHoverName( EliteAIHelper.getText( this, "helmet" ) );
            ((DyeableArmorItem) helmet.getItem()).setColor( helmet, 0xFF0000 );
            EliteAIHelper.equip( entity, helmet, Config.ELITE_AI.THROW_ENEMY.strengthHelmetDropChance.get() );
        }
    };
    
    /** The suffix to append to AI type keys to generate the extra data tag name. */
    private static final String TAG_SUFFIX = "_data";
    
    /** The display name for this elite AI type. */
    private final String NAME;
    /** The unique key for this elite AI type. */
    private final String KEY;
    /** The default weight for this elite AI type in the config. */
    private final int DEFAULT_WEIGHT;
    /** Creates a new elite AI goal instance for an entity. */
    protected final IFactory FACTORY;
    
    EliteAIType( String name, int defaultWeight, IFactory factory ) {
        this( name, name.toLowerCase().replace( " ", "_" ), defaultWeight, factory );
    }
    
    EliteAIType( String name, String key, int defaultWeight, IFactory factory ) {
        NAME = name;
        KEY = key;
        DEFAULT_WEIGHT = defaultWeight;
        FACTORY = factory;
    }
    
    /** @return Returns this AI type's config category. */
    // Note: We can't store a reference to the config at construction time because we use these types to make the config
    public abstract EliteAIConfig.EliteAICategory getConfigCategory();
    
    /** Adds a description of what this AI type does to the list, using default values. */
    public abstract void describe( List<String> list );
    
    /** @return Returns the unique key for this object. */
    public final String getDisplayName() { return NAME; }
    
    /** @return Returns the unique key for this object. */
    @Override
    public final String getKey() { return KEY; }
    
    /** @return Returns the default weight for this object. */
    @Override
    public final int getDefaultWeight() { return DEFAULT_WEIGHT; }
    
    /** Saves this AI to the entity tag. */
    public final void saveTo( CompoundTag aiTag ) { aiTag.putBoolean( KEY, true ); }
    
    /** @return Returns true if this AI type is saved to the tag. */
    public final boolean isSaved( CompoundTag aiTag ) {
        if( NBTHelper.containsNumber( aiTag, KEY ) ) return aiTag.getBoolean( KEY );
        return false;
    }
    
    /** @return True if the nbt compound used to store any additional data used by the AI exists. */
    public final boolean hasTag( CompoundTag aiTag ) { return NBTHelper.containsCompound( aiTag, KEY + TAG_SUFFIX ); }
    
    /** @return Gets or creates the nbt compound used to store any additional data used by the AI. */
    public final CompoundTag getTag( CompoundTag aiTag ) { return NBTHelper.getOrCreateCompound( aiTag, KEY + TAG_SUFFIX ); }
    
    /** Initializes one-time effects on the entity specific to this AI type, such as unique equipment. Called before the first load. */
    public void initialize( Mob entity, CompoundTag aiTag ) { }
    
    /** Adds the AI goal corresponding to this type to the given entity, with any additional values loaded from the entity tag as needed. */
    public void loadTo( Mob entity, CompoundTag aiTag ) {
        entity.goalSelector.addGoal( 0, FACTORY.create( entity, aiTag ) );
    }
    
    private interface IFactory {
        AbstractEliteGoal create( Mob entity, CompoundTag aiTag );
    }
}