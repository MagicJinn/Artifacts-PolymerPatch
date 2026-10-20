package magicjinn.artifactspolymer.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import magicjinn.artifactspolymer.ArtifactsPolymerPatch;
import magicjinn.artifactspolymer.polymer.AquaDashersPolymerPatch;
import magicjinn.artifactspolymer.polymer.CharmOfSinkingPolymerPatch;
import magicjinn.artifactspolymer.polymer.CloudInABottlePolymerPatch;
import magicjinn.artifactspolymer.polymer.HeliumFlamingoPolymerPatch;
import magicjinn.artifactspolymer.polymer.SnowshoesPolymerPatch;
import magicjinn.artifactspolymer.polymer.SteadfastSpikesPolymerPatch;
import magicjinn.artifactspolymer.polymer.StriderShoesPolymerPatch;
import magicjinn.artifactspolymer.polymer.SwimSpeedPolymerPatch;
import net.minecraft.server.level.ServerPlayer;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin {
    // POLYMER WORKAROUND: Hook into player tick to fix server-client
    // desync/oddities
    @Inject(method = "tick", at = @At("TAIL"))
    private void onServerPlayerTick(CallbackInfo info) {
        ServerPlayer player = (ServerPlayer) (Object) this;

        safeTick("aqua_dashers", () -> AquaDashersPolymerPatch.tick(player));
        safeTick("charm_of_sinking", () -> CharmOfSinkingPolymerPatch.tick(player));
        safeTick("cloud_in_a_bottle", () -> CloudInABottlePolymerPatch.tick(player));
        safeTick("helium_flamingo", () -> HeliumFlamingoPolymerPatch.tick(player));
        safeTick("snowshoes", () -> SnowshoesPolymerPatch.tick(player));
        safeTick("steadfast_spikes", () -> SteadfastSpikesPolymerPatch.tick(player));
        safeTick("strider_shoes", () -> StriderShoesPolymerPatch.tick(player));
        safeTick("swim_speed", () -> SwimSpeedPolymerPatch.tick(player));
    }

    private static void safeTick(String name, Runnable tick) {
        try {
            tick.run();
        } catch (Throwable t) {
            ArtifactsPolymerPatch.LOGGER.error("Polymer patch tick failed: {}", name, t);
        }
    }
}
