package dev.magicjinn.artifactspatch.poly;

import eu.pb4.polymer.core.api.item.VanillaModeledPolymerItem;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Presents Artifacts items to non-Artifacts clients using vanilla item types plus
 * Artifacts models from the generated Polymer resource pack.
 */
public record PolyArtifactsItem(Item item) implements VanillaModeledPolymerItem {
	@Override
	public Item getPolymerItem(ItemStack itemStack, PacketContext packetContext) {
		return Items.TRIAL_KEY;
	}
}
