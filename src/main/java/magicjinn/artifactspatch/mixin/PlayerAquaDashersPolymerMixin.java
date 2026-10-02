package magicjinn.artifactspatch.mixin;

import magicjinn.artifactspatch.poly.AquaDashersClientBarrierSupport;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public class PlayerAquaDashersPolymerMixin {
	// POLYMER WORKAROUND: Client-only waterlogged barrier footing while aqua-dashers water sprint (see AquaDashersClientBarrierSupport).
	@Inject(method = "tick", at = @At("TAIL"))
	private void artifactsPatch$clientBarrierFooting(CallbackInfo ci) {
		AquaDashersClientBarrierSupport.tick((ServerPlayer) (Object) this);
	}
}
