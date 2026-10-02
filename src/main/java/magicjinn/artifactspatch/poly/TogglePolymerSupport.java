package magicjinn.artifactspatch.poly;

import artifacts.registry.ModDataComponents;
import artifacts.util.ItemDamageUtil;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.TrinketsApi;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

// POLYMER WORKAROUND: Toggle artifacts (Universal Attractor, night vision goggles, etc.) use the Artifacts client
// toggle key and DISABLED_BY_TOGGLE on the stack. Polymer/vanilla clients cannot send ToggleKeyPressedPacket,
// so those items stay off until we clear the disabled flag while equipped.
public final class TogglePolymerSupport {
	private TogglePolymerSupport() {
	}

	public static void register() {
		if (!FabricLoader.getInstance().isModLoaded("trinkets_updated")) {
			return;
		}
		ServerTickEvents.END_SERVER_TICK.register(TogglePolymerSupport::tick);
	}

	private static void tick(MinecraftServer server) {
		if ((server.getTickCount() & 0b1111) != 0) {
			return;
		}
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (!PolymerClientChecks.lacksArtifactsClient(player)) {
				continue;
			}
			enableEquippedToggles(player);
		}
	}

	private static void enableEquippedToggles(ServerPlayer player) {
		var disabledKey = ModDataComponents.DISABLED_BY_TOGGLE.get();
		var toggleKey = ModDataComponents.TOGGLE_KEY.get();
		TrinketsApi.getAttachment(player).forEach((TrinketSlotAccess slot, ItemStack stack) -> {
			if (stack.isEmpty() || !stack.has(toggleKey) || !stack.has(disabledKey)) {
				return;
			}
			if (ItemDamageUtil.needsRepair(stack)) {
				return;
			}
			ItemStack updated = stack.copy();
			updated.remove(disabledKey);
			slot.set(updated);
		});
	}
}
