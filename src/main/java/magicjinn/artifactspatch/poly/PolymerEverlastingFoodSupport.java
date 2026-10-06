package magicjinn.artifactspatch.poly;

import eu.pb4.polymer.core.api.utils.PolymerUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bottle-style everlasting food for Polymer/vanilla clients: match client eat prediction, then refill the slot.
 */
public final class PolymerEverlastingFoodSupport {
	private static final Map<UUID, EatSession> SESSIONS = new ConcurrentHashMap<>();

	private PolymerEverlastingFoodSupport() {
	}

	public static boolean shouldHandle(ServerPlayer player, ItemStack stack) {
		return PolyArtifactsItem.isNonPolymerFood(stack) && PolymerClientChecks.lacksArtifactsClient(player);
	}

	/**
	 * POLYMER WORKAROUND: Vanilla clients predict consuming the food item. Artifacts keeps the real stack via
	 * INFINITE_CONSUMABLE; after eat completes we shrink the server stack once (like it was eaten) and if the slot
	 * is empty we place a fresh copy, same idea as a bottle staying in the hand after drinking.
	 */
	public static void beginEat(ServerPlayer player, InteractionHand hand, ItemStack stack) {
		if (!shouldHandle(player, stack)) {
			return;
		}
		SESSIONS.put(player.getUUID(), new EatSession(hand, ItemStackTemplate.fromNonEmptyStack(stack.copy())));
	}

	public static void finishEat(ServerPlayer player) {
		EatSession session = SESSIONS.remove(player.getUUID());
		if (session == null) {
			return;
		}

		ItemStack inHand = player.getItemInHand(session.hand);
		if (inHand.isEmpty()) {
			player.setItemInHand(session.hand, session.refillTemplate.create());
		} else {
			inHand.shrink(1);
			if (inHand.isEmpty()) {
				player.setItemInHand(session.hand, session.refillTemplate.create());
			}
		}

		player.inventoryMenu.broadcastChanges();
		PolymerUtils.reloadInventory(player);
	}

	public static void cancelEat(ServerPlayer player) {
		SESSIONS.remove(player.getUUID());
	}

	private record EatSession(InteractionHand hand, ItemStackTemplate refillTemplate) {
	}
}
