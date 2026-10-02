package magicjinn.artifactspatch.res;

import artifacts.util.ItemDamageUtil;
import com.mojang.serialization.MapCodec;
import eu.pb4.polymer.resourcepack.extras.api.format.item.property.bool.BooleanProperty;
import net.minecraft.world.item.ItemStack;

// POLYMER WORKAROUND: Server-side stand-in for Artifacts' artifacts:needs_repair client item model property.
public record ArtifactsNeedsRepairProperty() implements BooleanProperty {
	public static final MapCodec<ArtifactsNeedsRepairProperty> MAP_CODEC = MapCodec.unit(new ArtifactsNeedsRepairProperty());

	@Override
	public MapCodec<? extends BooleanProperty> codec() {
		return MAP_CODEC;
	}

	public static boolean test(ItemStack stack) {
		return ItemDamageUtil.needsRepair(stack);
	}
}
