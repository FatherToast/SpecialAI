package fathertoast.specialai.ai.elite;


import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * Contains the most basic implementations used by all (or most) elite AI goals.
 */
public abstract class AbstractEliteGoal extends Goal {
    /** The owner of this AI. */
    protected final Mob mob;
    
    AbstractEliteGoal( Mob entity, @SuppressWarnings( "unused" ) CompoundTag aiTag ) { mob = entity; }
}