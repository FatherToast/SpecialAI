package fathertoast.specialai.ai.elite;

import fathertoast.crust.api.lib.DeferredAction;
import fathertoast.specialai.config.Config;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.function.Supplier;

/**
 * This AI causes an entity to make short, quick jumps at their target, similar to a spider.
 */
public class LeapEliteGoal extends AbstractEliteGoal {
    /** Ticks until the AI can activate again. */
    private int cooldownTimer;
    
    LeapEliteGoal( Mob entity, CompoundTag aiTag ) {
        super( entity, aiTag );
        setFlags( EnumSet.of( Flag.JUMP ) );
    }
    
    /** @return Returns true if this AI can be activated. */
    @Override
    public boolean canUse() {
        if( --cooldownTimer > 0 || !mob.onGround() || mob.isPassenger() || mob.getRandom().nextInt( 5 ) != 0 )
            return false;
        
        final LivingEntity target = mob.getTarget();
        if( target != null ) {
            final double distanceSqr = mob.distanceToSqr( target );
            return distanceSqr <= Config.ELITE_AI.LEAP.rangeSqrMax.get() && distanceSqr >= Config.ELITE_AI.LEAP.rangeSqrMin.get();
        }
        return false;
    }
    
    /** Called when this AI is activated. */
    @Override
    public void start() {
        final LivingEntity target = mob.getTarget();
        if( target == null )
            return;
        
        // Perform the jump
        mob.getLookControl().setLookAt( target, 180.0F, 0.0F );
        final Vec3 jumpXZ = new Vec3( target.getX() - mob.getX(), 0.0, target.getZ() - mob.getZ() )
                .normalize().scale( Config.ELITE_AI.LEAP.jumpSpeedForward.get() ).add( mob.getDeltaMovement().scale( 0.2 ) );
        mob.setDeltaMovement( jumpXZ.x, Config.ELITE_AI.LEAP.jumpSpeedUpward.get(), jumpXZ.z );
        
        // Try and stop pathfinders from sometimes
        // backtracking to a path node they didn't reach after leaping.
        if( !mob.getNavigation().isDone() ) {
            DeferredAction.queue( new PostLeapCheck( mob, target ) );
        }
        
        // Start the cooldown
        cooldownTimer = Config.ELITE_AI.LEAP.cooldown.next( mob.getRandom() );
    }
    
    /** @return Called each update while active and returns true if this AI can remain active. */
    @Override
    public boolean canContinueToUse() { return false; }
    
    /**
     * A post-leap checker runs for a few ticks on the server
     * after a leap has been performed. The main purpose of this check
     * is to stop pathfinding goal owners from backtracking to a node
     * that is farther away from the target than the goal owner
     * is after leaping.
     */
    public static class PostLeapCheck implements Supplier<Boolean> {
        
        /** The mob that performed a leap. */
        private final Mob leaper;
        /** The target entity that was leaped towards. */
        private final LivingEntity target;
        
        /** When this timer runs out, the check yields. */
        private int giveUpCounter = 30;
        
        
        public PostLeapCheck( Mob leaper, LivingEntity target ) {
            this.leaper = leaper;
            this.target = target;
        }
        
        @Override
        public Boolean get() {
            --giveUpCounter;
            
            // Give up early if either leaper or target
            // is dead or unloaded.
            if( !leaper.isAlive() || !target.isAlive() )
                return true;
            
            if( leaper.getNavigation().isDone() || giveUpCounter <= 0 )
                return true;
            
            if( leaper.onGround() && !leaper.getNavigation().isDone() ) {
                final Path path = leaper.getNavigation().getPath();
                
                if( path != null ) {
                    final Vec3 nodePos = path.getNextNode().asVec3();
                    
                    if( leaper.distanceToSqr( target ) < target.distanceToSqr( nodePos ) )
                        path.advance();
                }
            }
            return false;
        }
    }
}