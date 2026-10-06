package magicjinn.artifactspatch.mixin;

import magicjinn.artifactspatch.poly.PolymerWarpDrivePearlSupport;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnderpearlItem.class)
public abstract class EnderpearlItemWarpDriveMixin {
	@Inject(method = "use", at = @At("HEAD"))
	private void artifactsPatch$beginWarpDrivePearlSession(
			Level level,
			Player player,
			InteractionHand hand,
			CallbackInfoReturnable<InteractionResult> cir
	) {
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return;
		}
		PolymerWarpDrivePearlSupport.beginThrow(serverPlayer, hand, serverPlayer.getItemInHand(hand));
	}

	@Inject(method = "use", at = @At("RETURN"))
	private void artifactsPatch$refillWarpDrivePearl(
			Level level,
			Player player,
			InteractionHand hand,
			CallbackInfoReturnable<InteractionResult> cir
	) {
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return;
		}
		InteractionResult result = cir.getReturnValue();
		if (result == null || !result.consumesAction()) {
			PolymerWarpDrivePearlSupport.cancelThrow(serverPlayer);
			return;
		}
		PolymerWarpDrivePearlSupport.finishThrow(serverPlayer);
	}
}
