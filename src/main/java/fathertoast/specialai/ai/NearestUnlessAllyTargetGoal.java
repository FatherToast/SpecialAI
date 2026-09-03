package fathertoast.specialai.ai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.scores.Team;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Functions much like {@link net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal}, except
 * this goal checks if candidate targets are considered allies before targeting.
 */
public class NearestUnlessAllyTargetGoal<T extends LivingEntity> extends NearestAttackableTargetGoal<T> {
    
    /**
     * Creates a new instance of this goal, with parameters and values
     * copied from the given {@link NearestAttackableTargetGoal} instance.
     */
    public static <T extends LivingEntity> NearestUnlessAllyTargetGoal<T> convertFrom( NearestAttackableTargetGoal<T> goal ) {
        Objects.requireNonNull( goal );
        return new NearestUnlessAllyTargetGoal<>(
                goal.mob,
                goal.targetType,
                goal.randomInterval,
                goal.mustSee,
                goal.mustReach,
                goal.targetConditions.selector
        );
    }
    
    
    /**
     * @param mob         The owner of this AI.
     * @param targetClass The class of the entity to find nearby instances of.
     * @param mustSee     True if the owner of this AI must have line of sight to the target.
     */
    public NearestUnlessAllyTargetGoal( Mob mob, Class<T> targetClass, boolean mustSee ) {
        super( mob, targetClass, mustSee );
    }
    
    /**
     * @param mob            The owner of this AI.
     * @param targetClass    The class of the entity to find nearby instances of.
     * @param mustSee        True if the owner of this AI must have line of sight to the target.
     * @param targetSelector Optional targeting conditions to use when selecting a target.
     */
    public NearestUnlessAllyTargetGoal( Mob mob, Class<T> targetClass, boolean mustSee, Predicate<LivingEntity> targetSelector ) {
        super( mob, targetClass, mustSee, targetSelector );
    }
    
    /**
     * @param mob         The owner of this AI.
     * @param targetClass The class of the entity to find nearby instances of.
     * @param mustSee     True if the owner of this AI must have line of sight to the target.
     * @param mustReach   True if the owner of this AI requires a path to the target.
     */
    public NearestUnlessAllyTargetGoal( Mob mob, Class<T> targetClass, boolean mustSee, boolean mustReach ) {
        super( mob, targetClass, mustSee, mustReach );
    }
    
    /**
     * @param mob            The owner of this AI.
     * @param targetClass    The class of the entity to find nearby instances of.
     * @param randomInterval A 1 in n chance for the AI to not run at all, effectively creating a random run delay.
     * @param mustSee        True if the owner of this AI must have line of sight to the target.
     * @param mustReach      True if the owner of this AI requires a path to the target.
     * @param targetSelector Optional targeting conditions to use when selecting a target.
     */
    public NearestUnlessAllyTargetGoal( Mob mob, Class<T> targetClass, int randomInterval,
                                        boolean mustSee, boolean mustReach, @Nullable Predicate<LivingEntity> targetSelector ) {
        super( mob, targetClass, randomInterval, mustSee, mustReach, targetSelector );
    }
    
    /**
     * Attempts to locate a nearby target based on this goal's target selection parameters.
     * If a target is found, but is considered an ally to the owner of this AI, the target is discarded.
     */
    @Override
    protected void findTarget() {
        super.findTarget();
        if( target == null ) return;
        
        Team team = target.getTeam();
        if( team != null && team.isAlliedTo( mob.getTeam() ) ) {
            target = null;
            return;
        }
        
        if( target instanceof OwnableEntity ownable ) {
            if( ownable.getOwnerUUID() != null ) {
                target = null;
            }
        }
    }
}
