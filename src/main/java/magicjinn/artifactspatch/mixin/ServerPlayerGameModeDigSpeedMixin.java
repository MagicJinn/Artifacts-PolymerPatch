package magicjinn.artifactspatch.mixin;

import magicjinn.artifactspatch.poly.DiggingClawsPolymerSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// POLYMER WORKAROUND: Vanilla clients gate STOP_DESTROY_BLOCK on local mining math; server tick ignores
// incrementDestroyProgress's return value. Finish the break when server progress hits 1 for Polymer clients
// so digging claws speed / tool-tier bonuses actually apply (see DiggingClawsPolymerSupport).
@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeDigSpeedMixin {
	@Shadow
	@Final
	protected ServerPlayer player;

	@Shadow
	protected ServerLevel level;

	@Shadow
	private boolean isDestroyingBlock;

	@Shadow
	private BlockPos destroyPos;

	@Shadow
	private int destroyProgressStart;

	@Shadow
	private int gameTicks;

	@Shadow
	private int lastSentState;

	@Inject(method = "tick", at = @At("TAIL"))
	private void artifactsPatch$finishDestroyForPolymer(CallbackInfo ci) {
		if (!this.isDestroyingBlock) {
			return;
		}

		BlockState state = this.level.getBlockState(this.destroyPos);
		int elapsed = this.gameTicks - this.destroyProgressStart;
		if (!DiggingClawsPolymerSupport.shouldFinishDestroy(this.player, state, this.destroyPos, elapsed)) {
			return;
		}

		this.isDestroyingBlock = false;
		this.lastSentState = -1;
		DiggingClawsPolymerSupport.finishDestroy((ServerPlayerGameMode) (Object) this, this.player, this.level, this.destroyPos);
	}
}
