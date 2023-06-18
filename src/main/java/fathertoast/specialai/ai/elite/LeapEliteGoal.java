package fathertoast.specialai.ai.elite;

import fathertoast.specialai.config.Config;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.vector.Vector3d;

import java.util.EnumSet;

/**
 * This AI causes an entity to make short, quick jumps at their target, similar to a spider.
 */
public class LeapEliteGoal extends AbstractEliteGoal {
    /** Ticks until the AI can activate again. */
    private int cooldownTimer;
    
    LeapEliteGoal( MobEntity entity, CompoundNBT aiTag ) {
        super( entity, aiTag );
        setFlags( EnumSet.of( Flag.JUMP ) );
    }
    
    /** @return Returns true if this AI can be activated. */
    @Override
    public boolean canUse() {
        if( --cooldownTimer > 0 || !mob.isOnGround() || mob.isPassenger() || mob.getRandom().nextInt( 5 ) != 0 )
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
        final Vector3d jumpXZ = new Vector3d( target.getX() - mob.getX(), 0.0, target.getZ() - mob.getZ() )
                .normalize().scale( Config.ELITE_AI.LEAP.jumpSpeedForward.get() ).add( mob.getDeltaMovement().scale( 0.2 ) );
        mob.setDeltaMovement( jumpXZ.x, Config.ELITE_AI.LEAP.jumpSpeedUpward.get(), jumpXZ.z );
        
        // Start the cooldown
        cooldownTimer = Config.ELITE_AI.LEAP.cooldown.next( mob.getRandom() );
    }
    
    /** @return Called each update while active and returns true if this AI can remain active. */
    @Override
    public boolean canContinueToUse() { return false; }
}