package magicjinn.artifactspatch.res;

import artifacts.Artifacts;
import eu.pb4.polymer.resourcepack.api.PackResource;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.polymer.resourcepack.extras.api.ResourcePackExtras;
import eu.pb4.polymer.resourcepack.extras.api.format.item.ItemAsset;
import eu.pb4.polymer.resourcepack.extras.api.format.item.model.BasicItemModel;
import eu.pb4.polymer.resourcepack.extras.api.format.item.property.bool.BooleanProperty;

public final class ResourcePackSetup {
	private static final String ARTIFACTS_ITEMS_PREFIX = "assets/artifacts/items/";

	private ResourcePackSetup() {
	}

	public static void register() {
		BooleanProperty.TYPES.put(Artifacts.id("needs_repair"), ArtifactsNeedsRepairProperty.MAP_CODEC);

		ResourcePackExtras.forDefault().addBridgedModelsFolder(Artifacts.id("item"), (id, builder) ->
				new ItemAsset(new BasicItemModel(id), new ItemAsset.Properties(false, false))
		);

		PolymerResourcePackUtils.RESOURCE_PACK_CREATION_EVENT.register(builder ->
				builder.addResourceConverter(ResourcePackSetup::stripNeedsRepairItemModels)
		);
	}

	private static PackResource stripNeedsRepairItemModels(String path, PackResource resource) {
		if (!path.startsWith(ARTIFACTS_ITEMS_PREFIX) || !path.endsWith(".json")) {
			return resource;
		}
		if (!resource.asString().contains("artifacts:needs_repair")) {
			return resource;
		}

		String itemId = path.substring(ARTIFACTS_ITEMS_PREFIX.length(), path.length() - ".json".length());
		String simplified = """
				{
				  "model": {
				    "type": "minecraft:model",
				    "model": "artifacts:item/%s"
				  }
				}
				""".formatted(itemId);
		return PackResource.fromString(simplified);
	}
}
