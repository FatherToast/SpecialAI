package fathertoast.specialai.ai.elite;

import fathertoast.crust.api.lib.DeferredAction;
import fathertoast.specialai.config.Config;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Supplier;

/**
 * This AI causes an entity to steal a random item from a player, briefly turn invisible, and then just run away.
 */
public class ThiefEliteGoal extends AbstractPathingEliteGoal {
    /** The avoidance AI to be used after an item was stolen. */
    private final AvoidEntityGoal<Player> aiAvoid;
    
    ThiefEliteGoal( Mob entity, CompoundTag aiTag ) {
        super( entity, aiTag );
        if( entity instanceof PathfinderMob pathfinderMob ) {
            aiAvoid = new AvoidEntityGoal<>( pathfinderMob, Player.class, (float) Config.ELITE_AI.THIEF.avoidRange.get(),
                    Config.ELITE_AI.THIEF.avoidWalkSpeed.get(), Config.ELITE_AI.THIEF.avoidRunSpeed.get() );
        }
        else {
            aiAvoid = null;
        }
        setFlags( EnumSet.of( Flag.MOVE, Flag.LOOK ) );
    }
    
    /** @return Returns true if this AI can be activated. */
    @Override
    public boolean canUse() {
        final LivingEntity target = mob.getTarget();
        // Wants to steal an item
        if( target instanceof Player player && mob.getMainHandItem().isEmpty() ) {
            return hasItems( player );
        }
        // Wants to avoid the target
        if( aiAvoid != null && (target == null || target.getHealth() > target.getMaxHealth() * 0.3333F) ) {
            try {
                return aiAvoid.canUse();
            }
            catch( Exception ex ) {
                return false;
            }
        }
        return false;
    }
    
    /** Called when this AI is activated. */
    @Override
    public void start() {
        final LivingEntity target = mob.getTarget();
        // Try to steal an item
        if( target != null && mob.getMainHandItem().isEmpty() ) {
            startPathing( target, Config.ELITE_AI.THIEF.moveSpeed.get() );
        }
        // Try to avoid the target
        else if( aiAvoid != null ) {
            try {
                aiAvoid.start();
            }
            catch( Exception ex ) {
                mob.getNavigation().stop();
            }
        }
    }
    
    /** @return Called each update while active and returns true if this AI can remain active. */
    @Override
    public boolean canContinueToUse() {
        return mob.getNavigation().isInProgress();
    }
    
    /** @return Returns true if this AI can be interrupted by a higher priority conflicting task. */
    @Override
    public boolean isInterruptable() { return false; }
    
