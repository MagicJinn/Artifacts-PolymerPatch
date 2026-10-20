package magicjinn.artifactspolymer.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import artifacts.component.SwimData;
import magicjinn.artifactspolymer.polymer.CharmOfSinkingPolymerPatch;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

@Mixin(SwimData.class)
public class SwimDataMixin {
    /**
     * POLYMER WORKAROUND: Artifacts drains Helium Flamingo charge when
     * {@code !isUnderWater() || SINKING}. Charm of Sinking's ExpandAbility FAIL
     * makes {@code isUnderWater()} false while submerged, and the SINKING clause
     * drains even when eyes are in water. Polymer clients may activate underwater
     * (intended) but must not burn charge there.
     */
    @Inject(method = "shouldDepleteSwimFlyingCharge", at = @At("HEAD"), cancellable = true)
    private void artifactsPolymer$noDrainInWorldWater(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (player instanceof ServerPlayer serverPlayer
                && CharmOfSinkingPolymerPatch.inWorldWater(serverPlayer)) {
            cir.setReturnValue(false);
        }
    }
}
