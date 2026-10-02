package magicjinn.artifactspatch.poly;

import artifacts.entity.MimicEntity;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.elements.BlockDisplayElement;
import eu.pb4.polymer.virtualentity.api.elements.InteractionElement;
import net.minecraft.world.entity.Display;
import net.minecraft.world.level.block.Blocks;

// POLYMER WORKAROUND: Static vanilla chest BlockDisplay placeholder until a real mimic model path is proven.
public final class MimicPolymerChestPlaceholder extends ElementHolder {
	public MimicPolymerChestPlaceholder(MimicEntity mimic) {
		var hitbox = InteractionElement.redirect(mimic);
		hitbox.setSize(MimicPolymerEntity.MIMIC_HITBOX_SIZE, MimicPolymerEntity.MIMIC_HITBOX_SIZE);
		hitbox.setResponse(true);
		addElement(hitbox);

		var chest = new BlockDisplayElement(Blocks.CHEST.defaultBlockState());
		chest.setBillboardMode(Display.BillboardConstraints.FIXED);
		addElement(chest);
	}
}
