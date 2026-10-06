package magicjinn.artifactspatch.poly;

import artifacts.component.SwimData;
import artifacts.platform.PlatformServices;
import artifacts.registry.ModDataComponents;
import magicjinn.artifactspatch.ArtifactsPolymerPatch;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * POLYMER WORKAROUND: Charm of Sinking is ExpandAbility FAIL (server dry-land travel). Vanilla clients
 * stay in travelInWater, so motion packets alone stay crawl-speed (~0.4 b/s) as water drag eats them.
 * Spoof full WME + enough MOVEMENT_SPEED that water terminal velocity matches land walk/sprint, raise
 * GRAVITY to fight buoyancy, keep step_height 1, and clear swim each tick.
 */
public final class CharmOfSinkingPolymerSupport {
	private static final Identifier STEP_ID = ArtifactsPolymerPatch.id("polymer_sinking_step");
	private static final Identifier MS_ID = ArtifactsPolymerPatch.id("polymer_sinking_ms");
	private static final Identifier WME_ID = ArtifactsPolymerPatch.id("polymer_sinking_wme");
	private static final Identifier GRAV_ID = ArtifactsPolymerPatch.id("polymer_sinking_grav");
	private static final double STEP_TARGET = 1.0;
	private static final double WME_DRAG_TARGET = 0.54600006;
	private static final double BASE_SWIM_ACCEL = 0.02;
	/** Vanilla land walk / sprint blocks-per-second targets for water-terminal calibration. */
	private static final double WALK_BPS = 4.317;
	private static final double SPRINT_BPS = 5.612;
	/** Default gravity is 0.08; raise so client sinks instead of floating. */
	private static final double GRAVITY_TARGET = 0.20;

	private static final Set<UUID> ACTIVE = new HashSet<>();

	private CharmOfSinkingPolymerSupport() {
	}

	public static void tick(ServerPlayer player) {
		if (!PolymerClientChecks.lacksArtifactsClient(player)) {
			clear(player);
			return;
		}
		if (!ModDataComponents.SINKING.on(player).findAny()) {
			clear(player);
			return;
		}
		SwimData swimData = PlatformServices.getPlatformHelper().getSwimData(player);
		if (swimData != null && swimData.isSwimFlying()) {
			clear(player);
			return;
		}
		if (!inWorldWater(player)) {
			clear(player);
			return;
		}

		player.setSwimming(false);
		applyStepHeight(player);
		sendSpeedSpoof(player, computeSpoof(player));

		if (player.connection != null) {
			player.connection.send(new ClientboundSetEntityMotionPacket(player));
		}
		ACTIVE.add(player.getUUID());
	}

	public static void onDisconnect(ServerPlayer player) {
		clear(player);
	}

	public static void afterAttributeSync(ServerPlayer player) {
		tick(player);
	}

	private static Spoof computeSpoof(ServerPlayer player) {
		boolean swimmingPath = !player.onGround();
		double groundFactor = swimmingPath ? 0.5 : 1.0;
		double e = groundFactor; // WME packet = 1.0
		double d0 = player.isSprinting() ? 0.9 : 0.8;
		double d = d0 + (WME_DRAG_TARGET - d0) * e;
		double targetV = (player.isSprinting() ? SPRINT_BPS : WALK_BPS) / 20.0;
		double a = targetV * (1.0 - d) / Math.max(1e-6, d);
		double ms = e <= 1e-6
				? targetV
				: BASE_SWIM_ACCEL + (a - BASE_SWIM_ACCEL) / e;
		return new Spoof(1.0, Mth.clamp(ms, 0.0, 1024.0), GRAVITY_TARGET);
	}

	private static void sendSpeedSpoof(ServerPlayer player, Spoof spoof) {
		if (player.connection == null) {
			return;
		}
		AttributeInstance realMs = player.getAttribute(Attributes.MOVEMENT_SPEED);
		AttributeInstance realWme = player.getAttribute(Attributes.WATER_MOVEMENT_EFFICIENCY);
		AttributeInstance realGrav = player.getAttribute(Attributes.GRAVITY);
		if (realMs == null || realWme == null || realGrav == null) {
			return;
		}
		List<AttributeInstance> packet = new ArrayList<>(3);
		packet.add(copyWithTotal(realWme, spoof.wme(), WME_ID));
		packet.add(copyWithTotal(realMs, spoof.movementSpeed(), MS_ID));
		packet.add(copyWithTotal(realGrav, spoof.gravity(), GRAV_ID));
		player.connection.send(new ClientboundUpdateAttributesPacket(player.getId(), packet));
	}

	private static void applyStepHeight(ServerPlayer player) {
		AttributeInstance step = player.getAttribute(Attributes.STEP_HEIGHT);
		if (step == null) {
			return;
		}
		step.removeModifier(STEP_ID);
		double delta = STEP_TARGET - step.getValue();
		if (Math.abs(delta) > 1e-6) {
			step.addTransientModifier(new AttributeModifier(STEP_ID, delta, AttributeModifier.Operation.ADD_VALUE));
		}
	}

	private static void clear(ServerPlayer player) {
		AttributeInstance step = player.getAttribute(Attributes.STEP_HEIGHT);
		if (step != null) {
			step.removeModifier(STEP_ID);
		}
		if (!ACTIVE.remove(player.getUUID())) {
			return;
		}
		if (player.connection == null) {
			return;
		}
		AttributeInstance realMs = player.getAttribute(Attributes.MOVEMENT_SPEED);
		AttributeInstance realWme = player.getAttribute(Attributes.WATER_MOVEMENT_EFFICIENCY);
		AttributeInstance realGrav = player.getAttribute(Attributes.GRAVITY);
		if (realMs == null || realWme == null || realGrav == null) {
			return;
		}
		player.connection.send(new ClientboundUpdateAttributesPacket(
				player.getId(), List.of(realWme, realMs, realGrav)));
	}

	private static AttributeInstance copyWithTotal(AttributeInstance real, double targetTotal, Identifier deltaId) {
		AttributeInstance fake = new AttributeInstance(real.getAttribute(), _ -> {
		});
		fake.setBaseValue(real.getBaseValue());
		for (AttributeModifier modifier : real.getModifiers()) {
			fake.addTransientModifier(modifier);
		}
		double delta = targetTotal - fake.getValue();
		if (Math.abs(delta) > 1e-6) {
			fake.addTransientModifier(new AttributeModifier(deltaId, delta, AttributeModifier.Operation.ADD_VALUE));
		}
		return fake;
	}

	/** ExpandAbility spoofs isInWater to false while sinking; read real fluid from the world. */
	static boolean inWorldWater(ServerPlayer player) {
		AABB box = player.getBoundingBox().deflate(0.001);
		return BlockPos.betweenClosedStream(box).anyMatch(pos ->
				player.level().getFluidState(pos).is(FluidTags.WATER));
	}

	private record Spoof(double wme, double movementSpeed, double gravity) {
	}
}
