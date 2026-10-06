package magicjinn.artifactspatch.poly;

import artifacts.component.SwimData;
import artifacts.platform.PlatformServices;
import artifacts.registry.ModAttributes;
import artifacts.registry.ModDataComponents;
import magicjinn.artifactspatch.ArtifactsPolymerPatch;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * POLYMER WORKAROUND: Flippers use ADD_MULTIPLIED_BASE on artifacts:swim_speed (default +70%).
 * Artifacts only multiplies travelInWater's moveRelative accel; drag is unchanged. Vanilla clients
 * never run that mixin. Spoof a tiny water_movement_efficiency (so movement_speed affects swim
 * accel) plus enough movement_speed that client moveRelative matches Artifacts' boosted accel.
 *
 * Mid-water sprint-swim is the fragile case: server sprint attribute syncs wipe our movement_speed
 * spoof (ocean-floor wading does not toggle sprint). Resend the spoof every tick while active.
 */
public final class SwimSpeedPolymerSupport {
	private static final double BASE_SWIM_ACCEL = 0.02;
	/** Vanilla travelInWater lerps water drag toward this as WME rises. */
	private static final double WME_DRAG_TARGET = 0.54600006;
	/**
	 * Minimal effective WME so movement_speed participates in swim accel. Keep this small so drag
	 * stays close to vanilla (Artifacts does not reduce drag).
	 */
	private static final double E_BOOST = 0.03;
	/**
	 * Attribute spoof still slightly outruns Artifacts' accel-only multiply on a speedometer
	 * (measured 7.54 vs target 6.7 from baseline 3.92 at default +70% flippers). Scale the (S-1)
	 * bonus so the measured ratio matches swim_speed.
	 */
	private static final double SPOOF_BONUS_SCALE = (6.7 / 3.92 - 1.0) / (7.54 / 3.92 - 1.0);
	private static final Identifier MS_DELTA_ID = ArtifactsPolymerPatch.id("polymer_swim_ms");
	private static final Identifier WME_DELTA_ID = ArtifactsPolymerPatch.id("polymer_swim_wme");
	/** Keep spoof briefly if isInWater flickers at the surface. */
	private static final int WATER_GRACE_TICKS = 5;

	private static final Set<UUID> ACTIVE = ConcurrentHashMap.newKeySet();
	private static final Map<UUID, Integer> WATER_GRACE = new ConcurrentHashMap<>();

	private SwimSpeedPolymerSupport() {
	}

	public static void tick(ServerPlayer player) {
		if (!PolymerClientChecks.lacksArtifactsClient(player)) {
			clear(player);
			return;
		}
		if (!player.getAttributes().hasAttribute(ModAttributes.SWIM_SPEED)) {
			clear(player);
			return;
		}

		double swimSpeed = player.getAttributeValue(ModAttributes.SWIM_SPEED);
		if (swimSpeed <= 1.0001) {
			clear(player);
			return;
		}

		boolean inWater = player.isInWater() || player.isSwimming();
		// Charm of Sinking: ExpandAbility FAIL makes isInWater false; sinking support owns water spoofs.
		if (ModDataComponents.SINKING.on(player).findAny() && !inWater) {
			SwimData swimData = PlatformServices.getPlatformHelper().getSwimData(player);
			if (swimData == null || !swimData.isSwimFlying()) {
				clear(player);
				return;
			}
		}
		int grace = WATER_GRACE.getOrDefault(player.getUUID(), 0);
		if (inWater) {
			grace = WATER_GRACE_TICKS;
		} else if (grace > 0) {
			grace--;
		}
		if (grace <= 0) {
			WATER_GRACE.remove(player.getUUID());
			clear(player);
			return;
		}
		WATER_GRACE.put(player.getUUID(), grace);

		// Resend every tick: sprint / equipment attribute syncs otherwise wipe the spoof.
		sendSpoof(player, computeSpoof(player, swimSpeed));
		ACTIVE.add(player.getUUID());
	}

