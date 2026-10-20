package magicjinn.artifactspolymer.polymer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import artifacts.component.SwimData;
import artifacts.component.ability.SwimInAir;
import artifacts.platform.PlatformServices;
import artifacts.registry.ModDataComponents;
import magicjinn.artifactspolymer.ArtifactsPolymerPatch;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec3;

/**
 * POLYMER WORKAROUND: Vanilla clients lack Helium Flamingo flight. There is no
 * good way to let the player swim in the air, so we fake it by letting them fly
 * around, with speed and mechanics closely following swimming.
 * Flamingo charge is tracked on the server and synced to the client as a boss bar.
 */
public final class HeliumFlamingoPolymerPatch {

    private static final Map<UUID, PlayerState> STATES = new HashMap<>();

    private static final int NUM_AIR_BUBBLES = 10;

    private static final String CHARGE_ICON = "🦩";
    private static final String EMPTY_ICON = "_";

    private static final Component NAME_CHARGE = Component.literal("Helium Flamingo");

    /**
     * Target ~3.91 blocks/s sprint-swim. Scaled by 3.91/4.40 so client air
     * control on top of the motion packet does not overshoot.
     */
    private static final double SWIM_SPEED_PER_TICK = 3.91 / 20.0 * (3.91 / 4.40);

    /** Idle sink while floating in water (~0.5 blocks/s). */
    private static final double SINK_SPEED_PER_TICK = 0.5 / 20.0;

    /** Active swim up/down with jump/shift (~4.5 blocks/s). */
    private static final double VERTICAL_SWIM_PER_TICK = 4.5 / 20.0;

    private static final Identifier NO_GRAVITY_ID = ArtifactsPolymerPatch.id("helium_flamingo_no_gravity");

    /**
     * POLYMER WORKAROUND: Self-only SCALE (×1/3) so the flyer's own client gets a
     * swim-sized hitbox. Pose.SWIMMING shrinks the server entity, but a vanilla
     * client still predicts a standing self-hitbox in air. Not applied on the
     * server entity. Other players see Pose.SWIMMING instead.
     */
    private static final Identifier SELF_SWIM_SCALE_ID = ArtifactsPolymerPatch.id("helium_flamingo_self_swim_scale");

    private static final double SELF_SWIM_SCALE_MULTIPLIER = -2.0 / 3.0;

    private static final class PlayerState {
        ServerBossEvent chargeBar;
        boolean wasSprint;
        int lastBubblePop;
        boolean selfScaleActive;
        /** True only while Polymer is forcing air-swim pose / motion. */
        boolean airSwimActive;
    }

    private static PlayerState state(ServerPlayer player) {
        return STATES.computeIfAbsent(player.getUUID(), _ -> new PlayerState());
    }

    public static void tick(ServerPlayer player) {
        updateBossBar(player);

        if (!ModDataComponents.SWIM_IN_AIR.on(player).findAny()) {
            resetSprintAndFlight(player);
            return;
        }

        SwimData swimData = PlatformServices.getPlatformHelper().getSwimData(player);
        if (swimData == null) {
            resetSprintAndFlight(player);
            return;
        }

        handleActivation(player, swimData);
        maintainAirSwim(player, swimData);
        tickBubblePopSound(player, swimData);
    }

    public static void onDisconnect(ServerPlayer player) {
        clearPlayer(player);
        STATES.remove(player.getUUID());
    }

    /** Same UUID as the dead player. Drop stale boss bar / flight state. */
    public static void onRespawn(ServerPlayer oldPlayer) {
        onDisconnect(oldPlayer);
    }

    public static void clearAll() {
        for (PlayerState state : STATES.values()) {
            if (state.chargeBar != null) {
                state.chargeBar.setVisible(false);
                state.chargeBar = null;
            }
        }
        STATES.clear();
    }

    private static void clearPlayer(ServerPlayer player) {
        resetSprintAndFlight(player);
        removeChargeBossBar(player);
    }

    private static void resetSprintAndFlight(ServerPlayer player) {
        PlayerState state = STATES.get(player.getUUID());
        if (state != null)
            state.wasSprint = false;
        clearFlight(player);
    }

