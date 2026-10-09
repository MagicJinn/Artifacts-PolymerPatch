package magicjinn.artifactspolymer.polymer;

import artifacts.registry.ModItems;
import eu.pb4.polymer.core.api.item.PolymerItemUtils;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.UseRemainder;

public final class EverlastingFoodPolymerPatch {
	private EverlastingFoodPolymerPatch() {
	}

	public static void patch() {
		// POLYMER WORKAROUND: Keep Artifacts' INFINITE_CONSUMABLE on the server stack
		// (correct durability / no consume). Polymer does not copy USE_REMAINDER onto
		// client stand-ins, so add it only on the polymer stack so vanilla clients
		// predict "eat -> get the same item back".
		PolymerItemUtils.ITEM_MODIFICATION_EVENT.register((original, client, context) -> {
			if (!isEverlastingFood(original.getItem()))
				return client;

			client.set(DataComponents.USE_REMAINDER,
					new UseRemainder(ItemStackTemplate.fromNonEmptyStack(client.copyWithCount(1))));
			return client;
		});
	}

	private static boolean isEverlastingFood(Item item) {
		return item == ModItems.EVERLASTING_BEEF.value() || item == ModItems.ETERNAL_STEAK.value();
	}
}
