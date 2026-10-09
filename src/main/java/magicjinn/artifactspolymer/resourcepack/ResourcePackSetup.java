package magicjinn.artifactspolymer.resourcepack;

import com.mojang.serialization.MapCodec;

import artifacts.Artifacts;
import artifacts.util.ItemDamageUtil;
import eu.pb4.polymer.resourcepack.api.PackResource;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.polymer.resourcepack.extras.api.ResourcePackExtras;
import eu.pb4.polymer.resourcepack.extras.api.format.item.property.bool.BooleanProperty;
import eu.pb4.polymer.resourcepack.extras.api.format.item.ItemAsset;
import eu.pb4.polymer.resourcepack.extras.api.format.item.model.BasicItemModel;
import magicjinn.artifactspolymer.ArtifactsPolymerPatch;
import net.minecraft.world.item.ItemStack;

public class ResourcePackSetup {

    private static final String ARTIFACTS_ITEMS_PREFIX = "assets/artifacts/items/";

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
        BooleanProperty.TYPES.put(Artifacts.id("needs_repair"), ArtifactsNeedsRepairProperty.MAP_CODEC);

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
        resource = stripNeedsRepairItemModels(path, resource);
        return resource;
    }

    // POLYMER WORKAROUND: Artifacts item models reference artifacts:needs_repair.
    // Rewrite to remove the property.
    private static PackResource stripNeedsRepairItemModels(String path, PackResource resource) {
        if (!path.startsWith(ARTIFACTS_ITEMS_PREFIX) || !path.endsWith(".json"))
            return resource;

        if (!resource.asString().contains("artifacts:needs_repair"))
            return resource;

        String itemId = path.substring(ARTIFACTS_ITEMS_PREFIX.length(), path.length() - ".json".length());
        // if (HeldCuboidItemModels.UMBRELLA.equals(itemId)) { // TODO: Add this back in
        // return PackResource.fromString(umbrellaDisplayContextItemModel());
        // }

        return PackResource.fromString(flatIntactItemAsset(itemId));
    }

    private static String flatIntactItemAsset(String itemId) {
        return ITEM_ASSET.formatted(itemId);
    }
}