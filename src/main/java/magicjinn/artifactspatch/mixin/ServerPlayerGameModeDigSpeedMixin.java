package magicjinn.artifactspatch.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import magicjinn.artifactspatch.mixin.accessor.ServerPlayerGameModeAccessor;
import magicjinn.artifactspatch.poly.DiggingClawsPolymerSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeDigSpeedMixin {
	@ModifyReturnValue(method = "incrementDestroyProgress", at = @At("RETURN"))
	private float artifactsPatch$scaleDestroyProgressForPolymer(
			float progress,
			BlockState state,
			BlockPos pos,
			int startTick
	) {
		var player = ((ServerPlayerGameModeAccessor) this).artifactsPatch$getPlayer();
		return progress * DiggingClawsPolymerSupport.destroyProgressMultiplier(player, state);
	}
}
