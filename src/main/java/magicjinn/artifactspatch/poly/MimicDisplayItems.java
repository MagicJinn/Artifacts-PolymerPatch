package magicjinn.artifactspatch.poly;

import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.rsm.api.RegistrySyncUtils;
import magicjinn.artifactspatch.ArtifactsPolymerPatch;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

// POLYMER WORKAROUND: Server-only display items for mimic ItemDisplay virtual entities (not obtainable in survival).
public final class MimicDisplayItems {
	public static final ResourceKey<Item> LID_KEY = ResourceKey.create(
			Registries.ITEM,
			ArtifactsPolymerPatch.id("mimic_display_lid")
	);
	public static final ResourceKey<Item> BOTTOM_KEY = ResourceKey.create(
			Registries.ITEM,
			ArtifactsPolymerPatch.id("mimic_display_bottom")
	);

	public static Item LID;
	public static Item BOTTOM;

	private MimicDisplayItems() {
	}

	public static void register() {
		LID = register(LID_KEY, "mimic_lid");
		BOTTOM = register(BOTTOM_KEY, "mimic_bottom");
	}

	private static Item register(ResourceKey<Item> key, String modelPath) {
		Item item = Registry.register(
				BuiltInRegistries.ITEM,
				key,
				new Item(new Item.Properties())
		);
		// POLYMER WORKAROUND: Vanilla clients receive trial_key + bridged model from the pack, not this registry id.
		RegistrySyncUtils.setServerEntry(BuiltInRegistries.ITEM, item);
		PolymerItem.registerOverlay(item, new MimicDisplayPolymerItem(item, ArtifactsPolymerPatch.id("display/" + modelPath)));
		return item;
	}
}
