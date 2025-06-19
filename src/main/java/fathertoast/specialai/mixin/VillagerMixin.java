package fathertoast.specialai.mixin;

import fathertoast.specialai.util.MixinHooks;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin( Villager.class )
public abstract class VillagerMixin {

    @Inject(
            method = "setVillagerData",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/syncher/SynchedEntityData;set(Lnet/minecraft/network/syncher/EntityDataAccessor;Ljava/lang/Object;)V"
            )
    )
    public void onSetVillagerData( VillagerData data, CallbackInfo ci ) {
        MixinHooks.onSetVillagerData( (Villager)(Object) this, data );
    }
}
