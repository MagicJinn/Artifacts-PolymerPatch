package magicjinn.artifactspatch.poly;

import eu.pb4.polymer.core.api.entity.PolymerEntity;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.boat.Boat;

import java.util.UUID;

/**
 * Support boats are only tracked on the owning Polymer player's client.
 */
public final class AquaDashersSupportBoatPolymer implements PolymerEntity {
	private final Boat boat;
	private final UUID ownerId;

	public AquaDashersSupportBoatPolymer(Boat boat, UUID ownerId) {
		this.boat = boat;
		this.ownerId = ownerId;
	}

	public static PolymerEntity tryCreate(Boat boat) {
		UUID owner = AquaDashersPolymerSupport.getSupportOwner(boat);
		if (owner == null) {
			return null;
		}
		return new AquaDashersSupportBoatPolymer(boat, owner);
	}

	@Override
	public EntityType<?> getPolymerEntityType(PacketContext context) {
		return EntityType.OAK_BOAT;
	}

	@Override
	public boolean sendPacketsTo(ServerPlayer player) {
		return player.getUUID().equals(ownerId);
	}

	@Override
	public boolean sendEmptyTrackerUpdates(ServerPlayer player) {
		return player.getUUID().equals(ownerId);
	}
}
