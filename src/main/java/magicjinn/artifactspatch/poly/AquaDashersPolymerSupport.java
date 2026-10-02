package magicjinn.artifactspatch.poly;

import artifacts.component.SwimData;
import artifacts.platform.PlatformServices;
import artifacts.registry.ModDataComponents;
import magicjinn.artifactspatch.ArtifactsPolymerPatch;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Invisible support boat under Polymer players while aqua-dashers water sprinting.
 * Only the owning player receives entity packets (see {@link AquaDashersSupportBoatPolymer}).
 */
public final class AquaDashersPolymerSupport {
	public static final String SUPPORT_BOAT_TAG = ArtifactsPolymerPatch.id("aqua_dashers_support").toString();

	/** Deck top ~1 pixel above the fluid surface. */
	private static final double DECK_ABOVE_SURFACE = 1.0 / 16.0;
	private static final int DISCARD_AFTER_TICKS_WITHOUT_SPRINT = 15;

	private static final Map<UUID, Boat> SUPPORT_BOATS = new ConcurrentHashMap<>();
	private static final Map<Integer, UUID> BOAT_ENTITY_OWNERS = new ConcurrentHashMap<>();
	private static final Map<UUID, Integer> TICKS_WITHOUT_SPRINT = new ConcurrentHashMap<>();

	private AquaDashersPolymerSupport() {
	}

	public static UUID getSupportOwner(Boat boat) {
		if (!boat.entityTags().contains(SUPPORT_BOAT_TAG)) {
			return null;
		}
		return BOAT_ENTITY_OWNERS.get(boat.getId());
	}

	public static void tick(ServerPlayer player) {
		if (!PolymerClientChecks.lacksArtifactsClient(player)) {
			clearSupport(player.getUUID());
			return;
		}

		if (!isWaterSprinting(player)) {
			int idleTicks = TICKS_WITHOUT_SPRINT.merge(player.getUUID(), 1, Integer::sum);
			if (idleTicks >= DISCARD_AFTER_TICKS_WITHOUT_SPRINT) {
				clearSupport(player.getUUID());
			} else {
				Boat boat = SUPPORT_BOATS.get(player.getUUID());
				if (boat != null && !boat.isRemoved()) {
					syncSupportBoat(player, boat);
				}
			}
			return;
		}

		TICKS_WITHOUT_SPRINT.put(player.getUUID(), 0);

		ServerLevel level = (ServerLevel) player.level();
		Boat boat = SUPPORT_BOATS.get(player.getUUID());
		if (boat == null || boat.isRemoved() || boat.level() != level) {
			if (boat != null) {
				unregisterBoat(boat);
			}
			boat = spawnSupportBoat(level, player);
			SUPPORT_BOATS.put(player.getUUID(), boat);
			BOAT_ENTITY_OWNERS.put(boat.getId(), player.getUUID());
		}

		syncSupportBoat(player, boat);
	}

	public static void onDisconnect(ServerPlayer player) {
		clearSupport(player.getUUID());
	}

	public static void clearOrphanedBoats(ServerLevel level) {
		Iterator<Map.Entry<UUID, Boat>> iterator = SUPPORT_BOATS.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, Boat> entry = iterator.next();
			Boat boat = entry.getValue();
			if (boat.isRemoved() || boat.level() != level) {
				unregisterBoat(boat);
				iterator.remove();
				TICKS_WITHOUT_SPRINT.remove(entry.getKey());
			}
		}
	}

	public static boolean isWaterSprinting(Player player) {
		if (!(player instanceof LivingEntity living)) {
			return false;
		}
		if (!player.isSprinting()) {
			return false;
		}
		SwimData swimData = PlatformServices.getPlatformHelper().getSwimData(living);
		if (swimData == null || swimData.shouldBreakSurfaceTension()) {
			return false;
		}
		if (!ModDataComponents.FLUID_COLLISION.on(living).findAny()) {
			return false;
		}
		FluidState fluid = fluidAtFeet(player);
		return fluid != null && fluid.is(FluidTags.WATER);
	}

	private static Boat spawnSupportBoat(ServerLevel level, ServerPlayer owner) {
		Boat boat = new Boat(EntityType.OAK_BOAT, level, () -> Items.OAK_BOAT);
		boat.setInvisible(true);
		boat.setSilent(true);
		boat.setInvulnerable(true);
		boat.setNoGravity(true);
		boat.addTag(SUPPORT_BOAT_TAG);
		level.addFreshEntity(boat);
		return boat;
	}

	private static void syncSupportBoat(ServerPlayer player, Boat boat) {
		WaterSurface surface = waterSurfaceAt(player);
		if (surface == null) {
			return;
		}

		Vec3 motion = player.getDeltaMovement();
		alignBoatDeck(boat, player.getX(), player.getZ(), surface.y);
		boat.setYRot(player.getYRot());
		boat.setXRot(0.0F);
		boat.setDeltaMovement(motion.x, 0, motion.z);
		boat.setPaddleState(false, false);
	}

	private static void alignBoatDeck(Boat boat, double x, double z, double surfaceY) {
		double targetTop = surfaceY + DECK_ABOVE_SURFACE;
		boat.setPos(x, surfaceY, z);
		AABB box = boat.getBoundingBox();
		double adjust = targetTop - box.maxY;
		if (Math.abs(adjust) > 1.0E-4) {
			boat.setPos(x, boat.getY() + adjust, z);
		}
	}

	private static WaterSurface waterSurfaceAt(Player player) {
		FluidState fluid = fluidAtFeet(player);
		if (fluid == null || fluid.isEmpty()) {
			return null;
		}
		BlockPos fluidPos = BlockPos.containing(player.getX(), player.getY() - 0.2, player.getZ());
		if (player.level().getFluidState(fluidPos).isEmpty()) {
			fluidPos = player.blockPosition();
			fluid = player.level().getFluidState(fluidPos);
			if (fluid.isEmpty()) {
				return null;
			}
		}
		double y = fluidPos.getY() + fluid.getHeight(player.level(), fluidPos);
		return new WaterSurface(y);
	}

	private static void clearSupport(UUID playerId) {
		TICKS_WITHOUT_SPRINT.remove(playerId);
		Boat boat = SUPPORT_BOATS.remove(playerId);
		if (boat != null && !boat.isRemoved()) {
			unregisterBoat(boat);
			boat.discard();
		}
	}

	private static void unregisterBoat(Boat boat) {
		BOAT_ENTITY_OWNERS.remove(boat.getId());
	}

	private static FluidState fluidAtFeet(Player player) {
		BlockPos pos = BlockPos.containing(player.getX(), player.getY() - 0.2, player.getZ());
		FluidState fluid = player.level().getFluidState(pos);
		if (!fluid.isEmpty()) {
			return fluid;
		}
		pos = player.blockPosition();
		return player.level().getFluidState(pos);
	}

	private record WaterSurface(double y) {
	}
}
