package magicjinn.artifactspolymer.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import artifacts.component.ability.EquipmentAbility;
import artifacts.component.ability.SwimInAir;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;

@Mixin(SwimInAir.class)
public class SwimInAirTooltipMixin {
	/**
	 * POLYMER WORKAROUND: {@code SwimInAir.addToTooltip} calls
	 * {@code ModKeyMappings.getHeliumFlamingoKey()}, which loads client-only
	 * {@code KeyMapping$Category}. Polymer builds item tooltips on the dedicated
	 * server, so emit the same lines using the sprint key name instead.
	 */
	@Inject(method = "addToTooltip", at = @At("HEAD"), cancellable = true)
	private void artifactsPolymer$safeTooltip(EquipmentAbility.TooltipWriter writer, CallbackInfo ci) {
		if (FabricLoader.getInstance().getEnvironmentType() != EnvType.SERVER)
			return;

		writer.add("swimming");
		writer.add("keymapping", Component.translatable("key.sprint"));
		ci.cancel();
	}
}
