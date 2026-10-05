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
	private static final String HELD_ITEM_TEXTURE = HeldCuboidItemModels.HELD_ITEM_ATLAS_SPRITE;

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
		resource = patchHeldDisplayContextItemAssets(path, resource);
		resource = stripNeedsRepairItemModels(path, resource);
		return resource;
	}

	/**
	 * POLYMER WORKAROUND: Artifacts' SpearAnimationsMixin forces idle spear xRot to -π/4 (upright) and eases
	 * attack; vanilla spear pose leans ~45° into the face. Tip the held model back and mirror on X so the
	 * attack arc matches Artifacts' horizontal direction. FP blocking is patched separately; TP blocking
	 * stays on umbrella_held_blocking unchanged.
	 */
	private static PackResource patchUmbrellaHeldDisplays(String path, PackResource resource) {
		if (!path.startsWith(ARTIFACTS_HELD_MODELS_PREFIX) || !path.endsWith(".json")) {
			return resource;
		}
		String fileName = path.substring(ARTIFACTS_HELD_MODELS_PREFIX.length());
		String content = resource.asString();
		if ("umbrella_held.json".equals(fileName)) {
			// Stock thirdperson is [0,0,0] + [0,0,2]; spear arm alone leaves a ~45° forward lean.
			// +45 tips back upright; -45 was forward again. Raise Y so the canopy clears the head.
			content = content.replace(
					"\"thirdperson_righthand\": {\n      \"rotation\": [0, 0, 0],\n      \"translation\": [0, 0, 2]\n    }",
					"\"thirdperson_righthand\": {\n      \"rotation\": [45, 0, 0],\n      \"translation\": [0, 8, 2],\n      \"scale\": [-1, 1, 1]\n    }"
			);
			content = content.replace(
					"\"thirdperson_lefthand\": {\n      \"rotation\": [0, 0, 0],\n      \"translation\": [0, 0, 2]\n    }",
					"\"thirdperson_lefthand\": {\n      \"rotation\": [45, 0, 0],\n      \"translation\": [0, 8, 2],\n      \"scale\": [-1, 1, 1]\n    }"
			);
			return PackResource.fromString(content);
		}
		if ("umbrella_held_blocking.json".equals(fileName)) {
			// Only firstperson uses [-90, 22.5, 0]; thirdperson stays [-45, 0, -15].
			content = content.replace("\"rotation\": [-90, 22.5, 0]", "\"rotation\": [90, 22.5, -90]");
			return PackResource.fromString(content);
		}
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

	// POLYMER WORKAROUND: single-atlas item sprites for cuboid faces; drop block/oak_log particle (mixed atlases).
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
