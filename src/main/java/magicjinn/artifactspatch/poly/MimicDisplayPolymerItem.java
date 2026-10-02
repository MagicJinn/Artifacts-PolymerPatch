package magicjinn.artifactspatch.poly;

import eu.pb4.polymer.core.api.item.VanillaModeledPolymerItem;
import eu.pb4.polymer.resourcepack.extras.api.ResourcePackExtras;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

// POLYMER WORKAROUND: Mimic lid/bottom ItemDisplay stacks use Artifacts mimic.png models from the generated pack.
public record MimicDisplayPolymerItem(Item item, Identifier modelId) implements VanillaModeledPolymerItem {
	@Override
	public Item getPolymerItem(ItemStack itemStack, PacketContext packetContext) {
		// POLYMER WORKAROUND: Trial key generated item layer drew on top of custom cuboids; keep items invisible if used.
		return Items.BARRIER;
	}

	@Override
	public Identifier getPolymerItemModel(ItemStack stack, PacketContext context, HolderLookup.Provider lookup) {
		return ResourcePackExtras.bridgeModel(modelId);
	}
}
