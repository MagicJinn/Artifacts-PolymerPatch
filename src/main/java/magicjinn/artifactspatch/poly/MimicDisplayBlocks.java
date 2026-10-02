package magicjinn.artifactspatch.poly;

import magicjinn.artifactspatch.ArtifactsPolymerPatch;
import eu.pb4.polymer.blocks.api.BlockModelType;
import eu.pb4.polymer.blocks.api.PolymerBlockModel;
import eu.pb4.polymer.blocks.api.PolymerBlockResourceUtils;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

// POLYMER WORKAROUND: BlockDisplay on vanilla clients needs polymer-blocks virtual states, not world block overrides.
public final class MimicDisplayBlocks {
	private static BlockState bottomVisual = Blocks.AIR.defaultBlockState();
	private static BlockState lidVisual = Blocks.AIR.defaultBlockState();

	private MimicDisplayBlocks() {
	}

	public static void register() {
		BlockState bottom = PolymerBlockResourceUtils.requestBlock(
				BlockModelType.LEAVES,
				PolymerBlockModel.of(ArtifactsPolymerPatch.id("block/mimic_bottom"))
		);
		BlockState lid = PolymerBlockResourceUtils.requestBlock(
				BlockModelType.LEAVES,
				PolymerBlockModel.of(ArtifactsPolymerPatch.id("block/mimic_lid"))
		);
		if (bottom != null) {
			bottomVisual = bottom;
		} else {
			ArtifactsPolymerPatch.LOGGER.error("Failed to allocate polymer-blocks state for mimic bottom display");
		}
		if (lid != null) {
			lidVisual = lid;
		} else {
			ArtifactsPolymerPatch.LOGGER.error("Failed to allocate polymer-blocks state for mimic lid display");
		}
	}

	public static BlockState bottomVisual() {
		return bottomVisual;
	}

	public static BlockState lidVisual() {
		return lidVisual;
	}
}
