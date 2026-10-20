package magicjinn.artifactspolymer.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
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
    private int lastSentState;

    @Unique
    private float artifactsPolymer$destroyProgress;

    @Unique
    private BlockPos artifactsPolymer$destroyTrackingPos;

    @Inject(method = "tick", at = @At("TAIL"))
    private void onServerPlayerGameModeTick(CallbackInfo info) {
        if (!isDestroyingBlock) {
            artifactsPolymer$destroyProgress = 0.0F;
            artifactsPolymer$destroyTrackingPos = null;
            return;
        }

        if (!PolymerClientChecks.lacksArtifactsClient(player))
            return;

        if (!ModDataComponents.TOOL_TIER_UPGRADE.on(player).findAny())
            return;

        BlockState state = level.getBlockState(destroyPos);
        if (state.isAir())
            return;

        if (artifactsPolymer$destroyTrackingPos == null || !artifactsPolymer$destroyTrackingPos.equals(destroyPos)) {
            artifactsPolymer$destroyTrackingPos = destroyPos.immutable();
            artifactsPolymer$destroyProgress = 0.0F;
        }

        artifactsPolymer$destroyProgress += state.getDestroyProgress(player, level, destroyPos);
        if (artifactsPolymer$destroyProgress < 1.0F)
            return;

        artifactsPolymer$destroyProgress = 0.0F;
        artifactsPolymer$destroyTrackingPos = null;
        isDestroyingBlock = false;
        lastSentState = -1;
        finishDestroy((ServerPlayerGameMode) (Object) this, player, level, destroyPos);
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
