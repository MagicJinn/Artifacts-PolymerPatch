package magicjinn.artifactspolymer.polymer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import artifacts.component.ability.DoubleJump;
import artifacts.registry.ModDataComponents;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;

/**
 * POLYMER WORKAROUND: Server-side double jump for vanilla clients. Artifacts
 * only runs the client-side jump input when its client mod is present.
 */
public final class CloudInABottlePolymerPatch {
    private static final Map<UUID, State> STATES = new HashMap<>();

    private static final class State {
        boolean canDoubleJump;
        boolean hasReleasedJumpKey;
    }

    private static void clear(ServerPlayer player) {
        STATES.remove(player.getUUID());
    }

    public static void tick(ServerPlayer player) {
        // Only for vanilla/polymer clients. Artifacts handles double jump on its own
        // client.
        if (!PolymerClientChecks.lacksArtifactsClient(player) ||
                !ModDataComponents.DOUBLE_JUMP.on(player).findAny()) {
            clear(player);
            return;
        }

        State state = STATES.computeIfAbsent(player.getUUID(), _ -> new State());
        boolean jump = player.getLastClientInput().jump(); // Whether the player last pressed the jump key

        if (isGroundedForDoubleJump(player)) {
            state.hasReleasedJumpKey = false;
            state.canDoubleJump = true;
        } else if (!jump) {
            state.hasReleasedJumpKey = true;
        } else if (!player.getAbilities().flying && state.canDoubleJump && state.hasReleasedJumpKey) {
            state.canDoubleJump = false;
            DoubleJump.jump(player);
            player.connection.send(new ClientboundSetEntityMotionPacket(player));
            spawnParticles(player);
        }
    }

    public static void onDisconnect(ServerPlayer player) {
        clear(player);
    }

    private static boolean isGroundedForDoubleJump(ServerPlayer player) {
        if (!(player.onGround() || player.onClimbable()))
            return false;

        if (!player.isInWater())
            return true;

        // Match Artifacts CloudInABottleInputHandler: onGround+water only refreshes
        // double jump when Charm of Sinking is active (seafloor). Floating on water
        // without sinking does not count as grounded.
        return ModDataComponents.SINKING.on(player).findAny();
    }

    // Particle spawn code is identical to Artifacts' method
    private static void spawnParticles(ServerPlayer player) {
        ParticleOptions particle = player.isInWater() ? ParticleTypes.BUBBLE : ParticleTypes.POOF;
        for (int i = 0; i < 20; i++) {
            double motionX = player.getRandom().nextGaussian() * 0.02;
            double motionY = player.getRandom().nextGaussian() * 0.02 + 0.20;
            double motionZ = player.getRandom().nextGaussian() * 0.02;
            player.level().sendParticles(particle,
                    player.getX(), player.getY(), player.getZ(),
                    1,
                    motionX, motionY, motionZ,
                    0.15);
        }
    }
}
