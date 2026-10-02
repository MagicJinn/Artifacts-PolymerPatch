package dev.magicjinn.artifactspatch.res;

import artifacts.Artifacts;
import eu.pb4.polymer.resourcepack.extras.api.ResourcePackExtras;
import eu.pb4.polymer.resourcepack.extras.api.format.item.ItemAsset;
import eu.pb4.polymer.resourcepack.extras.api.format.item.model.BasicItemModel;

public final class ResourcePackSetup {
	private ResourcePackSetup() {
	}

	public static void register() {
		ResourcePackExtras.forDefault().addBridgedModelsFolder(Artifacts.id("item"), (id, builder) ->
				new ItemAsset(new BasicItemModel(id), new ItemAsset.Properties(false, false))
		);
	}
}
