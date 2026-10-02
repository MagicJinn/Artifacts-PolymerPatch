package magicjinn.artifactspatch.poly;

import eu.pb4.polymer.core.api.other.PolymerComponent;
import eu.pb4.trinkets.api.component.TrinketDataComponents;

/**
 * Marks Trinkets Updated registry entries as server-only so vanilla / Polymer clients are not
 * kicked by Fabric Registry Sync ({@code trinkets:*} data components).
 */
public final class TrinketsRegistrySync {
	private TrinketsRegistrySync() {
	}

	public static void register() {
		PolymerComponent.registerDataComponent(
				TrinketDataComponents.ATTRIBUTE_MODIFIERS,
				TrinketDataComponents.EQUIPMENT
		);
	}
}
