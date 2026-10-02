package magicjinn.artifactspatch.mixin;

import magicjinn.artifactspatch.poly.PolymerFoodUseGuard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityFoodUseMixin {
	@Inject(method = "startUsingItem", at = @At("HEAD"))
	private void artifactsPatch$inflateEverlastingFoodStack(InteractionHand hand, CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (!(self instanceof ServerPlayer player)) {
			return;
		}
		PolymerFoodUseGuard.onStartEverlastingEat(player, hand);
	}

	@Inject(method = "completeUsingItem", at = @At("TAIL"))
	private void artifactsPatch$normalizeEverlastingFoodStack(CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (!(self instanceof ServerPlayer player)) {
			return;
		}
		PolymerFoodUseGuard.onCompleteEverlastingEat(player);
	}
}
