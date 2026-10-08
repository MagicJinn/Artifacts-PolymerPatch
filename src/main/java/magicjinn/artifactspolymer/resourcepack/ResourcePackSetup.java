package magicjinn.artifactspolymer.resourcepack;

import com.mojang.serialization.MapCodec;

import artifacts.Artifacts;
import artifacts.util.ItemDamageUtil;
import eu.pb4.polymer.resourcepack.extras.api.ResourcePackExtras;
import eu.pb4.polymer.resourcepack.extras.api.format.item.property.bool.BooleanProperty;
import eu.pb4.polymer.resourcepack.extras.api.format.item.ItemAsset;
import eu.pb4.polymer.resourcepack.extras.api.format.item.model.BasicItemModel;
import net.minecraft.world.item.ItemStack;

public class ResourcePackSetup {
    public static void setup() {
        // POLYMER WORKAROUND: Server-side bool property for pack generation. Vanilla
        // clients cannot load artifacts:needs_repair.
        BooleanProperty.TYPES.put(Artifacts.id("needs_repair"), ArtifactsNeedsRepairProperty.MAP_CODEC);

        // This adds a texture to all items in the Artifacts mod
        ResourcePackExtras extras = ResourcePackExtras.forDefault();
        extras.addBridgedModelsFolder(Artifacts.id("item"),
                (id, builder) -> new ItemAsset(new BasicItemModel(id), new ItemAsset.Properties(false, false)));

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

        // public static boolean test(ItemStack stack) {
        // return ItemDamageUtil.needsRepair(stack);
        // }
    }
}