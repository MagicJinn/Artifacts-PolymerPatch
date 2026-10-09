package magicjinn.artifactspolymer.resourcepack;

import java.util.ArrayList;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.MapCodec;

import artifacts.Artifacts;
import eu.pb4.polymer.resourcepack.api.PackResource;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.polymer.resourcepack.extras.api.ResourcePackExtras;
import eu.pb4.polymer.resourcepack.extras.api.format.item.property.bool.BooleanProperty;
import eu.pb4.polymer.resourcepack.extras.api.format.item.ItemAsset;
import eu.pb4.polymer.resourcepack.extras.api.format.item.model.BasicItemModel;
import magicjinn.artifactspolymer.ArtifactsPolymerPatch;

public class ResourcePackSetup {

    private static final String ARTIFACTS_PREFIX = "assets/artifacts/";
    private static final String ARTIFACTS_ITEMS_PREFIX = ARTIFACTS_PREFIX + "items/";
    private static final String ARTIFACTS_HELD_MODELS_PREFIX = ARTIFACTS_PREFIX + "models/item/";
    private static final String NEEDS_REPAIR_ID = "needs_repair";
    private static final String NEEDS_REPAIR = "artifacts:" + NEEDS_REPAIR_ID;

    private static final String ITEM_ASSET = """
            {
              "model": {
                "type": "minecraft:model",
                "model": "artifacts:item/%s"
              }
            }
            """;


    public static void setup() {
        // POLYMER WORKAROUND: Server-side bool property for pack generation. Vanilla
        // clients cannot load artifacts:needs_repair.
        BooleanProperty.TYPES.put(Artifacts.id(NEEDS_REPAIR_ID), ArtifactsNeedsRepairProperty.MAP_CODEC);

        // This adds a texture to all items in the Artifacts mod
        ResourcePackExtras extras = ResourcePackExtras.forDefault();
        extras.addBridgedModelsFolder(Artifacts.id("item"),
                (id, builder) -> new ItemAsset(new BasicItemModel(id), new ItemAsset.Properties(false, false)));
        extras.addBridgedModelsFolder(ArtifactsPolymerPatch.id("entity"));

        PolymerResourcePackUtils.addModAssets("factorytools");

        PolymerResourcePackUtils.RESOURCE_PACK_CREATION_EVENT
                .register(builder -> builder.addResourceConverter(ResourcePackSetup::convertPackResource));

        MimicPolymerResourcePack.register();
    }

    // POLYMER WORKAROUND: Server-side stand-in for Artifacts'
    // artifacts:needs_repair client item model property.
    private record ArtifactsNeedsRepairProperty() implements BooleanProperty {
        public static final MapCodec<ArtifactsNeedsRepairProperty> MAP_CODEC = MapCodec
                .unit(new ArtifactsNeedsRepairProperty());

        @Override
        public MapCodec<? extends BooleanProperty> codec() {
            return MAP_CODEC;
        }
    }

    private static PackResource convertPackResource(String path, PackResource resource) {
        // POLYMER WORKAROUND: Artifacts item models reference artifacts:needs_repair.
        // Rewrite to remove the property.
        resource = stripNeedsRepairItemModels(path, resource);

        // resource = fixHeldCuboidModelTextures(path, resource);

        resource = patchUmbrellaHeldDisplays(path, resource);

        return resource;
    }

    /**
     * POLYMER WORKAROUND: Artifacts' SpearAnimationsMixin uprights idle spear xRot
     * and eases attack. Vanilla spear pose leans into the face. Tip the held model
     * back, raise it, and yaw 180 for the attack arc (negative X scale also mirrors
     * but inverts face normals so the canopy shades darker than the underside). FP
     * blocking needs its own rotation because Artifacts cancels first-person BLOCK
     * arm anim and vanilla does not.
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
                    "\"thirdperson_righthand\": {\n      \"rotation\": [45, 180, 0],\n      \"translation\": [0, 8, 2]\n    }");
            content = content.replace(
                    "\"thirdperson_lefthand\": {\n      \"rotation\": [0, 0, 0],\n      \"translation\": [0, 0, 2]\n    }",
                    "\"thirdperson_lefthand\": {\n      \"rotation\": [45, 180, 0],\n      \"translation\": [0, 8, 2]\n    }");
            return PackResource.fromString(content);
        }
        if ("umbrella_held_blocking.json".equals(fileName)) {
            content = content.replace("\"rotation\": [-90, 22.5, 0]", "\"rotation\": [90, 22.5, -90]");
            return PackResource.fromString(content);
        }
        return resource;
    }

    // POLYMER WORKAROUND: Collapse artifacts:needs_repair conditions to on_false
    // (intact) so vanilla clients never see the custom property.
    private static PackResource stripNeedsRepairItemModels(String path, PackResource resource) {
        if (!path.startsWith(ARTIFACTS_ITEMS_PREFIX) || !path.endsWith(".json"))
            return resource;

        String json = resource.asString();
        if (!json.contains(NEEDS_REPAIR))
            return resource;

        try {
            JsonElement root = collapseNeedsRepair(JsonParser.parseString(json));
            return PackResource.fromString(root.toString());
        } catch (Exception e) {
            String itemId = path.substring(ARTIFACTS_ITEMS_PREFIX.length(), path.length() - ".json".length());
            return PackResource.fromString(ITEM_ASSET.formatted(itemId));
        }
    }

    private static JsonElement collapseNeedsRepair(JsonElement element) {
        if (element == null || element.isJsonNull())
            return element;

        if (element.isJsonArray()) {
            var arr = element.getAsJsonArray();
            for (int i = 0; i < arr.size(); i++)
                arr.set(i, collapseNeedsRepair(arr.get(i)));
            return arr;
        }

        if (!element.isJsonObject())
            return element;

        JsonObject obj = element.getAsJsonObject();
        if (isNeedsRepairCondition(obj))
            return collapseNeedsRepair(obj.get("on_false"));

        for (String key : new ArrayList<>(obj.keySet()))
            obj.add(key, collapseNeedsRepair(obj.get(key)));
        return obj;
    }

    private static boolean isNeedsRepairCondition(JsonObject obj) {
        return obj.has("type")
                && "minecraft:condition".equals(asString(obj.get("type")))
                && obj.has("property")
                && NEEDS_REPAIR.equals(asString(obj.get("property")))
                && obj.has("on_false");
    }

    private static String asString(JsonElement element) {
        return element != null && element.isJsonPrimitive() ? element.getAsString() : null;
    }
}
