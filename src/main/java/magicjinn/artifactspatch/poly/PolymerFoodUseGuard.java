package magicjinn.artifactspatch.poly;

import artifacts.network.payload.UpdateConfigValuePacket;
import eu.pb4.polymer.core.api.utils.PolymerUtils;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;

/**
 * Blocks right-click "eating" of Artifacts everlasting food for Polymer/vanilla clients.
 * The visible polymer stack has no {@code FOOD}/{@code CONSUMABLE}, but the server item still does;
 * without cancelling use, clients can desync (ghost empty slot) while the real stack remains.
 */
public final class PolymerFoodUseGuard {
	private PolymerFoodUseGuard() {
	}

	public static void register() {
		UseItemCallback.EVENT.register((player, world, hand) -> {
			if (world.isClientSide()) {
				return InteractionResult.PASS;
			}
			if (!(player instanceof ServerPlayer serverPlayer)) {
				return InteractionResult.PASS;
			}

			ItemStack stack = player.getItemInHand(hand);
			if (!PolyArtifactsItem.isNonPolymerFood(stack) || hasArtifactsClient(serverPlayer)) {
				return InteractionResult.PASS;
			}

			resyncInventory(serverPlayer);
			return InteractionResult.FAIL;
		});
	}

	public static boolean shouldBlockUse(ServerPlayer player, ItemStack stack) {
		return PolyArtifactsItem.isNonPolymerFood(stack) && !hasArtifactsClient(player);
	}

	public static void resyncInventory(ServerPlayer player) {
		player.stopUsingItem();
		player.inventoryMenu.broadcastChanges();
		PolymerUtils.reloadInventory(player);
	}

	private static boolean hasArtifactsClient(ServerPlayer player) {
		return ServerPlayNetworking.canSend(player, UpdateConfigValuePacket.TYPE);
	}
}
