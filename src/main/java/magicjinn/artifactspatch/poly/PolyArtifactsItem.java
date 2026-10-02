package magicjinn.artifactspatch.poly;

import artifacts.Artifacts;
import eu.pb4.polymer.core.api.item.VanillaModeledPolymerItem;
import eu.pb4.polymer.resourcepack.extras.api.ResourcePackExtras;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Set;

/**
 * Presents Artifacts items to non-Artifacts clients using vanilla item types plus
 * Artifacts models from the generated Polymer resource pack.
 */
public record PolyArtifactsItem(Item item) implements VanillaModeledPolymerItem {
	private static final Set<String> NON_POLYMER_FOOD_ITEMS = Set.of(
			"everlasting_beef",
			"eternal_steak"
	);

	public static boolean isNonPolymerFood(ItemStack stack) {
		if (stack.isEmpty()) {
			return false;
		}
		return NON_POLYMER_FOOD_ITEMS.contains(BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath());
	}

	// POLYMER WORKAROUND: Vanilla clients have no Artifacts item ids; trial_key is a vanilla type that accepts
	// custom item models from the Polymer pack (Artifacts models bridged via ResourcePackExtras).
	@Override
	public Item getPolymerItem(ItemStack itemStack, PacketContext packetContext) {
		if (isNonPolymerFood(itemStack)) {
			// POLYMER WORKAROUND: Use vanilla beef items so eat animation/sounds match; model stays Artifacts via bridge.
			var path = BuiltInRegistries.ITEM.getKey(item).getPath();
			if ("everlasting_beef".equals(path)) {
				return Items.BEEF;
			}
			if ("eternal_steak".equals(path)) {
				return Items.COOKED_BEEF;
			}
		}
		return Items.TRIAL_KEY;
	}

	@Override
	public Identifier getPolymerItemModel(ItemStack stack, PacketContext context, HolderLookup.Provider lookup) {
		var path = BuiltInRegistries.ITEM.getKey(item).getPath();
		return ResourcePackExtras.bridgeModel(Artifacts.id("item/" + path));
	}

	// POLYMER WORKAROUND: Trinkets Polymer is a server dependency; vanilla clients cannot install Trinkets, so
	// Artifacts' "missing dependency" tooltip line is misleading on the Polymer pack.
	@Override
	public void modifyClientTooltip(List<Component> tooltip, ItemStack stack, PacketContext context) {
		tooltip.removeIf(PolyArtifactsItem::isMissingTrinketsDependencyLine);
	}

	private static boolean isMissingTrinketsDependencyLine(Component line) {
		if (line.getContents() instanceof TranslatableContents translatable) {
			return "artifacts.tooltip.missing_dependency".equals(translatable.getKey());
		}
		return false;
	}
}
