package fathertoast.specialai.ai;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.merchant.villager.AbstractVillagerEntity;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.passive.fish.AbstractFishEntity;
import net.minecraft.entity.passive.fish.SalmonEntity;
import net.minecraft.entity.passive.horse.AbstractHorseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;

import javax.annotation.Nullable;

/**
 * This AI is based on its super class, the melee attack goal, but it is modified to allow normally passive mobs
 * (i.e., mobs with no attack damage attribute) to use it without crashing the game.
 * <p>
 * In addition, it will attempt to substitute path navigators in an attempt to get the pathfinding working on entities
 * that are not set up to use normal pathfinding. (This is a work in progress.)
 */
public class AnimalMeleeAttackGoal extends MeleeAttackGoal {
    
    /** @return Returns the pathfinding speed multiplier for the entity while attacking. This is not used for swimming entities. */
    public static double attackingMoveSpeed( MobEntity entity ) {
        if( entity instanceof AbstractVillagerEntity ) {
            return 0.7;
        }
        if( entity instanceof RabbitEntity ) {
            return 3.3;
        }
        if( entity instanceof ChickenEntity || entity instanceof AbstractFishEntity ) {
            return 1.6;
        }
        if( entity instanceof AnimalEntity ) {
            return 1.3;
        }
        return 1.0;
    }
    
    /** @return Returns the fallback attack damage attribute for entities that do not have the attribute registered. */
    public static float defaultAttackDamage( MobEntity entity ) {
        // Note: Default attack damage is 2.0 and the units are half-hearts, held item damage is added on to this
        // Most animals have 3.0, wolves have 4.0, and large animals have 6.0
        if( entity instanceof SheepEntity || entity instanceof RabbitEntity || entity instanceof SalmonEntity ) {
            return 2.0F;
        }
        if( entity instanceof ChickenEntity || entity instanceof AbstractFishEntity ) {
            return 1.0F;
        }
        if( entity instanceof CowEntity || entity instanceof AbstractHorseEntity ) {
            return 4.0F;
        }
        return 3.0F;
    }
    
    /** @return Returns the fallback attack knockback attribute for entities that do not have the attribute registered. */
    public static float defaultAttackKnockback( MobEntity entity ) {
        // Note: Default attack knockback is 0.0 and the max is 5.0, level of knockback enchant on held item is added on to this
        // Most mobs have 0.0, hoglins/zoglins have 1.0 (but also 'throw' on hit), and ravagers have 1.5
        if( entity instanceof PigEntity || entity instanceof CowEntity ) {
            return 1.5F;
        }
        return 0.0F;
    }
    
    /**
     * @param entity The owner of this AI.
     * @param memory Whether the owner needs line of sight to follow its target.
     */
    public AnimalMeleeAttackGoal( CreatureEntity entity, boolean memory ) {
        super( entity, attackingMoveSpeed( entity ), memory );
    }
    
    /**
     * Called every AI tick while the goal is active to check if the target can be attacked, and performs the attack if able.
     *
     * @param target      The entity's current target. Should not be null, but I don't trust it enough.
     * @param distanceSqr Distance squared from the entity to its target.
     */
    @Override
    protected void checkAndPerformAttack( @Nullable LivingEntity target, double distanceSqr ) {
        if( target != null && distanceSqr <= getAttackReachSqr( target ) && getTicksUntilNextAttack() <= 0 ) {
            resetAttackCooldown();
            mob.swing( Hand.MAIN_HAND );
            doHurtTarget( mob, target );
        }
    }
    
