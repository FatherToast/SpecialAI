package fathertoast.specialai.ai.griefing;

import fathertoast.specialai.config.Config;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.item.Food;
import net.minecraft.item.ItemStack;
import net.minecraft.particles.ItemParticleData;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.ForgeEventFactory;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;

/**
 * This AI causes the entity to eat dropped items that could be used to breed it, possibly healing the entity based on config options.
 */
public class EatBreedingItemGoal extends Goal {
    /** Selects entities that are dropped items. */
    private static final Predicate<Entity> ITEM_SELECTOR = entity -> entity instanceof ItemEntity;
    
    /** The owner of this AI. */
    protected final AnimalEntity mob;
    
    /** The item this entity wants to eat. */
    private ItemEntity target;
    /** Ticks until the entity will search for more food. */
    private int checkTime;
    /** The cooldown in ticks between each time the entity eats one item out of the stack. */
    private int cooldown;
    /** Ticks until the entity gives up. */
    private int giveUpDelay;
    
    /**
     * @param entity The owner of this AI.
     */
    public EatBreedingItemGoal( AnimalEntity entity ) {
        mob = entity;
        setFlags( EnumSet.of( Flag.MOVE, Flag.LOOK ) );
    }
    
    /** @return Returns true if this AI can be activated. */
    @Override
    public boolean canUse() {
        if( !mob.isPassenger() && ForgeEventFactory.getMobGriefingEvent( mob.level, mob ) && ++checkTime > 30 ) {
            checkTime = 0;
            return findNearbyFood();
        }
        return false;
    }
    
    /** @return Called each update while active and returns true if this AI can remain active. */
    @Override
    public boolean canContinueToUse() {
        return !mob.isPassenger() && target != null && target.isAlive() && mob.isFood( target.getItem() ) && ++giveUpDelay <= 400;
    }
    
    /** Called when this AI is activated. */
    @Override
    public void start() {
        cooldown = 0;
        mob.getNavigation().moveTo( target.position().x, target.position().y, target.position().z, 1.0 );
    }
    
    /** Called each tick while this AI is active. */
    @Override
    public void tick() {
        if( target == null ) return; // Shouldn't happen, but just in case
        mob.getLookControl().setLookAt( target, 30.0F, 30.0F );
        
        List<Entity> list = mob.level.getEntities( mob, mob.getBoundingBox().inflate( 0.2, 0.0, 0.2 ) );
        if( list.contains( target ) ) {
            // Tick cooldown
            if ( cooldown > 0 ) {
                --cooldown;
            }
            else {
                // Eat one item out of the stack
                ItemStack item = target.getItem();
                if (Config.GENERAL.ANIMALS.eatingHeals.get()) {
                    final Food food = item.getItem().getFoodProperties();
                    final float healAmount = Math.max(food == null ? 0.0F : food.getNutrition(), 1.0F);
                    mob.heal(healAmount);
                }
                triggerEatingEffects(item);
                mob.getNavigation().stop();

                item.shrink(1);
                if (item.isEmpty()) {
                    target.remove();
                }
                cooldown = Config.GENERAL.ANIMALS.eatingCooldown.get();
            }
        }
        else {
            // Small magnet effect
            double reach = Config.GENERAL.ANIMALS.eatingReach.get();
            if( reach > 0.0 && mob.distanceToSqr( target ) < reach * reach ) {
                target.setDeltaMovement( mob.position().subtract( target.position() )
                        .normalize().scale( 0.05 ).add( 0.0, 0.04, 0.0 ) );
            }
            
            if( mob.getNavigation().isDone() ) {
                mob.getNavigation().moveTo( target.position().x, target.position().y, target.position().z, 1.0 );
            }
        }
    }
    
    /** Plays audio-visual effects when a food item is eaten. */
    private void triggerEatingEffects( ItemStack item ) {
        // Spawn particles
        for( int i = 0; i < 16; i++ ) {
            final Vector3d velocity = new Vector3d(
                    (mob.getRandom().nextFloat() - 0.5) * 0.1,
                    Math.random() * 0.1 + 0.1,
                    0.0 )
                    .xRot( (float) Math.toRadians( -mob.xRot ) ).yRot( (float) Math.toRadians( -mob.yRot ) )
                    .add( 0.0, 0.05, 0.0 );
            
            final Vector3d position = new Vector3d(
                    ((double) mob.getRandom().nextFloat() - 0.5) * 0.3,
                    -mob.getRandom().nextFloat() * 0.6 - 0.3,
                    0.6 )
                    .xRot( (float) Math.toRadians( -mob.xRot ) ).yRot( (float) Math.toRadians( -mob.yRot ) )
                    .add( mob.getX(), mob.getEyeY(), mob.getZ() );
            
            if( mob.level instanceof ServerWorld ) {
                ((ServerWorld) mob.level).sendParticles( new ItemParticleData( ParticleTypes.ITEM, item ),
                        position.x, position.y, position.z, 1,
                        velocity.x, velocity.y, velocity.z, 0.0 );
            }
        }
        // Play sound
        mob.playSound( mob.getEatingSound( item ), 0.5F + 0.5F * (float) mob.getRandom().nextInt( 2 ),
                (mob.getRandom().nextFloat() - mob.getRandom().nextFloat()) * 0.2F + 1.0F );
    }
    
    /** Called when this AI is deactivated. */
    @Override
    public void stop() {
        giveUpDelay = 0;
        mob.getNavigation().stop();
        target = null;
    }
    
    /** @return Searches for a nearby food item and targets it. Returns true if a target is found. */
    private boolean findNearbyFood() {
        List<Entity> list = mob.level.getEntities( mob, mob.getBoundingBox().inflate( 16.0, 8.0, 16.0 ), ITEM_SELECTOR );
        Collections.shuffle( list );
        for( Entity entity : list ) {
            ItemStack item = ((ItemEntity) entity).getItem();
            if( !item.isEmpty() && mob.isFood( item ) ) {
                target = (ItemEntity) entity;
                return true;
            }
        }
        return false;
    }
}