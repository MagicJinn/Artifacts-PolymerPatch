package magicjinn.artifactspatch.poly;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.level.block.state.BlockState;

// POLYMER WORKAROUND: Vanilla clients decide when to send STOP_DESTROY_BLOCK from local destroy progress
// (BLOCK_BREAK_SPEED + hasCorrectToolForDrops /30 vs /100). Artifacts applies digging claws on the server
// only, so Polymer clients never finish early. Finish the break server-side once server progress reaches 1.
public final class DiggingClawsPolymerSupport {
	private DiggingClawsPolymerSupport() {
	}

	public static boolean shouldFinishDestroy(ServerPlayer player, BlockState state, BlockPos pos, int elapsedTicks) {
		if (!PolymerClientChecks.lacksArtifactsClient(player) || state.isAir()) {
			return false;
		}
		float progress = state.getDestroyProgress(player, player.level(), pos) * (elapsedTicks + 1);
		return progress >= 1.0F;
	}

	public static void finishDestroy(
			ServerPlayerGameMode gameMode,
			ServerPlayer player,
			ServerLevel level,
			BlockPos pos
	) {
		level.destroyBlockProgress(player.getId(), pos, -1);
		gameMode.destroyBlock(pos);
	}
}
