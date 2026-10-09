package magicjinn.artifactspolymer.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import magicjinn.artifactspolymer.polymer.AquaDashersPolymerPatch;
import net.minecraft.server.level.ServerPlayer;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin {
    // POLYMER WORKAROUND: Hook into player tick to fix server-client
    // desync/oddities
    @Inject(method = "tick", at = @At("TAIL"))
    private void onServerPlayerTick(CallbackInfo info) {
        ServerPlayer player = (ServerPlayer) (Object) this;

        AquaDashersPolymerPatch.tick(player);
    }
}
