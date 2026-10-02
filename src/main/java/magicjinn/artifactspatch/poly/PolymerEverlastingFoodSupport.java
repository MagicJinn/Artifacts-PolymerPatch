package magicjinn.artifactspatch.poly;

import eu.pb4.polymer.common.api.PolymerCommonUtils;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Packet-side stack count for everlasting Artifacts food on Polymer clients.
 */
public final class PolymerEverlastingFoodSupport {
	private PolymerEverlastingFoodSupport() {
	}

	public static boolean appliesTo(PacketContext context, ItemStack serverStack) {
		if (!PolyArtifactsItem.isNonPolymerFood(serverStack)) {
			return false;
		}
		ServerPlayer player = PolymerCommonUtils.getPlayer(context);
		return player != null && PolymerClientChecks.lacksArtifactsClient(player);
	}

	/**
	 * POLYMER WORKAROUND: Vanilla clients predict eating a count-1 stack down to zero. Artifacts food is
	 * infinite server-side; we send count+1 on the polymer wire stack only (real inventory unchanged) so
	 * after the client consumes one item it still shows one left. Pair with
	 * {@link PolyArtifactsItem#shouldStorePolymerItemStackCount()} so Polymer keeps the true count in NBT.
	 */
	public static void inflatePolymerStackCountForClient(ItemStack serverStack, ItemStack polymerStack) {
		if (serverStack.getCount() == 1) {
			polymerStack.setCount(2);
		}
	}
}
