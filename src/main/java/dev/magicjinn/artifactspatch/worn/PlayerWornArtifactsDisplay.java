package dev.magicjinn.artifactspatch.worn;

import artifacts.Artifacts;
import dev.magicjinn.artifactspatch.ArtifactsPolymerPatch;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.IdentifiedUniqueEntityAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.TrinketsApi;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Attaches {@link ItemDisplayElement}s to players for equipped Artifacts trinkets (item models from the Polymer pack).
 * Requires Trinkets Updated on the server so Artifacts can occupy trinket slots; Trinkets Polymer handles slot UI separately.
 */
public final class PlayerWornArtifactsDisplay {
	private static final Map<UUID, WornState> STATES = new HashMap<>();

	private PlayerWornArtifactsDisplay() {
	}

	public static void register() {
		if (!FabricLoader.getInstance().isModLoaded("trinkets_updated")) {
			ArtifactsPolymerPatch.LOGGER.info(
					"Trinkets Updated not loaded; skipping worn Artifacts item displays (held/inventory models still use Polymer items)"
			);
			return;
		}

		ServerTickEvents.END_SERVER_TICK.register(PlayerWornArtifactsDisplay::tick);
		ServerPlayConnectionEvents.DISCONNECT.register((handler, _) -> removePlayer(handler.player.getUUID()));
		ArtifactsPolymerPatch.LOGGER.info("Registered worn Artifacts item displays for Trinkets slots");
	}

	private static void tick(MinecraftServer server) {
		if ((server.getTickCount() & 0b1111) != 0) {
			return;
		}

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			syncPlayer(player);
		}
	}

	private static void syncPlayer(ServerPlayer player) {
		Map<String, ItemStack> equipped = collectArtifacts(player);
		WornState state = STATES.computeIfAbsent(player.getUUID(), _ -> new WornState());

		if (equipped.isEmpty()) {
			if (state.attachment != null) {
				state.attachment.destroy();
				state.attachment = null;
				state.elements.clear();
				state.last.clear();
			}
			return;
		}

		if (state.attachment == null) {
			state.attachment = IdentifiedUniqueEntityAttachment.ofTicking(
					ArtifactsPolymerPatch.id("worn_artifacts"),
					state.holder,
					player
			);
		}

		if (equipped.equals(state.last)) {
			return;
		}

		for (var entry : state.elements.entrySet()) {
			if (!equipped.containsKey(entry.getKey())) {
				state.holder.removeElement(entry.getValue());
				state.elements.remove(entry.getKey());
			}
		}

		for (var entry : equipped.entrySet()) {
			String key = entry.getKey();
			ItemStack stack = entry.getValue();
			ItemDisplayElement element = state.elements.get(key);
			if (element == null) {
				element = new ItemDisplayElement();
				applyLayout(element, key);
				state.holder.addElement(element);
				state.elements.put(key, element);
			}
			element.setItem(stack.copy());
		}

		state.last = new HashMap<>(equipped);
	}

	private static void removePlayer(UUID playerId) {
		WornState state = STATES.remove(playerId);
		if (state != null && state.attachment != null) {
			state.attachment.destroy();
		}
	}

	private static Map<String, ItemStack> collectArtifacts(ServerPlayer player) {
		Map<String, ItemStack> map = new HashMap<>();
		TrinketsApi.getAttachment(player).forEachVisible((TrinketSlotAccess slot, ItemStack stack) -> {
			if (stack.isEmpty() || !isArtifactsItem(stack)) {
				return;
			}
			String group = slot.slotType().group();
			String key = group + "/" + slot.slotType().getId() + "#" + slot.index();
			map.put(key, stack.copy());
		});
		return map;
	}

	private static boolean isArtifactsItem(ItemStack stack) {
		return BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals(Artifacts.MOD_ID);
	}

	private static void applyLayout(ItemDisplayElement element, String slotKey) {
		String group = slotKey.split("/", 2)[0];
		var layout = WornArtifactLayout.forGroup(group);
		element.setOffset(layout.offset());
		element.setScale(layout.scale());
		element.setItemDisplayContext(layout.displayContext());
	}

	private static final class WornState {
		final ElementHolder holder = new ElementHolder();
		final Map<String, ItemDisplayElement> elements = new HashMap<>();
		Map<String, ItemStack> last = Map.of();
		IdentifiedUniqueEntityAttachment attachment;
	}
}
