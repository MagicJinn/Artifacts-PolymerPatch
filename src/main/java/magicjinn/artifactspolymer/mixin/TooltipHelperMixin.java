package magicjinn.artifactspolymer.mixin;

import java.util.function.Consumer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import artifacts.component.ToggleIdentifier;
import artifacts.util.TooltipHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

@Mixin(TooltipHelper.class)
public class TooltipHelperMixin {
	/**
	 * POLYMER WORKAROUND: Toggle-key tooltip lines call
	 * {@code ToggleKeyHandlers.addTooltip}, which pulls in {@code ModKeyMappings}
	 * (client-only {@code KeyMapping}). Skip those lines on dedicated servers so
	 * Polymer can still build the rest of the ability tooltip.
	 */
	@Inject(method = "lambda$addAbilityDescriptions$2", at = @At("HEAD"), cancellable = true)
	private static void artifactsPolymer$skipToggleKeyTooltip(
			ItemStack stack,
			Player player,
			Consumer<Component> tooltip,
			ToggleIdentifier identifier,
			CallbackInfo ci) {
		if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER)
			ci.cancel();
	}
}
