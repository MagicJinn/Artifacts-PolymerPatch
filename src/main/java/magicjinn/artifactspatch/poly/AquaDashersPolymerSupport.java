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
import net.minecraft.world.phys.Vec3;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Invisible support boat under Polymer players while aqua-dashers water sprinting.
 * Gives the vanilla client a predictable surface to stand on (Artifacts fluid collision is server-only).
 */
public final class AquaDashersPolymerSupport {
	private static final Map<UUID, Boat> SUPPORT_BOATS = new ConcurrentHashMap<>();

	private AquaDashersPolymerSupport() {
	}

	public static void tick(ServerPlayer player) {
		if (!PolymerClientChecks.lacksArtifactsClient(player)) {
			clearSupport(player.getUUID());
			return;
		}

		if (!isWaterSprinting(player)) {
			clearSupport(player.getUUID());
			return;
		}

		ServerLevel level = (ServerLevel) player.level();
		Boat boat = SUPPORT_BOATS.computeIfAbsent(player.getUUID(), id -> spawnSupportBoat(level, player));
		if (boat.isRemoved() || boat.level() != level) {
			SUPPORT_BOATS.put(player.getUUID(), spawnSupportBoat(level, player));
			boat = SUPPORT_BOATS.get(player.getUUID());
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
				iterator.remove();
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
		boat.addTag(ArtifactsPolymerPatch.id("aqua_dashers_support").toString());
		level.addFreshEntity(boat);
		return boat;
	}

	private static void syncSupportBoat(ServerPlayer player, Boat boat) {
		FluidState fluid = fluidAtFeet(player);
		if (fluid == null || fluid.isEmpty()) {
			return;
		}

		BlockPos blockPos = BlockPos.containing(player.getX(), player.getY(), player.getZ());
		double surfaceY = blockPos.getY() + fluid.getHeight(player.level(), blockPos);
		Vec3 motion = player.getDeltaMovement();

		boat.setPos(player.getX(), surfaceY - 0.45, player.getZ());
		boat.setYRot(player.getYRot());
		boat.setXRot(0.0F);
		boat.setDeltaMovement(motion.x, 0, motion.z);
		boat.setPaddleState(false, false);
	}

	private static void clearSupport(UUID playerId) {
		Boat boat = SUPPORT_BOATS.remove(playerId);
		if (boat != null && !boat.isRemoved()) {
			boat.discard();
		}
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
}