    /** Called each tick while this AI is active. */
    @Override
    public void tick() {
        // Trying to steal an item
        if( mob.getMainHandItem().isEmpty() ) {
            final LivingEntity target = mob.getTarget();
            if( target == null ) {
                mob.getNavigation().stop();
                return;
            }
            mob.getLookControl().setLookAt( target, 30.0F, 30.0F );
            
            if( mob.distanceToSqr( target ) <= mob.getBbWidth() * mob.getBbWidth() * 4.0F + target.getBbWidth() && mob.hasLineOfSight( target ) ) {
                // The target is in range; deal a tiny hit of damage, steal the item, and turn invisible
                target.hurt( mob.level().damageSources().mobAttack( mob ), (float) Config.ELITE_AI.THIEF.stealDamage.get() );
                mob.swing( InteractionHand.MAIN_HAND );
                if( target instanceof Player player ) {
                    final ItemStack stolen = removeRandomItem( player );
                    if( !stolen.isEmpty() ) {
                        final ItemEntity drop = new ItemEntity( mob.level(), mob.getX(), mob.getY() + 0.5, mob.getZ(), stolen );
                        drop.setPickUpDelay( 20 );
                        DeferredAction.queue( new EquipToThief( mob, drop ) );
                        mob.level().addFreshEntity( drop );
                    }
                }
                if( Config.ELITE_AI.THIEF.invisibilityDuration.get() > 0 ) {
                    mob.addEffect( new MobEffectInstance( MobEffects.INVISIBILITY, Config.ELITE_AI.THIEF.invisibilityDuration.get(), 0 ) );
                }
                mob.getNavigation().stop();
            }
            else {
                // Update pathing
                tickPathing( target, Config.ELITE_AI.THIEF.moveSpeed.get() );
            }
        }
        // Trying to avoid the target
        else if( aiAvoid != null ) {
            try {
                aiAvoid.tick();
            }
            catch( Exception ex ) {
                mob.getNavigation().stop();
            }
        }
    }
    
    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }
    
    /** Called when this AI is deactivated. */
    @Override
    public void stop() {
        if( aiAvoid != null ) {
            try {
                aiAvoid.stop();
            }
            catch( Exception ex ) {
                // Do nothing
            }
        }
    }
    
    /** Returns true if the given slot is a valid theft target. */
    private static boolean isValidSlot( int index, Player player ) {
        return Config.ELITE_AI.THIEF.validSlots.get().isValidSlot( index, player ) && !player.getInventory().getItem( index ).isEmpty();
    }
    
    /** Returns true if the player has any items in their inventory that can be stolen. */
    private static boolean hasItems( Player player ) {
        for( int i = 0; i < player.getInventory().getContainerSize(); i++ ) {
            if( isValidSlot( i, player ) ) return true;
        }
        return false;
    }
    
    /** Removes a random item stack from the player's inventory and returns it. */
    private static ItemStack removeRandomItem( Player player ) {
        // Build a list of valid inventory slot indexes
        final List<Integer> targetSlots = new ArrayList<>();
        for( int i = 0; i < player.getInventory().getContainerSize(); i++ ) {
            if( isValidSlot( i, player ) ) targetSlots.add( i );
        }
        // Pick a random slot index, remove the item from the player, and return the item removed
        if( !targetSlots.isEmpty() ) {
            return player.getInventory().removeItemNoUpdate( targetSlots.get( player.getRandom().nextInt( targetSlots.size() ) ) );
        }
        return ItemStack.EMPTY;
    }
    
    /** Allows the user to choose which inventory slots that can be stolen from through the config file. */
    @SuppressWarnings( "unused" )
    public enum ValidSlots {
        ANY( true, true, true ),
        HOTBAR_ONLY( true, false, false ),
        MAIN_ONLY( false, true, false ),
        ARMOR_ONLY( false, false, true ),
        HOTBAR_AND_MAIN( true, true, false ),
        HOTBAR_AND_ARMOR( true, false, true ),
        MAIN_AND_ARMOR( false, true, true );
        
        private final boolean TARGET_HOTBAR;
        private final boolean TARGET_MAIN;
        private final boolean TARGET_ARMOR;
        
        ValidSlots( boolean hotbar, boolean main, boolean armor ) {
            TARGET_HOTBAR = hotbar;
            TARGET_MAIN = main;
            TARGET_ARMOR = armor;
        }
        
        /** @return Returns true if the provided index is a valid slot for this targeting setup. */
        public boolean isValidSlot( int index, Player player ) {
            // In hotbar
            if( Inventory.isHotbarSlot( index ) )
                return TARGET_HOTBAR;
            // In main inventory
            if( index < player.getInventory().items.size() )
                return TARGET_MAIN;
            // In armor inventory
            if( index - player.getInventory().items.size() < player.getInventory().armor.size() )
                return TARGET_ARMOR;
            // In offhand (considered part of hotbar)
            return TARGET_HOTBAR;
        }
    }
    
    /**
     * Used to connect a stolen item to the thief that stole it, so the item can be equipped to the thief.
     * This strategy is used because changing entity equipment during the AI tick can crash the game.
     */
    private static class EquipToThief implements Supplier<Boolean> {
        /** The thief that stole the item. */
        private final Mob THIEF;
        /** The item stolen. */
        private final ItemEntity ITEM;
        
        EquipToThief( Mob entity, ItemEntity item ) {
            THIEF = entity;
            ITEM = item;
        }
        
        /** Called to finalize the item stealing process. Equips the item to the thief and destroys the dropped item. */
        @Override
        public Boolean get() {
            THIEF.setItemSlot( EquipmentSlot.MAINHAND, ITEM.getItem() );
            THIEF.setGuaranteedDrop( EquipmentSlot.MAINHAND );
            THIEF.setPersistenceRequired();
            ITEM.discard();
            return true;
        }
    }
}