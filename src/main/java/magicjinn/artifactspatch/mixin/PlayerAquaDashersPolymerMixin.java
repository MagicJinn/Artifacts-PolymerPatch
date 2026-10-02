package magicjinn.artifactspatch.mixin;

import magicjinn.artifactspatch.poly.AquaDashersPolymerSupport;
import magicjinn.artifactspatch.poly.PolymerClientChecks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// POLYMER WORKAROUND: Aqua dashers water sprint needs client collision feedback; spawn owner-only virtual
// interaction deck (see AquaDashersPolymerSupport) because vanilla clients lack Artifacts fluid collision.
@Mixin(Player.class)
public class PlayerAquaDashersPolymerMixin {
	@Inject(method = "tick", at = @At("TAIL"))
	private void artifactsPatch$stabilizeAquaDashersOnWater(CallbackInfo ci) {
		Player player = (Player) (Object) this;
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return;
		}
		if (!PolymerClientChecks.lacksArtifactsClient(serverPlayer)) {
			return;
		}
		AquaDashersPolymerSupport.tick(serverPlayer);
	}
}