    private static void updateBossBar(ServerPlayer player) {
        // Include cooldown: mid-air cancel puts the flamingo on cooldown for a few
        // seconds, but charge still recharges and the bar should stay visible.
        if (!ModDataComponents.SWIM_IN_AIR.on(player).includeItemsOnCooldown().findAny()) {
            removeChargeBossBar(player);
            return;
        }

        SwimData swimData = PlatformServices.getPlatformHelper().getSwimData(player);
        if (swimData == null) {
            removeChargeBossBar(player);
            return;
        }

        boolean shouldDrain = swimData.shouldDepleteSwimFlyingCharge(player) && !player.isCreative();
        float charge = (float) Mth.clamp(swimData.getSwimFlyingCharge(), 0.0, 1.0);
        boolean recharging = !shouldDrain && charge < 0.999f;
        if (!recharging && !shouldDrain) {
            removeChargeBossBar(player);
            return;
        }

        Component name = chargeBarName(charge, remainingCooldownSeconds(player));

        PlayerState state = state(player);
        if (state.chargeBar == null) {
            ServerBossEvent created = new ServerBossEvent(
                    UUID.randomUUID(),
                    name,
                    BossEvent.BossBarColor.PINK,
                    BossEvent.BossBarOverlay.PROGRESS);
            created.setVisible(true);
            created.addPlayer(player);
            state.chargeBar = created;
        } else {
            state.chargeBar.setVisible(true);
        }
        state.chargeBar.setName(name);
        // Round to nearest 10% to avoid jitter on clients.
        state.chargeBar.setProgress((float) Mth.roundToward((int) (charge * 100), 10) / 100.0f);
    }

    private static Component chargeBarName(float charge, float cooldownSec) {
        if (cooldownSec > 0.05f)
            return Component.literal(String.format("Helium Flamingo (cooldown %.1fs)", cooldownSec));

        StringBuilder icons = new StringBuilder(1 + NUM_AIR_BUBBLES).append(' ');
        for (int i = 0; i < NUM_AIR_BUBBLES; i++)
            icons.append(i < charge * NUM_AIR_BUBBLES ? CHARGE_ICON : EMPTY_ICON);
        return NAME_CHARGE.copy().append(Component.literal(icons.toString()));
    }

    /** Remaining Helium Flamingo item cooldown in seconds (mid-air cancel). */
    private static float remainingCooldownSeconds(ServerPlayer player) {
        float[] max = { 0f };
        ModDataComponents.SWIM_IN_AIR.on(player).includeItemsOnCooldown().iterate((ability, slot) -> {
            if (!slot.isOnCooldown(player))
                return;
            float percent = player.getCooldowns().getCooldownPercent(slot.get(), 0f);
            int totalTicks = Math.max(5, ability.cooldown().get() * 20);
            max[0] = Math.max(max[0], percent * totalTicks / 20f);
        });
        return max[0];
    }

    private static void removeChargeBossBar(ServerPlayer player) {
        PlayerState state = STATES.get(player.getUUID());
        if (state == null || state.chargeBar == null)
            return;

        state.chargeBar.removePlayer(player);
        state.chargeBar.setVisible(false);
        state.chargeBar = null;
    }

    private static void handleActivation(ServerPlayer player, SwimData swimData) {
        PlayerState state = state(player);
        boolean sprint = player.getLastClientInput().sprint();
        boolean wasSprint = state.wasSprint;
        state.wasSprint = sprint;
        if (!sprint || wasSprint)
            return;

        if (swimData.isSwimFlying()) {
            if (swimData.shouldDepleteSwimFlyingCharge(player))
                swimData.toggleSwimFlying(player);
        } else if (SwimInAir.canSwim(player)) {
            swimData.toggleSwimFlying(player);
        }
    }

    private static void maintainAirSwim(ServerPlayer player, SwimData swimData) {
        if (!swimData.isSwimFlying() || player.onGround()) {
            // Only tear down when we were air-swimming. Clearing every idle tick
            // forced Pose.STANDING and broke real water swimming (e.g. under ice).
            if (state(player).airSwimActive)
                clearFlight(player);
            return;
        }

        applyNoGravity(player);
        // Server + other clients see Pose.SWIMMING. Own client gets SCALE so its
        // standing self-view is swim-sized.
        player.setSwimming(true);
        player.setPose(Pose.SWIMMING);
        sendSelfScaleOverride(player, true);
        state(player).airSwimActive = true;

        Input input = player.getLastClientInput();
        Vec3 motion;
        if (input.forward()) {
            motion = player.getLookAngle().scale(SWIM_SPEED_PER_TICK);
        } else if (input.shift()) {
            motion = new Vec3(0.0, -VERTICAL_SWIM_PER_TICK, 0.0);
        } else if (input.jump()) {
            motion = new Vec3(0.0, VERTICAL_SWIM_PER_TICK, 0.0);
        } else {
            motion = new Vec3(0.0, -SINK_SPEED_PER_TICK, 0.0);
        }

        // Client-authoritative position. Push swim-like velocity only. Do not
        // rewind/setPos or server-move. The client reports position as usual.
        player.setDeltaMovement(motion);
        player.connection.send(new ClientboundSetEntityMotionPacket(player));
    }

