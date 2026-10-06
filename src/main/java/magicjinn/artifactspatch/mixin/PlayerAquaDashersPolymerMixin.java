package magicjinn.artifactspatch.mixin;

import magicjinn.artifactspatch.poly.AquaDashersClientBarrierSupport;
import magicjinn.artifactspatch.poly.CloudInABottlePolymerSupport;
import magicjinn.artifactspatch.poly.SnowshoesClientSnowSupport;
import magicjinn.artifactspatch.poly.SteadfastSpikesClientIceSupport;
import magicjinn.artifactspatch.poly.StriderShoesClientMagmaSupport;
import magicjinn.artifactspatch.poly.SwimSpeedPolymerSupport;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public class PlayerAquaDashersPolymerMixin {
	// POLYMER WORKAROUND: per-tick Polymer client ability shims (aqua dashers / strider shoes / snowshoes / steadfast spikes footing, cloud double jump, flippers).
	@Inject(method = "tick", at = @At("TAIL"))
	private void artifactsPatch$polymerAbilityTicks(CallbackInfo ci) {
		ServerPlayer player = (ServerPlayer) (Object) this;
		AquaDashersClientBarrierSupport.tick(player);
		StriderShoesClientMagmaSupport.tick(player);
		SnowshoesClientSnowSupport.tick(player);
		SteadfastSpikesClientIceSupport.tick(player);
		CloudInABottlePolymerSupport.tick(player);
		SwimSpeedPolymerSupport.tick(player);
	}
}
