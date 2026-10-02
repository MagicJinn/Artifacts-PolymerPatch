package magicjinn.artifactspatch.poly;

import artifacts.Artifacts;
import eu.pb4.polymer.core.api.item.VanillaModeledPolymerItem;
import eu.pb4.polymer.resourcepack.extras.api.ResourcePackExtras;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
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

	@Override
	public Item getPolymerItem(ItemStack itemStack, PacketContext packetContext) {
		return Items.TRIAL_KEY;
	}

	@Override
	public Identifier getPolymerItemModel(ItemStack stack, PacketContext context, HolderLookup.Provider lookup) {
		var path = BuiltInRegistries.ITEM.getKey(item).getPath();
		return ResourcePackExtras.bridgeModel(Artifacts.id("item/" + path));
	}

	@Override
	public void modifyBasePolymerItemStack(
			ItemStack original,
			ItemStack polymer,
			PacketContext context,
			HolderLookup.Provider lookup
	) {
		// Keep FOOD/CONSUMABLE on polymer stacks so vanilla clients can start eating;
		// {@link PolymerFoodUseGuard} prevents desync via a brief stack size of 2.
	}

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
