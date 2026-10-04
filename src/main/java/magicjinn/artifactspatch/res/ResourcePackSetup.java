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
	private static final String ARTIFACTS_BRIDGED_ITEMS_PREFIX = "assets/artifacts/items/-/item/";
	private static final String ARTIFACTS_HELD_MODELS_PREFIX = "assets/artifacts/models/item/";
	private static final String HELD_ITEM_TEXTURE = "artifacts:item/" + HeldCuboidItemModels.HELD_TEXTURE_PATH;

	private ResourcePackSetup() {
	}

	public static void register() {
		// POLYMER WORKAROUND: Server-side bool property for pack generation; vanilla clients cannot load artifacts:needs_repair.
		BooleanProperty.TYPES.put(Artifacts.id("needs_repair"), ArtifactsNeedsRepairProperty.MAP_CODEC);

		ResourcePackExtras.forDefault().addBridgedModelsFolder(Artifacts.id("item"), (id, builder) ->
				new ItemAsset(new BasicItemModel(id), new ItemAsset.Properties(false, false))
		);
		PolymerResourcePackUtils.RESOURCE_PACK_CREATION_EVENT.register(builder ->
				builder.addResourceConverter(ResourcePackSetup::convertPackResource)
		);
	}

	private static PackResource convertPackResource(String path, PackResource resource) {
		resource = fixHeldCuboidModelTextures(path, resource);
		resource = patchHeldDisplayContextItemAssets(path, resource);
		resource = stripNeedsRepairItemModels(path, resource);
		return resource;
	}

	/** Polymer also emits bridged {@code items/-/item/<id>.json} from {@code models/item/}; keep it in sync with {@code items/<id>.json}. */
	private static PackResource patchHeldDisplayContextItemAssets(String path, PackResource resource) {
		if (!path.startsWith(ARTIFACTS_BRIDGED_ITEMS_PREFIX) || !path.endsWith(".json")) {
			return resource;
		}
		String itemId = path.substring(ARTIFACTS_BRIDGED_ITEMS_PREFIX.length(), path.length() - ".json".length());
		if (!HeldCuboidItemModels.HELD_DISPLAY_CONTEXT_ITEMS.contains(itemId)) {
			return resource;
		}
		return PackResource.fromString(heldDisplayContextItemModel(itemId));
	}

	// POLYMER WORKAROUND: held cuboids sample the items atlas; drop block/oak_log particle (mixed atlases).
	private static PackResource fixHeldCuboidModelTextures(String path, PackResource resource) {
		if (!path.startsWith(ARTIFACTS_HELD_MODELS_PREFIX) || !path.endsWith(".json")) {
			return resource;
		}
		String fileName = path.substring(ARTIFACTS_HELD_MODELS_PREFIX.length());
		if (!HeldCuboidItemModels.HELD_CUBOID_MODEL_FILES.contains(fileName)) {
			return resource;
		}
		String content = resource.asString();
		String itemTexture = HELD_ITEM_TEXTURE;
		String held = HeldCuboidItemModels.HELD_TEXTURE_PATH;
		content = content.replace("\"particle\": \"block/oak_log\"", "\"particle\": \"" + itemTexture + "\"");
		content = content.replace("\"particle\": \"block/" + held + "\"", "\"particle\": \"" + itemTexture + "\"");
		content = content.replace("\"particle\": \"artifacts:block/" + held + "\"", "\"particle\": \"" + itemTexture + "\"");
		content = content.replace(
				"\"particle\": \"minecraft:block/artifacts_" + held + "\"",
				"\"particle\": \"" + itemTexture + "\""
		);
		content = content.replace("\"umbrella\": \"artifacts:block/" + held + "\"", "\"umbrella\": \"" + itemTexture + "\"");
		content = content.replace("\"umbrella\": \"block/" + held + "\"", "\"umbrella\": \"" + itemTexture + "\"");
		return PackResource.fromString(content);
	}

	// POLYMER WORKAROUND: Artifacts item models reference artifacts:needs_repair; rewrite to plain models for the Polymer pack.
	private static PackResource stripNeedsRepairItemModels(String path, PackResource resource) {
		if (!path.startsWith(ARTIFACTS_ITEMS_PREFIX) || !path.endsWith(".json")) {
			return resource;
		}
		if (!resource.asString().contains("artifacts:needs_repair")) {
			return resource;
		}

		String itemId = path.substring(ARTIFACTS_ITEMS_PREFIX.length(), path.length() - ".json".length());
		if (HeldCuboidItemModels.HELD_DISPLAY_CONTEXT_ITEMS.contains(itemId)
				&& resource.asString().contains("minecraft:display_context")) {
			return PackResource.fromString(heldDisplayContextItemModel(itemId));
		}

		return PackResource.fromString(flatIntactItemAsset(itemId));
	}

	private static String flatIntactItemAsset(String itemId) {
		return """
				{
				  "model": {
				    "type": "minecraft:model",
				    "model": "artifacts:item/%s"
				  }
				}
				""".formatted(itemId);
	}

	/** GUI flat sprite + hand cuboid models; fallback uses the same intact flattening as other repairable items. */
	private static String heldDisplayContextItemModel(String itemId) {
		if (!HeldCuboidItemModels.UMBRELLA.equals(itemId)) {
			return flatIntactItemAsset(itemId);
		}
		String guiFallback = flatIntactItemModelNode(itemId);
		return """
				{
				  "model": {
				    "type": "minecraft:select",
				    "property": "minecraft:display_context",
				    "cases": [
				      {
				        "when": [
				          "firstperson_lefthand",
				          "firstperson_righthand",
				          "thirdperson_lefthand",
				          "thirdperson_righthand",
				          "head"
				        ],
				        "model": {
				          "type": "minecraft:condition",
				          "property": "minecraft:using_item",
				          "on_false": {
				            "type": "minecraft:model",
				            "model": "artifacts:item/umbrella_held"
				          },
				          "on_true": {
				            "type": "minecraft:model",
				            "model": "artifacts:item/umbrella_held_blocking"
				          }
				        }
				      }
				    ],
				    "fallback": %s
				  }
				}
				""".formatted(guiFallback);
	}

	private static String flatIntactItemModelNode(String itemId) {
		return """
				{
				  "type": "minecraft:model",
				  "model": "artifacts:item/%s"
				}
				""".formatted(itemId);
	}
}