	/** Re-apply after a full attribute sync so real snapshots do not wipe the spoof. */
	public static void afterAttributeSync(ServerPlayer player) {
		tick(player);
	}

	public static void onDisconnect(ServerPlayer player) {
		ACTIVE.remove(player.getUUID());
		WATER_GRACE.remove(player.getUUID());
	}

	private static void clear(ServerPlayer player) {
		WATER_GRACE.remove(player.getUUID());
		if (!ACTIVE.remove(player.getUUID())) {
			return;
		}
		sendReal(player);
	}

	private static Spoof computeSpoof(ServerPlayer player, double swimSpeed) {
		AttributeInstance msInst = player.getAttribute(Attributes.MOVEMENT_SPEED);
		AttributeInstance wmeInst = player.getAttribute(Attributes.WATER_MOVEMENT_EFFICIENCY);
		// getSpeed() includes sprint; travelInWater uses that same value.
		double realMs = msInst != null ? msInst.getValue() : 0.1;
		double realWme = wmeInst != null ? wmeInst.getValue() : 0.0;

		// Prefer the swimming (!onGround) half-WME path; that is where flippers matter and where
		// ocean-floor-calibrated spoofs previously broke.
		boolean swimmingPath = player.isSwimming() || !player.onGround();
		double groundFactor = swimmingPath ? 0.5 : 1.0;
		double eReal = realWme * groundFactor;
		double d0 = player.isSprinting() ? 0.9 : 0.8;

		double effectiveSwimSpeed = 1.0 + (swimSpeed - 1.0) * SPOOF_BONUS_SCALE;
		double aArtifacts = (BASE_SWIM_ACCEL + (realMs - BASE_SWIM_ACCEL) * eReal) * effectiveSwimSpeed;
		double dArtifacts = d0 + (WME_DRAG_TARGET - d0) * eReal;

		double eClient = eReal > 1e-4 ? eReal : E_BOOST;
		double wmeClient = Mth.clamp(eClient / groundFactor, 0.0, 1.0);
		eClient = wmeClient * groundFactor;

		double vArtifacts = aArtifacts * dArtifacts / Math.max(1e-6, 1.0 - dArtifacts);
		double dClient = d0 + (WME_DRAG_TARGET - d0) * eClient;
		double aClient = vArtifacts * (1.0 - dClient) / Math.max(1e-6, dClient);

		double msNeeded = eClient <= 1e-6
				? realMs
				: BASE_SWIM_ACCEL + (aClient - BASE_SWIM_ACCEL) / eClient;
		msNeeded = Mth.clamp(msNeeded, 0.0, 1024.0);

		return new Spoof(wmeClient, msNeeded);
	}

	private static void sendSpoof(ServerPlayer player, Spoof spoof) {
		if (player.connection == null) {
			return;
		}
		AttributeInstance realMs = player.getAttribute(Attributes.MOVEMENT_SPEED);
		AttributeInstance realWme = player.getAttribute(Attributes.WATER_MOVEMENT_EFFICIENCY);
		if (realMs == null || realWme == null) {
			return;
		}

		List<AttributeInstance> packet = new ArrayList<>(2);
		packet.add(copyWithTotal(realWme, spoof.wme(), WME_DELTA_ID));
		packet.add(copyWithTotal(realMs, spoof.movementSpeed(), MS_DELTA_ID));
		player.connection.send(new ClientboundUpdateAttributesPacket(player.getId(), packet));
	}

	private static void sendReal(ServerPlayer player) {
		if (player.connection == null) {
			return;
		}
		AttributeInstance realMs = player.getAttribute(Attributes.MOVEMENT_SPEED);
		AttributeInstance realWme = player.getAttribute(Attributes.WATER_MOVEMENT_EFFICIENCY);
		if (realMs == null || realWme == null) {
			return;
		}
		player.connection.send(new ClientboundUpdateAttributesPacket(player.getId(), List.of(realWme, realMs)));
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

	private record Spoof(double wme, double movementSpeed) {
	}
}
