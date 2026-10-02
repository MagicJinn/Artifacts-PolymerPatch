package magicjinn.artifactspatch.res;

import artifacts.Artifacts;
import eu.pb4.polymer.resourcepack.extras.api.ResourcePackExtras;
import eu.pb4.polymer.resourcepack.extras.api.format.item.ItemAsset;
import eu.pb4.polymer.resourcepack.extras.api.format.item.model.BasicItemModel;
import eu.pb4.polymer.resourcepack.extras.api.format.item.property.bool.BooleanProperty;

public final class ResourcePackSetup {
	private ResourcePackSetup() {
	}

	public static void register() {
		BooleanProperty.TYPES.put(Artifacts.id("needs_repair"), ArtifactsNeedsRepairProperty.MAP_CODEC);

		ResourcePackExtras.forDefault().addBridgedModelsFolder(Artifacts.id("item"), (id, builder) ->
				new ItemAsset(new BasicItemModel(id), new ItemAsset.Properties(false, false))
		);
	}
}
