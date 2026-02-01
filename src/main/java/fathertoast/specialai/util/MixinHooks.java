package fathertoast.specialai.util;

import fathertoast.specialai.ai.IVehicleControlOverride;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.vehicle.Boat;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

public class MixinHooks {

    public static void onUpdateControlFlags(Mob mob, CallbackInfo ci ) {
        List<WrappedGoal> runningGoals = mob.goalSelector.getRunningGoals().toList();

        for ( WrappedGoal goal : runningGoals ) {
            if ( goal.getGoal() instanceof IVehicleControlOverride ) {
                mob.goalSelector.setControlFlag( Goal.Flag.LOOK, true );
                mob.goalSelector.setControlFlag( Goal.Flag.MOVE, true );
                mob.goalSelector.setControlFlag( Goal.Flag.JUMP, !( mob.getVehicle() instanceof Boat ) );
                ci.cancel();
                return;
            }
        }
    }

    public static void onSetVillagerData( Villager villager, VillagerData data ) {
        if ( villager.level().isClientSide ) return;

        VillagerNameHelper.setVillagerName( villager.getRandom(), villager, data );
    }
}
