package magicjinn.artifactspatch.mixin;

import dev.yumi.mc.core.api.ModContainer;
import eu.pb4.trinkets.impl.TrinketsMain;
import magicjinn.artifactspatch.poly.TrinketsRegistrySync;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TrinketsMain.class)
public class TrinketsMainMixin {
	@Inject(method = "onInitialize", at = @At("RETURN"))
	private void artifacts$markTrinketsRegistriesServerOnly(ModContainer mod, CallbackInfo ci) {
		TrinketsRegistrySync.register();
	}
}
