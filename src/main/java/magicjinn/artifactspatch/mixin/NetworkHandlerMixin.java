package magicjinn.artifactspatch.mixin;

import artifacts.network.NetworkHandler;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Artifacts clientbound payloads are only registered when the Artifacts client mod is present.
 * Skip sending them to vanilla / non-Artifacts clients so they are not disconnected.
 */
@Mixin(NetworkHandler.class)
public class NetworkHandlerMixin {
	@Inject(method = "sendToPlayer", at = @At("HEAD"), cancellable = true)
	private static void artifacts$skipWhenClientCannotReceive(ServerPlayer player, CustomPacketPayload payload, CallbackInfo ci) {
		if (player == null || !ServerPlayNetworking.canSend(player, payload.type())) {
			ci.cancel();
		}
	}
}
