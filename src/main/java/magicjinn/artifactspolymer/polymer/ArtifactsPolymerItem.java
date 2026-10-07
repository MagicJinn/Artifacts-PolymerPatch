package magicjinn.artifactspolymer.polymer;

import artifacts.Artifacts;
import eu.pb4.polymer.core.api.item.VanillaModeledPolymerItem;
import eu.pb4.polymer.resourcepack.extras.api.ResourcePackExtras;
import magicjinn.artifactspolymer.resourcepack.HeldCuboidItemModels;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public record ArtifactsPolymerItem(Item item) implements VanillaModeledPolymerItem {
    private static final String ITEM_PATH = "item/";

    @Override
    public Item getPolymerItem(ItemStack stack, PacketContext context) {
        // POLYMER WORKAROUND: Umbrellas in Artifacts are tagged #minecraft:spears.
        // If not accounted for, the umbrella is held in a forwards manner, covering the
        // players face.
        if (HeldCuboidItemModels.UMBRELLA.equals(BuiltInRegistries.ITEM.getKey(item).getPath()))
            return Items.WOODEN_SPEAR;

        return Items.TRIAL_KEY;
    }

    @Override
    public Identifier getPolymerItemModel(ItemStack stack, PacketContext context, HolderLookup.Provider lookup) {
        var path = BuiltInRegistries.ITEM.getKey(item).getPath();
        // POLYMER WORKAROUND: The hand item model for artifacts lives at
        // assets/artifacts/items/<id>.json with the model id artifacts:<id>.
        // Using bridgeModel(artifacts:item/<id>) refers to the flat BasicItemModel
        // located in models/item only, not the one shown when held in hand.
        if (HeldCuboidItemModels.UMBRELLA.equals(path))
            return Artifacts.id(path);

        return ResourcePackExtras.bridgeModel(Artifacts.id(ITEM_PATH + path));
    }
}
