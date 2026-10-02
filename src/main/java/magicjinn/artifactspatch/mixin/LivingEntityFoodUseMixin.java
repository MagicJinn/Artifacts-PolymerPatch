package magicjinn.artifactspatch.mixin;

import magicjinn.artifactspatch.poly.PolymerFoodUseGuard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityFoodUseMixin {
	@Inject(method = "startUsingItem", at = @At("HEAD"), cancellable = true)
	private void artifacts$blockPolymerFoodUse(net.minecraft.world.item.ItemStack stack, int duration, CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (!(self instanceof ServerPlayer player)) {
			return;
		}
		if (!PolymerFoodUseGuard.shouldBlockUse(player, stack)) {
			return;
		}
		PolymerFoodUseGuard.resyncInventory(player);
		ci.cancel();
	}

	@Inject(method = "completeUsingItem", at = @At("HEAD"), cancellable = true)
	private void artifacts$blockPolymerFoodComplete(CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (!(self instanceof ServerPlayer player)) {
			return;
		}
		ItemStack stack = self.getUseItem();
		if (!PolymerFoodUseGuard.shouldBlockUse(player, stack)) {
			return;
		}
		PolymerFoodUseGuard.resyncInventory(player);
		ci.cancel();
	}
}
