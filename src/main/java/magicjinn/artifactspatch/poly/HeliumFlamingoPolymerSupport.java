package magicjinn.artifactspatch.poly;

import artifacts.component.SwimData;
import artifacts.component.ability.SwimInAir;
import artifacts.platform.PlatformServices;
import artifacts.registry.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * POLYMER WORKAROUND: Helium Flamingo activation is client-only (SwimInAirInputHooks +
 * UpdateSwimFlyingPacket). Vanilla clients never send that packet (gated). Default key falls back
 * to sprint, so mirror the press edge with getLastClientInput().sprint().
 * <p>
 * Mid-air source water is wiped by client fluid ticks. Use per-viewer waterlogged light blocks
 * (no collision, fluid stays) resent every tick, plus a look-based swim impulse + motion packet.
 * Do not cancel client move packets or teleport-spam — that froze Polymer players.
 */
public final class HeliumFlamingoPolymerSupport {
	private static final Map<UUID, Boolean> WAS_SPRINT = new HashMap<>();
	private static final Map<UUID, Set<BlockPos>> FAKE_FLUID = new HashMap<>();
	private static final Map<UUID, ServerBossEvent> CHARGE_BARS = new HashMap<>();
	/** ~5 b/s look-swim when client prediction still lags. */
	private static final double SWIM_IMPULSE = 0.25;
	private static final BlockState FAKE_FLUID_STATE = Blocks.LIGHT.defaultBlockState()
			.setValue(LightBlock.LEVEL, 0)
			.setValue(BlockStateProperties.WATERLOGGED, true);
	private static final Component NAME_DRAINING = Component.literal("Helium Flamingo");
	private static final Component NAME_RECHARGING = Component.literal("Helium Flamingo (recharging)");

	private HeliumFlamingoPolymerSupport() {
	}

	public static void tick(ServerPlayer player) {
		updateChargeBossBar(player);

		if (!PolymerClientChecks.lacksArtifactsClient(player)) {
			clearPhysics(player);
			return;
		}
		if (!ModDataComponents.SWIM_IN_AIR.on(player).findAny()) {
			clearPhysics(player);
			return;
		}

		SwimData swimData = PlatformServices.getPlatformHelper().getSwimData(player);
		if (swimData == null) {
			clearPhysics(player);
			return;
		}

		handleActivation(player, swimData);
		syncIfFlying(player, swimData);
	}

	public static void onDisconnect(ServerPlayer player) {
		clearPhysics(player);
		removeChargeBossBar(player);
	}

	private static void clearPhysics(ServerPlayer player) {
		WAS_SPRINT.remove(player.getUUID());
		clearFakeFluid(player);
	}

	/** Show while worn and charge is draining or recharging; hide when full and idle. */
	private static void updateChargeBossBar(ServerPlayer player) {
		if (!ModDataComponents.SWIM_IN_AIR.on(player).findAny()) {
			removeChargeBossBar(player);
			return;
		}
		SwimData swimData = PlatformServices.getPlatformHelper().getSwimData(player);
		if (swimData == null) {
			removeChargeBossBar(player);
			return;
		}

		boolean draining = swimData.shouldDepleteSwimFlyingCharge(player) && !player.isCreative();
		float charge = (float) Mth.clamp(swimData.getSwimFlyingCharge(), 0.0, 1.0);
		boolean recharging = !draining && charge < 0.999f;
		if (!draining && !recharging) {
			removeChargeBossBar(player);
			return;
		}

		ServerBossEvent bar = CHARGE_BARS.computeIfAbsent(player.getUUID(), _ -> {
			ServerBossEvent created = new ServerBossEvent(
					UUID.randomUUID(),
					NAME_DRAINING,
					BossEvent.BossBarColor.PINK,
					BossEvent.BossBarOverlay.PROGRESS);
			created.setVisible(true);
			created.addPlayer(player);
			return created;
		});
		bar.setProgress(charge);
		if (draining) {
			bar.setName(NAME_DRAINING);
			bar.setColor(BossEvent.BossBarColor.PINK);
		} else {
			bar.setName(NAME_RECHARGING);
			bar.setColor(BossEvent.BossBarColor.BLUE);
		}
	}

	private static void removeChargeBossBar(ServerPlayer player) {
		ServerBossEvent bar = CHARGE_BARS.remove(player.getUUID());
		if (bar == null) {
			return;
		}
		bar.removePlayer(player);
		bar.setVisible(false);
	}

