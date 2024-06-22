package fathertoast.specialai.ai.elite;

import fathertoast.specialai.config.Config;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * This AI causes an entity to make long, high jumps at their target when at range.
 */
public class JumpEliteGoal extends AbstractEliteGoal {
    /** Ticks until the AI can activate again. */
    private int cooldownTimer;
    
    JumpEliteGoal( Mob entity, CompoundTag aiTag ) {
        super( entity, aiTag );
        setFlags( EnumSet.of( Flag.JUMP ) );
    }
    
    /** @return Returns true if this AI can be activated. */
    @Override
    public boolean canUse() {
        if( --cooldownTimer > 0 || !mob.onGround() || mob.isPassenger() || mob.getRandom().nextInt( 10 ) != 0 )
            return false;
        
        final LivingEntity target = mob.getTarget();
        if( target != null ) {
            final double distanceSqr = mob.distanceToSqr( target );
            return distanceSqr <= Config.ELITE_AI.JUMP.rangeSqrMax.get() && distanceSqr >= Config.ELITE_AI.JUMP.rangeSqrMin.get();
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
                .normalize().scale( Config.ELITE_AI.JUMP.jumpSpeedForward.get() ).add( mob.getDeltaMovement().scale( 0.2 ) );
        mob.setDeltaMovement( jumpXZ.x, Config.ELITE_AI.JUMP.jumpSpeedUpward.get(), jumpXZ.z );
        
        // Start the cooldown (this won't tick down until the entity has landed)
        cooldownTimer = Config.ELITE_AI.JUMP.cooldown.next( mob.getRandom() );
    }
    
    /** @return Called each update while active and returns true if this AI can remain active. */
    @Override
    public boolean canContinueToUse() {
        // Keep the AI active so we can negate fall damage
        return !mob.onGround() && !mob.isPassenger() && !mob.isInWaterOrBubble() && !mob.isInLava();
    }
    
    /** @return Returns true if this AI can be interrupted by a higher priority conflicting task. */
    @Override
    public boolean isInterruptable() { return false; }
    
    /** Called each tick while this AI is active. */
    @Override
    public void tick() { mob.fallDistance = 0.0F; }
}