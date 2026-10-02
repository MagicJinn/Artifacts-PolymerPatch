package magicjinn.artifactspatch.mixin;

import artifacts.component.ability.EquipmentAttributeModifier;
import artifacts.equipment.EquipmentSlotAccess;
import magicjinn.artifactspatch.poly.PolymerClientChecks;
import magicjinn.artifactspatch.poly.PolymerPlayerAttributeSync;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// POLYMER WORKAROUND: After Artifacts applies equipment attribute modifiers server-side, sync attributes to
// Polymer/vanilla clients (they do not run Artifacts client attribute logic).
@Mixin(EquipmentAttributeModifier.Ticker.class)
public class EquipmentAttributeModifierTickerMixin {
	@Inject(
			method = "wornTick",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/ai/attributes/AttributeInstance;addPermanentModifier(Lnet/minecraft/world/entity/ai/attributes/AttributeModifier;)V",
					shift = At.Shift.AFTER
			)
	)
	private void artifactsPatch$syncAttributesAfterApply(
			EquipmentAttributeModifier ability,
			EquipmentSlotAccess slotAccess,
			LivingEntity entity,
			boolean isOnCooldown,
			boolean isDisabled,
			CallbackInfo ci
	) {
		syncIfNeeded(entity);
	}

	@Inject(method = "onUnequip", at = @At("TAIL"))
	private void artifactsPatch$syncAttributesAfterUnequip(EquipmentAttributeModifier ability, LivingEntity entity, CallbackInfo ci) {
		syncIfNeeded(entity);
	}

	private static void syncIfNeeded(LivingEntity entity) {
		if (entity instanceof ServerPlayer player && PolymerClientChecks.lacksArtifactsClient(player)) {
			PolymerPlayerAttributeSync.sync(player);
		}
	}
}
