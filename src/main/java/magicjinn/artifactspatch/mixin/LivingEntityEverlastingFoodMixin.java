package magicjinn.artifactspatch.mixin;

import magicjinn.artifactspatch.poly.PolymerEverlastingFoodSupport;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityEverlastingFoodMixin {
	@Inject(method = "startUsingItem", at = @At("TAIL"))
	private void artifactsPatch$beginEverlastingFoodSession(InteractionHand hand, CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (!(self instanceof ServerPlayer player)) {
			return;
		}
		PolymerEverlastingFoodSupport.beginEat(player, hand, player.getItemInHand(hand));
	}

	@Inject(method = "completeUsingItem", at = @At("TAIL"))
	private void artifactsPatch$refillEverlastingFood(CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (!(self instanceof ServerPlayer player)) {
			return;
		}
		PolymerEverlastingFoodSupport.finishEat(player);
	}

	@Inject(method = "releaseUsingItem", at = @At("HEAD"))
	private void artifactsPatch$cancelEverlastingFoodSession(CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (!(self instanceof ServerPlayer player)) {
			return;
		}
		PolymerEverlastingFoodSupport.cancelEat(player);
	}
}