	private static void handleActivation(ServerPlayer player, SwimData swimData) {
		boolean sprint = player.getLastClientInput().sprint();
		boolean wasSprint = WAS_SPRINT.getOrDefault(player.getUUID(), false);
		WAS_SPRINT.put(player.getUUID(), sprint);

		if (!sprint || wasSprint) {
			return;
		}

		if (swimData.isSwimFlying()) {
			if (player.isSwimming() && swimData.shouldDepleteSwimFlyingCharge(player)) {
				swimData.toggleSwimFlying(player);
			}
		} else if (SwimInAir.canSwim(player)) {
			swimData.toggleSwimFlying(player);
		}
	}

	private static void syncIfFlying(ServerPlayer player, SwimData swimData) {
		if (!swimData.isSwimFlying()) {
			clearFakeFluid(player);
			return;
		}

		player.setSwimming(true);

		if (!CharmOfSinkingPolymerSupport.inWorldWater(player)) {
			updateFakeFluid(player);
			applyLookSwimImpulse(player);
		} else {
			clearFakeFluid(player);
		}

		if (player.connection != null) {
			player.connection.send(new ClientboundSetEntityMotionPacket(player));
		}
	}

	private static void applyLookSwimImpulse(ServerPlayer player) {
		Input input = player.getLastClientInput();
		float forward = (input.forward() ? 1.0f : 0.0f) - (input.backward() ? 1.0f : 0.0f);
		float strafe = (input.left() ? 1.0f : 0.0f) - (input.right() ? 1.0f : 0.0f);
		boolean up = input.jump();
		boolean down = input.shift();
		if (forward == 0.0f && strafe == 0.0f && !up && !down) {
			return;
		}

		float yaw = player.getYRot() * Mth.DEG_TO_RAD;
		float pitch = player.getXRot() * Mth.DEG_TO_RAD;
		// Sprint-swim style: forward follows look; strafe is yaw-relative.
		double lookX = -Mth.sin(yaw) * Mth.cos(pitch);
		double lookY = -Mth.sin(pitch);
		double lookZ = Mth.cos(yaw) * Mth.cos(pitch);
		double flatX = -Mth.sin(yaw);
		double flatZ = Mth.cos(yaw);
		double rightX = flatZ;
		double rightZ = -flatX;

		Vec3 wish = new Vec3(
				lookX * forward + rightX * strafe,
				lookY * forward + (up ? 1.0 : 0.0) - (down ? 1.0 : 0.0),
				lookZ * forward + rightZ * strafe);
		if (wish.lengthSqr() < 1.0e-6) {
			return;
		}
		wish = wish.normalize().scale(SWIM_IMPULSE);
		player.setDeltaMovement(wish);
	}

	private static void updateFakeFluid(ServerPlayer player) {
		if (player.connection == null) {
			return;
		}
		Set<BlockPos> target = fluidShell(player);
		Set<BlockPos> previous = FAKE_FLUID.getOrDefault(player.getUUID(), Set.of());

		for (BlockPos pos : previous) {
			if (!target.contains(pos)) {
				sendRealBlock(player, pos);
			}
		}
		// Resend every tick so client fluid ticks cannot strip waterlogging.
		for (BlockPos pos : target) {
			player.connection.send(new ClientboundBlockUpdatePacket(pos, FAKE_FLUID_STATE));
		}

		if (target.isEmpty()) {
			FAKE_FLUID.remove(player.getUUID());
		} else {
			FAKE_FLUID.put(player.getUUID(), new HashSet<>(target));
		}
	}

	private static void clearFakeFluid(ServerPlayer player) {
		Set<BlockPos> previous = FAKE_FLUID.remove(player.getUUID());
		if (previous == null) {
			return;
		}
		for (BlockPos pos : previous) {
			sendRealBlock(player, pos);
		}
	}

	private static Set<BlockPos> fluidShell(ServerPlayer player) {
		AABB box = player.getBoundingBox().inflate(0.5, 0.25, 0.5);
		Set<BlockPos> positions = new HashSet<>();
		ServerLevel level = (ServerLevel) player.level();
		BlockPos.betweenClosedStream(box).forEach(pos -> {
			BlockState state = level.getBlockState(pos);
			if (state.isAir() || state.canBeReplaced()) {
				positions.add(pos.immutable());
			}
		});
		return positions;
	}

	private static void sendRealBlock(ServerPlayer player, BlockPos pos) {
		if (player.connection == null) {
			return;
		}
		BlockState state = ((ServerLevel) player.level()).getBlockState(pos);
		player.connection.send(new ClientboundBlockUpdatePacket(pos, state));
	}
}