    /**
     * An external implementation of a melee attack.
     *
     * @param target The target to attack.
     * @see MobEntity#doHurtTarget(Entity)
     */
    public static void doHurtTarget( MobEntity mob, Entity target ) {
        // Try to perform the attack organically
        try {
            mob.doHurtTarget( target );
            return;
        }
        catch( Exception ex ) {
            // Most likely failed due to missing attack damage/knockback attributes
        }
        
        // Get actual attributes if possible
        float damage;
        try {
            damage = (float) mob.getAttributeValue( Attributes.ATTACK_DAMAGE );
        }
        catch( Exception ex ) {
            // Failed to get attack damage, pick our own
            damage = defaultAttackDamage( mob );
        }
        float knockback;
        try {
            knockback = (float) mob.getAttributeValue( Attributes.ATTACK_KNOCKBACK );
        }
        catch( Exception ex ) {
            // Failed to get attack knockback, pick our own
            knockback = defaultAttackKnockback( mob );
        }
        
        // Modify based on enchantments and active effects
        if( target instanceof LivingEntity ) {
            damage += EnchantmentHelper.getDamageBonus( mob.getMainHandItem(), ((LivingEntity) target).getMobType() );
            knockback += (float) EnchantmentHelper.getKnockbackBonus( mob );
        }
        final int fire = EnchantmentHelper.getFireAspect( mob );
        if( fire > 0 ) {
            target.setSecondsOnFire( fire * 4 );
        }
        
        // Actually perform the attack
        if( target.hurt( DamageSource.mobAttack( mob ), damage ) ) {
            // Apply knockback
            if( knockback > 0.0F && target instanceof LivingEntity ) {
                final float yRot = (float) Math.toRadians( mob.yRot );
                ((LivingEntity) target).knockback( knockback * 0.5F, MathHelper.sin( yRot ), -MathHelper.cos( yRot ) );
                mob.setDeltaMovement( mob.getDeltaMovement().multiply( 0.6, 1.0, 0.6 ) );
            }
            // Handle shield
            if( target instanceof PlayerEntity ) {
                PlayerEntity playerentity = (PlayerEntity) target;
                maybeDisableShield( mob, playerentity, mob.getMainHandItem(), playerentity.isUsingItem() ? playerentity.getUseItem() : ItemStack.EMPTY );
            }
            
            final float effectiveDifficulty = mob.level.getCurrentDifficultyAt( mob.blockPosition() ).getEffectiveDifficulty();
            if( mob.getMainHandItem().isEmpty() && mob.isOnFire() && mob.getRandom().nextFloat() < effectiveDifficulty * 0.3F ) {
                target.setSecondsOnFire( 2 * (int) effectiveDifficulty );
            }
            
            mob.doEnchantDamageEffects( mob, target );
            mob.setLastHurtMob( target );
        }
    }
    
    /**
     * An external implementation of the shield disable event.
     * <p>
     * See also: MobEntity#maybeDisableShield(PlayerEntity, ItemStack, ItemStack)
     *
     * @param target The target to attack.
     */
    private static void maybeDisableShield( MobEntity mob, PlayerEntity target, ItemStack weapon, ItemStack possibleShield ) {
        if( !weapon.isEmpty() && !possibleShield.isEmpty() && weapon.getItem() instanceof AxeItem && possibleShield.getItem() == Items.SHIELD ) {
            final float disableChance = 0.25F + (float) EnchantmentHelper.getBlockEfficiency( mob ) * 0.05F;
            if( mob.getRandom().nextFloat() < disableChance ) {
                target.getCooldowns().addCooldown( Items.SHIELD, 100 );
                mob.level.broadcastEntityEvent( target, (byte) 30 );
            }
        }
    }
    
    //    /** A speed modifier to use because swimmers don't use the pathfinding speed multiplier. */
    //    private static final AttributeModifier SPEED_MODIFIER_ATTACKING = new AttributeModifier(
    //            UUID.fromString( "70457F3F-5346-4AC6-AE99-B2549BBC6170" ),
    //            "Attacking speed boost", 0.0, AttributeModifier.Operation.MULTIPLY_TOTAL );
    
    //    /** Applies the speed modifier. Used instead of the pathing speed multiplier for swimmers. */
    //    private void applySpeedModifier() {
    //        ModifiableAttributeInstance attribute = mob.getAttribute( Attributes.MOVEMENT_SPEED );
    //        if( attribute != null && !attribute.hasModifier( SPEED_MODIFIER_ATTACKING ) ) {
    //            attribute.addTransientModifier( SPEED_MODIFIER_ATTACKING );
    //        }
    //    }
    //
    //    /** Removes the speed modifier. Used instead of the pathing speed multiplier for swimmers. */
    //    private void removeSpeedModifier() {
    //        ModifiableAttributeInstance attribute = mob.getAttribute( Attributes.MOVEMENT_SPEED );
    //        if( attribute != null ) {
    //            attribute.removeModifier( SPEED_MODIFIER_ATTACKING );
    //        }
    //    }
}