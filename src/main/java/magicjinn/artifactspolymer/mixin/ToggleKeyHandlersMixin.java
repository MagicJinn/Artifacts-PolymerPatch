package magicjinn.artifactspolymer.mixin;

import java.util.function.Consumer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import artifacts.client.ToggleKeyHandlers;
import artifacts.component.ToggleIdentifier;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;

/**
 * POLYMER WORKAROUND: Toggle-key tooltip lines call
 * {@link ToggleKeyHandlers#addTooltip}, which pulls in {@code ModKeyMappings}
 * (client-only {@code KeyMapping}). Skip those lines on dedicated servers so
 * Polymer can still build the rest of the ability tooltip.
 *
 * Targets the named method instead of {@code lambda$addAbilityDescriptions$N},
 * which renumbers when Artifacts edits {@code TooltipHelper}.
 */
@Mixin(ToggleKeyHandlers.class)
public class ToggleKeyHandlersMixin {
	@Inject(method = "addTooltip", at = @At("HEAD"), cancellable = true, require = 0)
	private static void artifactsPolymer$skipToggleKeyTooltip(
			ToggleIdentifier identifier,
			boolean alwaysShow,
			Consumer<Component> tooltip,
			CallbackInfo ci) {
		if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER)
			ci.cancel();
	}
}
