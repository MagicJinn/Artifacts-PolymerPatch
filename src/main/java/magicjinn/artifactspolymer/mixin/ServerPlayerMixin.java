package magicjinn.artifactspolymer.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import magicjinn.artifactspolymer.polymer.AquaDashersPolymerPatch;
import magicjinn.artifactspolymer.polymer.CloudInABottlePolymerPatch;
import magicjinn.artifactspolymer.polymer.HeliumFlamingoPolymerPatch;
import magicjinn.artifactspolymer.polymer.SnowshoesPolymerPatch;
import magicjinn.artifactspolymer.polymer.SteadfastSpikesPolymerPatch;
import magicjinn.artifactspolymer.polymer.StriderShoesPolymerPatch;
import net.minecraft.server.level.ServerPlayer;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin {
    // POLYMER WORKAROUND: Hook into player tick to fix server-client
    // desync/oddities
    @Inject(method = "tick", at = @At("TAIL"))
    private void onServerPlayerTick(CallbackInfo info) {
        ServerPlayer player = (ServerPlayer) (Object) this;

        AquaDashersPolymerPatch.tick(player);
        // CharmOfSinkingPolymerPatch.tick(player); // TODO
        CloudInABottlePolymerPatch.tick(player);
        HeliumFlamingoPolymerPatch.tick(player);
        SnowshoesPolymerPatch.tick(player);
        SteadfastSpikesPolymerPatch.tick(player);
        StriderShoesPolymerPatch.tick(player);
        // SwimSpeedPolymerSupport.tick(player); // TODO
    }
}
