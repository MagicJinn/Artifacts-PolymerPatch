package magicjinn.artifactspatch.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import magicjinn.artifactspatch.poly.MagnetismPolymerSupport;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMagnetPickupPolymerMixin {
	@Inject(
			method = "playerTouch",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/player/Player;take(Lnet/minecraft/world/entity/Entity;I)V",
					shift = At.Shift.AFTER
			)
	)
	private void artifacts$polymerResyncPickup(Player player, CallbackInfo ci, @Local(name = "orgCount") int orgCount) {
		if (player instanceof ServerPlayer serverPlayer) {
			MagnetismPolymerSupport.syncPickup(serverPlayer, (ItemEntity) (Object) this, orgCount);
		}
	}
}
