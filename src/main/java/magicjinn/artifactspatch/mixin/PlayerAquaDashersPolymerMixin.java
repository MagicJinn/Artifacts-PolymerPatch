package magicjinn.artifactspatch.mixin;

import magicjinn.artifactspatch.poly.AquaDashersClientBarrierSupport;
import magicjinn.artifactspatch.poly.CloudInABottlePolymerSupport;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public class PlayerAquaDashersPolymerMixin {
	// POLYMER WORKAROUND: per-tick Polymer client ability shims (aqua dashers footing, cloud double jump).
	@Inject(method = "tick", at = @At("TAIL"))
	private void artifactsPatch$polymerAbilityTicks(CallbackInfo ci) {
		ServerPlayer player = (ServerPlayer) (Object) this;
		AquaDashersClientBarrierSupport.tick(player);
		CloudInABottlePolymerSupport.tick(player);
	}
}
