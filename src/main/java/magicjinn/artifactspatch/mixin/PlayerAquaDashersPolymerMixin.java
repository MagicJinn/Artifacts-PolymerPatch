package magicjinn.artifactspatch.mixin;

import magicjinn.artifactspatch.poly.AquaDashersClientBarrierSupport;
import magicjinn.artifactspatch.poly.PolymerClientChecks;
import magicjinn.artifactspatch.poly.TogglePolymerSupport;
import magicjinn.artifactspatch.poly.UniversalAttractorPolymerSupport;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public class PlayerAquaDashersPolymerMixin {
	@Inject(method = "tick", at = @At("HEAD"))
	private void artifactsPatch$enableToggleTrinketsBeforeArtifactsTick(CallbackInfo ci) {
		ServerPlayer player = (ServerPlayer) (Object) this;
		if (PolymerClientChecks.lacksArtifactsClient(player)) {
			TogglePolymerSupport.enableEquippedToggles(player);
		}
	}

	// POLYMER WORKAROUND: Client-only waterlogged barrier footing while aqua-dashers water sprint (see AquaDashersClientBarrierSupport).
	@Inject(method = "tick", at = @At("TAIL"))
	private void artifactsPatch$polymerGameplayTick(CallbackInfo ci) {
		ServerPlayer player = (ServerPlayer) (Object) this;
		UniversalAttractorPolymerSupport.tick(player);
		AquaDashersClientBarrierSupport.tick(player);
	}
}