    private static void applyNoGravity(ServerPlayer player) {
        AttributeInstance gravity = player.getAttribute(Attributes.GRAVITY);
        if (gravity == null)
            return;

        gravity.addOrUpdateTransientModifier(new AttributeModifier(
                NO_GRAVITY_ID,
                -1.0,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    /**
     * Rewrite SCALE for {@code player} only. Server entity scale is unchanged, so
     * trackers keep sending full-size to everyone else.
     */
    private static void sendSelfScaleOverride(ServerPlayer player, boolean shrink) {
        AttributeInstance real = player.getAttribute(Attributes.SCALE);
        if (real == null)
            return;

        AttributeInstance view = new AttributeInstance(Attributes.SCALE, _ -> {
        });
        view.setBaseValue(real.getBaseValue());
        for (AttributeModifier mod : real.getModifiers())
            view.addOrUpdateTransientModifier(mod);
        if (shrink) {
            view.addOrUpdateTransientModifier(new AttributeModifier(
                    SELF_SWIM_SCALE_ID,
                    SELF_SWIM_SCALE_MULTIPLIER,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            state(player).selfScaleActive = true;
        } else {
            PlayerState state = STATES.get(player.getUUID());
            if (state != null)
                state.selfScaleActive = false;
        }

        player.connection.send(new ClientboundUpdateAttributesPacket(player.getId(), List.of(view)));
    }

    /**
     * Mirror HeliumFlamingoOverlay. As flight charge crosses each of 10 bubble
     * thresholds, play {@code ui.hud.bubble_pop} with rising volume/pitch.
     */
    private static void tickBubblePopSound(ServerPlayer player, SwimData swimData) {
        PlayerState state = STATES.get(player.getUUID());
        if (!swimData.isSwimFlying()
                || !swimData.shouldDepleteSwimFlyingCharge(player)
                || player.isCreative()) {
            if (state != null)
                state.lastBubblePop = 0;
            return;
        }

        int maxFlight = SwimInAir.getMaxFlightDuration(player);
        if (maxFlight <= 0)
            return;

        int progress = (int) Math.floor(swimData.getSwimFlyingCharge() * maxFlight);
        int filledBubble = airSupplyBubble(progress, maxFlight, -2);
        int poppingBubble = airSupplyBubble(progress, maxFlight, 0);
        int emptyBubble = airSupplyBubble(progress, maxFlight, 2);
        // Overlay only plays when the popping sprite shows (not still filled).
        if (poppingBubble <= filledBubble || poppingBubble < 1 || poppingBubble > NUM_AIR_BUBBLES)
            return;

        state = state(player);
        if (state.lastBubblePop == poppingBubble)
            return;

        int numPopped = NUM_AIR_BUBBLES - emptyBubble;
        float volume = 0.5f + 0.1f * Math.max(0, numPopped - 3 + 1);
        float pitch = 1.0f + 0.1f * Math.max(0, numPopped - 5 + 1);
        player.level().playSound(
                null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.BUBBLE_POP,
                player.getSoundSource(),
                volume,
                pitch);
        state.lastBubblePop = poppingBubble;
    }

    /** Same formula as HeliumFlamingoOverlay.getCurrentAirSupplyBubble. */
    private static int airSupplyBubble(int progress, float maxDuration, int offset) {
        return Mth.ceil((progress + offset) * NUM_AIR_BUBBLES / maxDuration);
    }

    private static void clearFlight(ServerPlayer player) {
        AttributeInstance gravity = player.getAttribute(Attributes.GRAVITY);
        if (gravity != null)
            gravity.removeModifier(NO_GRAVITY_ID);

        PlayerState state = STATES.get(player.getUUID());
        boolean wasAirSwim = state != null && state.airSwimActive;
        if (state != null && state.selfScaleActive)
            sendSelfScaleOverride(player, false);

        // Don't force standing in water. Vanilla swim pose must stay under ice etc.
        if (wasAirSwim && !player.isInWater()) {
            player.setSwimming(false);
            if (player.getPose() == Pose.SWIMMING)
                player.setPose(Pose.STANDING);
        }

        if (state != null) {
            state.airSwimActive = false;
            state.lastBubblePop = 0;
        }
    }
}
