package magicjinn.artifactspolymer.polymer;

import artifacts.network.payload.UpdateConfigValuePacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

/**
 * Detects whether a player has the Artifacts client mod. This patch is
 * Polymer / vanilla-client only. Artifacts clients are kicked on join.
 */
public final class PolymerClientChecks {
    private PolymerClientChecks() {
    }

    // Artifacts registers UpdateConfigValuePacket only when its client mod is present.
    public static boolean lacksArtifactsClient(ServerPlayer player) {
        return !hasArtifactsClient(player);
    }

    public static boolean hasArtifactsClient(ServerPlayer player) {
        if (player.connection == null)
            return false;
        try {
            return ServerPlayNetworking.canSend(player, UpdateConfigValuePacket.TYPE);
        } catch (NullPointerException e) {
            // Play addon not attached yet.
            return false;
        }
    }
}
