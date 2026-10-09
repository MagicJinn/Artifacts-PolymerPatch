package magicjinn.artifactspolymer.polymer;

import artifacts.registry.ModItems;
import eu.pb4.polymer.core.api.item.PolymerItemUtils;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.UseRemainder;

public final class EverlastingFoodPolymerPatch {
	// Each eat replaces the stack with the remainder template. That template must
	// itself carry USE_REMAINDER, or the next eat has nothing to predict with.
	// Nest enough layers for a long eat streak without a slot resync.
	private static final int REMAINDER_DEPTH = 8;

	public static void patch() {
		// POLYMER WORKAROUND: Keep Artifacts' INFINITE_CONSUMABLE on the server stack
		// (correct durability / no consume). Polymer does not copy USE_REMAINDER onto
		// client stand-ins, so add it only on the polymer stack so vanilla clients
		// predict "eat -> get the same item back".
		PolymerItemUtils.ITEM_MODIFICATION_EVENT.register((original, client, context) -> {
			if (!isEverlastingFood(original.getItem()))
				return client;

			client.set(DataComponents.USE_REMAINDER, recursiveUseRemainder(client));
			return client;
		});
	}

	/** Recursively nest enough USE_REMAINDER components to prevent slot resyncs */
	private static UseRemainder recursiveUseRemainder(ItemStack appearance) {
		ItemStack seed = appearance.copyWithCount(1);
		seed.set(DataComponents.USE_REMAINDER, null);

		UseRemainder remainder = new UseRemainder(ItemStackTemplate.fromNonEmptyStack(seed));
		for (int i = 0; i < REMAINDER_DEPTH; i++) {
			ItemStack layer = appearance.copyWithCount(1);
			layer.set(DataComponents.USE_REMAINDER, remainder);
			remainder = new UseRemainder(ItemStackTemplate.fromNonEmptyStack(layer));
		}
		return remainder;
	}

	private static boolean isEverlastingFood(Item item) {
		return item == ModItems.EVERLASTING_BEEF.value() || item == ModItems.ETERNAL_STEAK.value();
	}
}
