package magicjinn.artifactspolymer.polymer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import artifacts.component.SwimData;
import artifacts.component.ability.SwimInAir;
import artifacts.platform.PlatformServices;
import artifacts.registry.ModDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.BossEvent;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;

/**
 * HeliumFlamingoPolymerPatch
 */
public final class HeliumFlamingoPolymerPatch {

    // Track bars for each player
    private static final Map<UUID, ServerBossEvent> CHARGE_BARS = new HashMap<>();

    private static final Map<UUID, Boolean> WAS_SPRINT = new HashMap<>();

    private static final Component NAME_CHARGE = Component.literal("Helium Flamingo");

    public static void tick(ServerPlayer player) {
        updateBossBar(player);

        if (!PolymerClientChecks.lacksArtifactsClient(player) || !ModDataComponents.SWIM_IN_AIR.on(player).findAny()) {
            clearPlayerState(player);
            return;
        }

        SwimData swimData = PlatformServices.getPlatformHelper().getSwimData(player);
        if (swimData == null) {
            clearPlayerState(player);
            return;
        }

        handleActivation(player, swimData);
    }

    private static void updateBossBar(ServerPlayer player) {
        if (!ModDataComponents.SWIM_IN_AIR.on(player).findAny()) {
            removeChargeBossBar(player, true);
            return;
        }

        SwimData swimData = PlatformServices.getPlatformHelper().getSwimData(player);
        if (swimData == null) {
            removeChargeBossBar(player);
            return;
        }

        boolean shouldDrain = swimData.shouldDepleteSwimFlyingCharge(player) && !player.isCreative();
        float charge = (float) Mth.clamp(swimData.getSwimFlyingCharge(), 0.0, 1.0);
        boolean recharging = !shouldDrain && charge < 0.999f; // Whether the flamingo is recharging
        // If the player is not using or recharging the flamingo, remove the boss bar
        if (!recharging || !shouldDrain) {
            removeChargeBossBar(player);
            return;
        }

        CHARGE_BARS.computeIfAbsent(player.getUUID(), _ -> {
            ServerBossEvent created = new ServerBossEvent(
                    UUID.randomUUID(),
                    NAME_CHARGE,
                    BossEvent.BossBarColor.PINK,
                    BossEvent.BossBarOverlay.PROGRESS);
            created.setVisible(true);
            created.addPlayer(player);
            return created;
        }).setProgress(charge);
    }

    private static void removeChargeBossBar(ServerPlayer player) {
        removeChargeBossBar(player, false);
    }

    private static void removeChargeBossBar(ServerPlayer player, boolean removePlayer) {
        ServerBossEvent bar = CHARGE_BARS.remove(player.getUUID());
        if (bar == null)
            return;

        if (removePlayer) // TODO check if this is correct
            bar.removePlayer(player);
        bar.setVisible(false);
    }

    private static void handleActivation(ServerPlayer player, SwimData swimData) {
        // TODO: Implement
        // Did the player press the sprint key?
        boolean sprint = player.getLastClientInput().sprint();
        // Was the player sprinting last tick?
        boolean wasSprint = WAS_SPRINT.getOrDefault(player.getUUID(), false);
        WAS_SPRINT.put(player.getUUID(), sprint);
        // If not sprinting, or was sprinting last tick, do nothing
        if (!sprint || wasSprint)
            return;

        if (swimData.isSwimFlying()) {
            if (player.isSwimming() && swimData.shouldDepleteSwimFlyingCharge(player))
                swimData.toggleSwimFlying(player);
        } else if (SwimInAir.canSwim(player)) {
            swimData.toggleSwimFlying(player);
        }
    }

    private static void clearPlayerState(ServerPlayer player) {
        WAS_SPRINT.remove(player.getUUID());
    }
}
