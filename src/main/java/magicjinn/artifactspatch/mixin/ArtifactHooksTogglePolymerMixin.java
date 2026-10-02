package magicjinn.artifactspatch.mixin;

import artifacts.equipment.EquipmentSlotAccess;
import artifacts.event.ArtifactHooks;
import artifacts.registry.ModDataComponents;
import artifacts.util.ItemDamageUtil;
import magicjinn.artifactspatch.poly.PolymerClientChecks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// POLYMER WORKAROUND: Toggle trinkets stay "disabled" for ability ticks when DISABLED_BY_TOGGLE is on the stack.
// Polymer clients cannot press the Artifacts toggle key; treat toggle-disabled stacks as enabled unless broken.
@Mixin(ArtifactHooks.class)
public class ArtifactHooksTogglePolymerMixin {
	@Redirect(
			method = "onAbilityTick",
			at = @At(
					value = "INVOKE",
					target = "Lartifacts/equipment/EquipmentSlotAccess;isDisabledOrBroken()Z"
			)
	)
	private static boolean artifactsPatch$ignoreToggleDisabledForPolymer(EquipmentSlotAccess slot) {
		if (slot.entity() instanceof ServerPlayer player && PolymerClientChecks.lacksArtifactsClient(player)) {
			ItemStack stack = slot.get();
			if (stack.has(ModDataComponents.DISABLED_BY_TOGGLE.get()) && !ItemDamageUtil.needsRepair(stack)) {
				return false;
			}
		}
		return slot.isDisabledOrBroken();
	}
}
