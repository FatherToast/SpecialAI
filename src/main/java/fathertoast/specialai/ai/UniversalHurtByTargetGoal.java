package fathertoast.specialai.ai;

import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.Iterator;

/**
 * A copy-paste of {@link net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal} with
 * a less strict goal owner requirement. Namely, the owner only needs to inherit {@link Mob},
 * instead of {@link PathfinderMob}.
 */
public class UniversalHurtByTargetGoal extends TargetGoal {
    
    /** The Y-range to use when making an AABB for scanning for nearby mobs to alert. */
    private static final int ALERT_RANGE_Y = 10;
    
    /** If true, the goal owner will */
    private boolean alertSameType;
    /**
     * The timestamp of when the goal owner was last hurt by a mob,
     * assigned when this goal starts running.
     */
    private int timestamp;
    /** An array of classes of entities that the goal owner should ignore damage from. */
    private final Class<?>[] toIgnoreDamage;
    
    /**
     * Optional array of classes of entities that should not care about alerts.
     * When this field is null, only entities of the same type as the goal owner will be alerted
     */
    @Nullable
    private Class<?>[] toIgnoreAlert;
    
    
    /**
     * @param owner              The goal owner.
     * @param toIgnoreDamageFrom Entity classes to not aggro on if received damage from.
     */
    public UniversalHurtByTargetGoal( Mob owner, Class<?>... toIgnoreDamageFrom ) {
        super( owner, true );
        toIgnoreDamage = toIgnoreDamageFrom;
        setFlags( EnumSet.of( Goal.Flag.TARGET ) );
    }
    
    /**
     * Enables alerting others of the same type for this goal.
     *
     * @param toAlert Optional classes of entities that should ignore the goal owner's alerting.
     */
    public void setAlertOthers( Class<?>... toAlert ) {
        alertSameType = true;
        toIgnoreAlert = toAlert;
    }
    
    /**
     * @return True if this goal can start executing.
     * This is the first check that is run BEFORE
     * the goal starts executing.
     */
    @Override
    public boolean canUse() {
        final int timeLastHurtBy = mob.getLastHurtByMobTimestamp();
        final LivingEntity lastHurtBy = mob.getLastHurtByMob();
        
        if( timeLastHurtBy != timestamp && lastHurtBy != null ) {
            if( lastHurtBy.getType() == EntityType.PLAYER && mob.level().getGameRules().getBoolean( GameRules.RULE_UNIVERSAL_ANGER ) ) {
                return false;
            }
            else {
                for( Class<?> ignored : toIgnoreDamage ) {
                    if( ignored.isAssignableFrom( lastHurtBy.getClass() ) ) {
                        return false;
                    }
                }
                // Check using HurtByTargetGoal's target conditions, in case it
                // has been modified by other mods.
                return canAttack( lastHurtBy, HurtByTargetGoal.HURT_BY_TARGETING );
            }
        }
        else {
            return false;
        }
    }
    
    /** Called when this goal starts executing. */
    @Override
    public void start() {
        mob.setTarget( mob.getLastHurtByMob() );
        targetMob = mob.getTarget();
        timestamp = mob.getLastHurtByMobTimestamp();
        unseenMemoryTicks = 300;
        
        if( alertSameType ) {
            alertOthers();
        }
        super.start();
    }
    
    /**
     * Alerts other nearby mobs of the same type as the owner,
     * setting their attack target to the owner's target.
     */
    protected void alertOthers() {
        final double followDist = getFollowDistance();
        final AABB box = AABB.unitCubeFromLowerCorner( mob.position() ).inflate( followDist, ALERT_RANGE_Y, followDist );
        final Iterator<? extends Mob> nearbyMobs = mob.level().getEntitiesOfClass( mob.getClass(), box, EntitySelector.NO_SPECTATORS ).iterator();
        
        // Loop through nearby mobs
        while( true ) {
            Mob toAlert;
            
            while( true ) {
                if( !nearbyMobs.hasNext() ) {
                    return;
                }
                toAlert = nearbyMobs.next();
                
                // Check if the mob to alert even should be alerted
                // noinspection ConstantConditions
                if( mob != toAlert && toAlert.getTarget() == null
                        && (!(mob instanceof TamableAnimal) || ((TamableAnimal) mob).getOwner() == ((TamableAnimal) toAlert).getOwner())
                        && !toAlert.isAlliedTo( mob.getLastHurtByMob() ) ) {
                    // If no exceptions are specified, skip further checks and alert
                    if( toIgnoreAlert == null ) {
                        break;
                    }
                    boolean skip = false;
                    
                    // Check if the mob to alert should ignore the alert
                    for( Class<?> clazz : toIgnoreAlert ) {
                        if( toAlert.getClass() == clazz ) {
                            skip = true;
                            break;
                        }
                    }
                    if( !skip ) {
                        break;
                    }
                }
            }
            toAlert.setTarget( mob.getLastHurtByMob() );
        }
    }
}
