package magicjinn.artifactspolymer.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import artifacts.registry.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import magicjinn.artifactspolymer.polymer.PolymerClientChecks;
import net.minecraft.world.level.block.state.BlockState;

// POLYMER WORKAROUND: Vanilla clients gate STOP_DESTROY_BLOCK on local mining math. Server tick ignores
// incrementDestroyProgress's return value. When Digging Claws are equipped, finish the break when server
// progress hits 1 so their speed / tool-tier bonuses actually apply for Polymer clients.
@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {
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
    private void onServerPlayerGameModeTick(CallbackInfo info) {
        if (!isDestroyingBlock)
            return;

        BlockState state = level.getBlockState(destroyPos);
        int elapsed = gameTicks - destroyProgressStart;
        if (!shouldFinishDestroy(player, state, destroyPos, elapsed))
            return;

        isDestroyingBlock = false;
        lastSentState = -1;
        finishDestroy((ServerPlayerGameMode) (Object) this, player, level, destroyPos);
    }

    private static boolean shouldFinishDestroy(ServerPlayer player, BlockState state, BlockPos pos, int elapsedTicks) {
        if (!PolymerClientChecks.lacksArtifactsClient(player) || state.isAir())
            return false;

        if (!ModDataComponents.TOOL_TIER_UPGRADE.on(player).findAny())
            return false;

        float progress = state.getDestroyProgress(player, player.level(), pos) * (elapsedTicks + 1);
        return progress >= 1.0F;
    }

    /** Finish the destroy block serverside */
    private static void finishDestroy(
            ServerPlayerGameMode gameMode,
            ServerPlayer player,
            ServerLevel level,
            BlockPos pos) {
        level.destroyBlockProgress(player.getId(), pos, -1);
        gameMode.destroyBlock(pos);
    }
}
