package magicjinn.artifactspatch.poly;

import artifacts.registry.ModDataComponents;
import eu.pb4.polymer.core.api.utils.PolymerUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Warp Drive ender pearls for Polymer/vanilla clients: keep server stack, bump the slot so client drops consume prediction.
 */
public final class PolymerWarpDrivePearlSupport {
	private static final Map<UUID, ThrowSession> SESSIONS = new ConcurrentHashMap<>();

	private PolymerWarpDrivePearlSupport() {
	}

	public static boolean shouldHandle(ServerPlayer player, ItemStack stack) {
		if (stack.isEmpty() || !stack.is(Items.ENDER_PEARL)) {
			return false;
		}
		if (player.getAbilities().instabuild) {
			return false;
		}
		if (!PolymerClientChecks.lacksArtifactsClient(player)) {
			return false;
		}
		return ModDataComponents.ENDER_PEARL_HUNGER_COST.on(player).findAny();
	}

	/**
	 * POLYMER WORKAROUND: Vanilla clients always predict consuming the pearl. Warp Drive skips
	 * ItemStack.consume and spends hunger instead, so the real count must stay. Shrinking like food
	 * would delete pearls (they stack). Instead clear+restore the kept stack so the client gets a
	 * fresh slot update, same "remainder appears" idea as a bottle without changing count.
	 */
	public static void beginThrow(ServerPlayer player, InteractionHand hand, ItemStack stack) {
		if (!shouldHandle(player, stack)) {
			return;
		}
		SESSIONS.put(player.getUUID(), new ThrowSession(hand, stack.getCount(), ItemStackTemplate.fromNonEmptyStack(stack.copy())));
	}

	public static void finishThrow(ServerPlayer player) {
		ThrowSession session = SESSIONS.remove(player.getUUID());
		if (session == null) {
			return;
		}

		ItemStack inHand = player.getItemInHand(session.hand);
		// Consume was not skipped (e.g. not enough hunger) — client and server already agree.
		if (inHand.isEmpty() || !inHand.is(Items.ENDER_PEARL) || inHand.getCount() != session.countBefore) {
			return;
		}

		player.setItemInHand(session.hand, ItemStack.EMPTY);
		player.setItemInHand(session.hand, session.refillTemplate.create());

		player.inventoryMenu.broadcastChanges();
		PolymerUtils.reloadInventory(player);
	}

	public static void cancelThrow(ServerPlayer player) {
		SESSIONS.remove(player.getUUID());
	}

	private record ThrowSession(InteractionHand hand, int countBefore, ItemStackTemplate refillTemplate) {
	}
}
