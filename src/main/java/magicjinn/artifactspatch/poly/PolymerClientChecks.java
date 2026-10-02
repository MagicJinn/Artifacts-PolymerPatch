package magicjinn.artifactspatch.poly;

import artifacts.network.payload.UpdateConfigValuePacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

public final class PolymerClientChecks {
	private PolymerClientChecks() {
	}

	public static boolean lacksArtifactsClient(ServerPlayer player) {
		return !ServerPlayNetworking.canSend(player, UpdateConfigValuePacket.TYPE);
	}
}
