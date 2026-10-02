package magicjinn.artifactspatch.poly;

import artifacts.network.payload.UpdateConfigValuePacket;
import eu.pb4.polymer.core.api.utils.PolymerUtils;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/**
 * Keeps everlasting Artifacts food usable for Polymer/vanilla clients without ghost slots.
 * The client expects one item to be consumed when eating; we briefly inflate the server stack to 2
 * so after eating the counts stay aligned (server restores to 1).
 */
public final class PolymerFoodUseGuard {
	private PolymerFoodUseGuard() {
	}

	public static void register() {
		// Eating is handled in {@link magicjinn.artifactspatch.mixin.LivingEntityFoodUseMixin}.
	}

	public static boolean shouldApplyPolymerEverlastingEatFix(ServerPlayer player, ItemStack stack) {
		return PolyArtifactsItem.isNonPolymerFood(stack) && PolymerClientChecks.lacksArtifactsClient(player);
	}

	public static void onStartEverlastingEat(ServerPlayer player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!shouldApplyPolymerEverlastingEatFix(player, stack)) {
			return;
		}
		if (stack.getCount() == 1) {
			stack.setCount(2);
			syncHand(player);
		}
	}

	public static void onCompleteEverlastingEat(ServerPlayer player) {
		ItemStack stack = player.getUseItem();
		if (!shouldApplyPolymerEverlastingEatFix(player, stack)) {
			return;
		}
		if (!stack.isEmpty() && stack.getCount() != 1) {
			stack.setCount(1);
		}
		syncHand(player);
	}

	public static void resyncInventory(ServerPlayer player) {
		player.stopUsingItem();
		player.inventoryMenu.broadcastChanges();
		PolymerUtils.reloadInventory(player);
	}

	private static void syncHand(ServerPlayer player) {
		player.inventoryMenu.broadcastChanges();
		PolymerUtils.reloadInventory(player);
	}
}
