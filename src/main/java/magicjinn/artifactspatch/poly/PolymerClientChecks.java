package magicjinn.artifactspatch.poly;

import artifacts.network.payload.UpdateConfigValuePacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

public final class PolymerClientChecks {
	private PolymerClientChecks() {
	}

	// POLYMER WORKAROUND: Artifacts registers UpdateConfigValuePacket only when its client mod is present;
	// canSend is our stand-in for "vanilla / Polymer-only client".
	public static boolean lacksArtifactsClient(ServerPlayer player) {
		return !ServerPlayNetworking.canSend(player, UpdateConfigValuePacket.TYPE);
	}
}
