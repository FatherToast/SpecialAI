package fathertoast.specialai.ai;

import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

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
    public static double attackingMoveSpeed( Mob entity ) {
        if( entity instanceof AbstractVillager ) {
            return 0.7;
        }
        if( entity instanceof Rabbit ) {
            return 3.3;
        }
        if( entity instanceof Chicken || entity instanceof AbstractFish ) {
            return 1.6;
        }
        if ( entity instanceof Frog ) {
            return 1.5;
        }
        if( entity instanceof Animal ) {
            return 1.3;
        }
        return 1.0;
    }
    
    /** @return Returns the fallback attack damage attribute for entities that do not have the attribute registered. */
    public static float defaultAttackDamage( Mob entity ) {
        // Note: Default attack damage is 2.0 and the units are half-hearts, held item damage is added on to this
        // Most animals have 3.0, wolves have 4.0, and large animals have 6.0
        if( entity instanceof Sheep || entity instanceof Rabbit || entity instanceof Salmon ) {
            return 2.0F;
        }
        if( entity instanceof Chicken || entity instanceof AbstractFish ) {
            return 1.0F;
        }
        if( entity instanceof Cow || entity instanceof AbstractHorse ) {
            return 4.0F;
        }
        if ( entity instanceof Sniffer ) {
            return 6.0F;
        }
        return 3.0F;
    }
    
    /** @return Returns the fallback attack knockback attribute for entities that do not have the attribute registered. */
    public static float defaultAttackKnockback( Mob entity ) {
        // Note: Default attack knockback is 0.0 and the max is 5.0, level of knockback enchant on held item is added on to this
        // Most mobs have 0.0, hoglins/zoglins have 1.0 (but also 'throw' on hit), and ravagers have 1.5
        if( entity instanceof Pig || entity instanceof Cow ) {
            return 1.5F;
        }
        return 0.0F;
    }
    
    /**
     * @param entity The owner of this AI.
     * @param memory Whether the owner needs line of sight to follow its target.
     */
    public AnimalMeleeAttackGoal( PathfinderMob entity, boolean memory ) {
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
            mob.swing( InteractionHand.MAIN_HAND );
            doHurtTarget( mob, target );
        }
    }
    
    /**
     * An external implementation of a melee attack.
     *
     * @param target The target to attack.
     * @see Mob#doHurtTarget(Entity)
     */
    public static void doHurtTarget( Mob mob, Entity target ) {
        // Try to perform the attack organically
        try {
            mob.doHurtTarget(target);
            return;
        }
        catch (Exception ex) {
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
        if( target.hurt( mob.level().damageSources().mobAttack( mob ), damage ) ) {
            // Apply knockback
            if( knockback > 0.0F && target instanceof LivingEntity ) {
                final float yRot = (float) Math.toRadians( mob.getYRot() );
                ((LivingEntity) target).knockback( knockback * 0.5F, Mth.sin( yRot ), -Mth.cos( yRot ) );
                mob.setDeltaMovement( mob.getDeltaMovement().multiply( 0.6, 1.0, 0.6 ) );
            }
            // Handle shield
            if(target instanceof Player player ) {
                maybeDisableShield( mob, player, mob.getMainHandItem(), player.isUsingItem() ? player.getUseItem() : ItemStack.EMPTY );
            }
            
            final float effectiveDifficulty = mob.level().getCurrentDifficultyAt( mob.blockPosition() ).getEffectiveDifficulty();
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
    private static void maybeDisableShield( Mob mob, Player target, ItemStack weapon, ItemStack possibleShield ) {
        if( !weapon.isEmpty() && !possibleShield.isEmpty() && weapon.getItem() instanceof AxeItem && possibleShield.getItem() == Items.SHIELD ) {
            final float disableChance = 0.25F + (float) EnchantmentHelper.getBlockEfficiency( mob ) * 0.05F;
            if( mob.getRandom().nextFloat() < disableChance ) {
                target.getCooldowns().addCooldown( Items.SHIELD, 100 );
                mob.level().broadcastEntityEvent( target, (byte) 30 );
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