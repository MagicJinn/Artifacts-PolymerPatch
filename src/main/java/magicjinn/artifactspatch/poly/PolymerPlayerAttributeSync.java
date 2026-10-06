package magicjinn.artifactspatch.poly;

import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.server.level.ServerPlayer;

// POLYMER WORKAROUND: Pushes attribute snapshots to Polymer/vanilla clients after Artifacts applies equipment
// modifiers server-side (no Artifacts client to mirror modifier application locally).
public final class PolymerPlayerAttributeSync {
	private PolymerPlayerAttributeSync() {
	}

	public static void sync(ServerPlayer player) {
		if (player.connection == null) {
			return;
		}
		player.connection.send(new ClientboundUpdateAttributesPacket(
				player.getId(),
				player.getAttributes().getSyncableAttributes()
		));
		// POLYMER WORKAROUND: Full sync restores real water_movement_efficiency; re-apply flippers spoof.
		SwimSpeedPolymerSupport.afterAttributeSync(player);
	}
}
