package magicjinn.artifactspatch.poly;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;

/**
 * Short grey tooltip notes describing Polymer-client workarounds for specific
 * Artifacts items.
 */
public final class PolymerWorkaroundTooltips {
	private static final Map<String, String> NOTES = Map.ofEntries(
			Map.entry("aqua_dashers", "Shows waterlogged barriers while sprinting to avoid desync."),
			Map.entry("strider_shoes", "Shows magma underfoot on lava so walking stays in sync."),
			Map.entry("snowshoes", "Shows snow blocks in powder snow so walking stays in sync."),
			Map.entry("steadfast_spikes", "Ice briefly looks like wool so you don't slip out of sync."),
			Map.entry("flippers", "Spoofs swimming speed on the client and server."),
			Map.entry("helium_flamingo", "Press sprint to air-swim. Will show yourself surrounded by water blocks."),
			Map.entry("charm_of_sinking",
					"Increases sinking, walking speed and step height in water to compensate. Always on (no toggle)."),
			Map.entry("digging_claws", "Triggers block breaking early on the server."),
			Map.entry("umbrella", "Held upright like a spear so the canopy looks right."),
			Map.entry("night_vision_goggles", "Always on while equipped (no toggle key)."),
			Map.entry("universal_attractor", "Always on while equipped (no toggle key)."),
			Map.entry("scarf_of_invisibility", "Always on while equipped (no toggle key)."),
			Map.entry("charm_of_shrinking", "Always on while equipped (no toggle key)."));

	private PolymerWorkaroundTooltips() {
	}

	public static void append(List<Component> tooltip, ItemStack stack) {
		if (stack.isEmpty()) {
			return;
		}
		String note = NOTES.get(BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath());
		if (note == null) {
			return;
		}
		tooltip.add(Component.literal(note).withStyle(ChatFormatting.DARK_GRAY));
	}
}
