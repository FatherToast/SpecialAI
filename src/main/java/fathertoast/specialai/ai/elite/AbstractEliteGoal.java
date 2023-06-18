package fathertoast.specialai.ai.elite;

import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.nbt.CompoundNBT;

/**
 * Contains the most basic implementations used by all (or most) elite AI goals.
 */
public abstract class AbstractEliteGoal extends Goal {
    /** The owner of this AI. */
    protected final MobEntity mob;
    
    AbstractEliteGoal( MobEntity entity, @SuppressWarnings( "unused" ) CompoundNBT aiTag ) { mob = entity; }
}