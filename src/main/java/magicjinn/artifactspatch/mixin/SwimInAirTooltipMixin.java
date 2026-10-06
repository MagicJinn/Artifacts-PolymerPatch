package magicjinn.artifactspatch.mixin;

import artifacts.component.ability.EquipmentAbility;
import artifacts.component.ability.SwimInAir;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SwimInAir.class)
public abstract class SwimInAirTooltipMixin {
	// POLYMER WORKAROUND: Artifacts' helium flamingo tooltip calls ModKeyMappings.getHeliumFlamingoKey(),
	// which touches net.minecraft.client.KeyMapping on a dedicated server. Polymer builds item tooltips on
	// the server for vanilla clients, so we omit the key-binding line and keep the swimming ability text.
	@Inject(method = "addToTooltip", at = @At("HEAD"), cancellable = true)
	private void artifactsPatch$serverSafeSwimTooltip(EquipmentAbility.TooltipWriter writer, CallbackInfo ci) {
		if (FabricLoader.getInstance().getEnvironmentType() != EnvType.SERVER) {
			return;
		}
		writer.add("swimming");
		ci.cancel();
	}
}
