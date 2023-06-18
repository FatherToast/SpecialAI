package fathertoast.specialai.ai.elite;

import fathertoast.specialai.SpecialAI;
import fathertoast.specialai.config.Config;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Hand;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import javax.annotation.Nullable;

/**
 * This class contains several helper methods for managing elite AI.
 */
public final class EliteAIHelper {
    /** The base lang key for translating text for elite AIs. */
    private static final String LANG_KEY = SpecialAI.LANG_KEY + "elite.";
    
    /** The lang key for translating a specific line of text. */
    static String getLangKey( EliteAIType ai, String subKey ) { return LANG_KEY + ai.getKey() + "." + subKey; }
    
    /** The translation text component. */
    static ITextComponent getText( EliteAIType ai, String subKey ) { return new TranslationTextComponent( getLangKey( ai, subKey ) ); }
    
    /**
     * @param entity Applies a random elite AI to this mob.
     * @param aiTag  The mob's save data.
     */
    public static void saveEliteAI( CompoundNBT aiTag, MobEntity entity ) {
        saveEliteAI( aiTag, Config.ELITE_AI.GENERAL.eliteAIWeights.next( entity.getRandom(), entity.level, entity.blockPosition() ) );
    }
    
    /**
     * @param aiTag   The mob's save data.
     * @param eliteAI The AI pattern to apply.
     */
    public static void saveEliteAI( CompoundNBT aiTag, @Nullable EliteAIType eliteAI ) {
        if( eliteAI != null && !eliteAI.isSaved( aiTag ) ) eliteAI.saveTo( aiTag );
    }
    
    /**
     * @param entity Mob to apply loaded elite AI to.
     * @param aiTag  The mob's save data.
     * @param init   When true, the loaded AIs will apply their attribute modifiers and equipment.
     */
    public static void loadEliteAI( MobEntity entity, CompoundNBT aiTag, boolean init ) {
        float healthDiff = init ? entity.getMaxHealth() - entity.getHealth() : Float.NaN;
        
        // Load each AI goal and initialize, as needed
        for( EliteAIType ai : EliteAIType.values() ) {
            if( ai.isSaved( aiTag ) ) {
                if( init ) { initialize( ai, entity, aiTag ); }
                ai.loadTo( entity, aiTag );
            }
        }
        // Update health based on attribute changes during initialization
        if( init ) entity.setHealth( entity.getMaxHealth() - healthDiff );
    }
    
    /** Called to perform the initialization for an elite AI type. */
    private static void initialize( EliteAIType ai, MobEntity entity, CompoundNBT aiTag ) {
        // Handle weapon preference
        if( ai.getConfigCategory().preferMelee.get() ) {
            preferMelee( entity );
        }
        // Apply attribute modifiers; these are all configurable for all AI types, just for fun
        addModifier( ai, entity, Attributes.FOLLOW_RANGE, ai.getConfigCategory().followRange.get(), AttributeModifier.Operation.ADDITION );
        addModifier( ai, entity, Attributes.MAX_HEALTH, ai.getConfigCategory().addedMaxHealth.get(), AttributeModifier.Operation.ADDITION );
        addModifier( ai, entity, Attributes.MAX_HEALTH, ai.getConfigCategory().increasedMaxHealth.get(), AttributeModifier.Operation.MULTIPLY_BASE );
        addModifier( ai, entity, Attributes.KNOCKBACK_RESISTANCE, ai.getConfigCategory().knockbackResist.get(), AttributeModifier.Operation.ADDITION );
        addModifier( ai, entity, Attributes.ARMOR, ai.getConfigCategory().armor.get(), AttributeModifier.Operation.ADDITION );
        addModifier( ai, entity, Attributes.ARMOR_TOUGHNESS, ai.getConfigCategory().armorToughness.get(), AttributeModifier.Operation.ADDITION );
        addModifier( ai, entity, Attributes.ATTACK_DAMAGE, ai.getConfigCategory().addedDamage.get(), AttributeModifier.Operation.ADDITION );
        addModifier( ai, entity, Attributes.ATTACK_DAMAGE, ai.getConfigCategory().increasedDamage.get(), AttributeModifier.Operation.MULTIPLY_BASE );
        addModifier( ai, entity, Attributes.ATTACK_KNOCKBACK, ai.getConfigCategory().knockback.get(), AttributeModifier.Operation.ADDITION );
        addModifier( ai, entity, Attributes.MOVEMENT_SPEED, ai.getConfigCategory().speed.get(), AttributeModifier.Operation.MULTIPLY_BASE );
        addModifier( ai, entity, Attributes.FLYING_SPEED, ai.getConfigCategory().speed.get(), AttributeModifier.Operation.MULTIPLY_BASE );
        
        // Apply the effects specific to the AI type
        ai.initialize( entity, aiTag );
    }
    
    /** Replaces any held shooting item with a melee weapon, in hopes that the entity will use melee attacks. */
    static void preferMelee( MobEntity entity ) {
        if( Config.ELITE_AI.GENERAL.enablePreferMelee.get() ) {
            for( Hand hand : Hand.values() ) {
                ItemStack held = entity.getItemInHand( hand );
                if( !held.isEmpty() && held.getItem() instanceof ShootableItem ) {
                    entity.setItemInHand( hand, new ItemStack( Items.GOLDEN_SWORD ) );
                }
            }
        }
    }
    
    /** Unequips the entity of any equipped item in the specified slot. */
    static void unequip( MobEntity entity, @SuppressWarnings( "SameParameterValue" ) EquipmentSlotType slot ) {
        equip( entity, ItemStack.EMPTY, 0.085, slot );
    }
    
    /** Equips the entity with an item in its natural slot, overwriting any currently equipped item. */
    static void equip( MobEntity entity, ItemStack item, double dropChance ) {
        equip( entity, item, dropChance, MobEntity.getEquipmentSlotForItem( item ) );
    }
    
    /** Equips the entity with an item in the specified slot, optionally overwriting any currently equipped item. */
    static void equip( MobEntity entity, ItemStack item, double dropChance, EquipmentSlotType slot ) {
        if( dropChance >= 0.0 && (Config.ELITE_AI.GENERAL.enableEquipmentReplace.get() || !entity.hasItemInSlot( slot )) ) {
            entity.setItemSlot( slot, item );
            entity.setDropChance( slot, (float) dropChance );
        }
    }
    
    /** Adds a custom attribute modifier to the entity. */
    private static void addModifier( EliteAIType ai, MobEntity entity, Attribute attribute, double value, AttributeModifier.Operation operation ) {
        if( Config.ELITE_AI.GENERAL.enableAttributeMods.get() && value != 0.0 ) {
            ModifiableAttributeInstance attributeInstance = entity.getAttribute( attribute );
            if( attributeInstance != null ) {
                attributeInstance.addPermanentModifier(
                        new AttributeModifier( SpecialAI.MOD_ID + ":" + ai.getKey() + " spawn bonus", value, operation ) );
            }
        }
    }
    
    /** Adds a custom attribute modifier to the item stack that only applies while in an appropriate equipment slot. */
    static void addModifier( EliteAIType ai, ItemStack stack, Attribute attribute, double value, AttributeModifier.Operation operation ) {
        if( value != 0.0 ) {
            stack.addAttributeModifier( attribute,
                    new AttributeModifier( SpecialAI.MOD_ID + ":" + ai.getKey() + " item bonus", value, operation ),
                    MobEntity.getEquipmentSlotForItem( stack ) );
        }
    }
}