package magicjinn.artifactspatch.poly;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

// POLYMER WORKAROUND: BlockDisplay needs vanilla BlockStates; pack overrides rare coral blocks to mimic cuboid models.
public final class MimicDisplayBlocks {
	public static final BlockState BOTTOM_VISUAL = Blocks.DEAD_TUBE_CORAL_BLOCK.defaultBlockState();
	public static final BlockState LID_VISUAL = Blocks.DEAD_BRAIN_CORAL_BLOCK.defaultBlockState();

	private MimicDisplayBlocks() {
	}
}
