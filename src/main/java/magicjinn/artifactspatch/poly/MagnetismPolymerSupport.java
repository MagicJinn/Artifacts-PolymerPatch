package magicjinn.artifactspatch.poly;

import artifacts.extensions.mobeffect.magnetism.ItemEntityExtensions;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

// POLYMER WORKAROUND: Artifacts magnetism sets ItemEntity velocity in MobEffect.applyEffectTick, outside ItemEntity.tick
// where needsSync is normally raised. Vanilla/Polymer clients can keep ghost items at the drop position while the server
// moves and picks them up; on reconnect inventory already contains the items. We push motion/position/remove/take packets
// directly to viewers without the Artifacts client.
public final class MagnetismPolymerSupport {
	private static final int MAX_ITEMS_PER_TICK = 50;

	private MagnetismPolymerSupport() {
	}

	public static void syncPulledItems(ServerLevel level, ServerPlayer player, int amplifier) {
		if (!PolymerClientChecks.lacksArtifactsClient(player)) {
			return;
		}

		Vec3 center = player.position().add(0.0, 0.75, 0.0);
		int radius = Math.min(amplifier + 1, 10);
		AABB box = new AABB(
				center.x - radius,
				center.y - radius,
				center.z - radius,
				center.x + radius,
				center.y + radius,
				center.z + radius
		);

		int processed = 0;
		for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, box)) {
			if (!item.isAlive() || item.hasPickUpDelay()) {
				continue;
			}
			if (((ItemEntityExtensions) item).artifacts$wasThrownBy(player)) {
				continue;
			}
			if (++processed > MAX_ITEMS_PER_TICK) {
				break;
			}

			item.needsSync = true;
			player.connection.send(new ClientboundSetEntityMotionPacket(item.getId(), item.getDeltaMovement()));
			player.connection.send(ClientboundEntityPositionSyncPacket.of(item));
		}
	}

	public static void syncPickup(ServerPlayer player, ItemEntity item, int pickedCount) {
		if (!PolymerClientChecks.lacksArtifactsClient(player) || pickedCount <= 0) {
			return;
		}

		player.connection.send(new ClientboundTakeItemEntityPacket(item.getId(), player.getId(), pickedCount));
		if (!item.isAlive() || item.isRemoved()) {
			player.connection.send(new ClientboundRemoveEntitiesPacket(item.getId()));
		}
		player.inventoryMenu.sendAllDataToRemote();
	}
}
