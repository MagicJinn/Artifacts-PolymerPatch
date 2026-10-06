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
	private static final String ARTIFACTS_HELD_MODELS_PREFIX = "assets/artifacts/models/item/";

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
		resource = patchUmbrellaHeldDisplays(path, resource);
		resource = stripNeedsRepairItemModels(path, resource);
		return resource;
	}

	/**
	 * POLYMER WORKAROUND: Artifacts' SpearAnimationsMixin uprights idle spear xRot and eases attack; vanilla spear
	 * pose leans into the face. Tip the held model back, raise it, and yaw 180 for the attack arc (negative X scale
	 * also mirrors but inverts face normals so the canopy shades darker than the underside). FP blocking needs its
	 * own rotation because Artifacts cancels first-person BLOCK arm anim and vanilla does not.
	 */
	private static PackResource patchUmbrellaHeldDisplays(String path, PackResource resource) {
		if (!path.startsWith(ARTIFACTS_HELD_MODELS_PREFIX) || !path.endsWith(".json")) {
			return resource;
		}
		String fileName = path.substring(ARTIFACTS_HELD_MODELS_PREFIX.length());
		String content = resource.asString();
		if ("umbrella_held.json".equals(fileName)) {
			content = content.replace(
					"\"thirdperson_righthand\": {\n      \"rotation\": [0, 0, 0],\n      \"translation\": [0, 0, 2]\n    }",
					"\"thirdperson_righthand\": {\n      \"rotation\": [45, 180, 0],\n      \"translation\": [0, 8, 2]\n    }"
			);
			content = content.replace(
					"\"thirdperson_lefthand\": {\n      \"rotation\": [0, 0, 0],\n      \"translation\": [0, 0, 2]\n    }",
					"\"thirdperson_lefthand\": {\n      \"rotation\": [45, 180, 0],\n      \"translation\": [0, 8, 2]\n    }"
			);
			return PackResource.fromString(content);
		}
		if ("umbrella_held_blocking.json".equals(fileName)) {
			content = content.replace("\"rotation\": [-90, 22.5, 0]", "\"rotation\": [90, 22.5, -90]");
			return PackResource.fromString(content);
		}
		return resource;
	}

	// POLYMER WORKAROUND: held cuboid particle is block/oak_log (blocks atlas); faces already use items-atlas umbrella_held.
	private static PackResource fixHeldCuboidModelTextures(String path, PackResource resource) {
		if (!path.equals(ARTIFACTS_HELD_MODELS_PREFIX + HeldCuboidItemModels.HELD_TEXTURE_MODEL)) {
			return resource;
		}
		String content = resource.asString().replace(
				"\"particle\": \"block/oak_log\"",
				"\"particle\": \"" + HeldCuboidItemModels.HELD_ITEM_ATLAS_SPRITE + "\""
		);
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
		if (HeldCuboidItemModels.UMBRELLA.equals(itemId)) {
			return PackResource.fromString(umbrellaDisplayContextItemModel());
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

	/** GUI flat sprite + hand cuboid models; omits artifacts:needs_repair for vanilla clients. */
	private static String umbrellaDisplayContextItemModel() {
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
				    "fallback": {
				      "type": "minecraft:model",
				      "model": "artifacts:item/umbrella"
				    }
				  }
				}
				""";
	}
}
