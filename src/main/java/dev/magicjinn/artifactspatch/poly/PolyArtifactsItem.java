package dev.magicjinn.artifactspatch.poly;

import artifacts.Artifacts;
import eu.pb4.polymer.core.api.item.VanillaModeledPolymerItem;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
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

	@Override
	public Identifier getPolymerItemModel(ItemStack stack, PacketContext context, HolderLookup.Provider lookup) {
		return Artifacts.id("item/" + BuiltInRegistries.ITEM.getKey(item).getPath());
	}
}
