package fathertoast.specialai.mixin;

import fathertoast.specialai.util.MixinHooks;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Targeting;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class MobMixin extends LivingEntity implements Targeting {

    protected MobMixin(EntityType<? extends LivingEntity> type, Level level ) {
        super( type, level );
    }

    @Inject(
            method = "updateControlFlags",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/ai/goal/GoalSelector;setControlFlag(Lnet/minecraft/world/entity/ai/goal/Goal$Flag;Z)V",
                    ordinal = 0
            ),
            cancellable = true
    )

    public void onUpdateControlFlags( CallbackInfo ci ) {
        MixinHooks.onUpdateControlFlags( (Mob)(Object) this, ci );
    }
}
