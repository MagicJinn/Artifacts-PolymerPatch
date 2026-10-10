package magicjinn.artifactspolymer.polymer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

/**
 * POLYMER WORKAROUND: Shared packet-only fake footing for boot patches. Vanilla
 * clients lack Artifacts' client fluid/ice/powder-snow handling, so we overlay
 * solid BlockUpdate packets under the player without changing the real world.
 */
public final class PacketBootFooting {
    @FunctionalInterface
    public interface FakeState {
        BlockState get(ServerPlayer player, BlockPos pos);
    }

    private final Map<UUID, Set<BlockPos>> active = new HashMap<>();
    private final FakeState fakeState;

    public PacketBootFooting(BlockState fakeState) {
        this((player, pos) -> fakeState);
    }

    public PacketBootFooting(FakeState fakeState) {
        this.fakeState = fakeState;
    }

    public void tick(ServerPlayer player, boolean conditionsMet, Set<BlockPos> target) {
        if (!PolymerClientChecks.lacksArtifactsClient(player) || !conditionsMet) {
            clear(player);
            return;
        }

        if (target.isEmpty()) {
            clear(player);
            return;
        }

        Set<BlockPos> previous = active.getOrDefault(player.getUUID(), Set.of());
        for (BlockPos pos : previous) {
            if (!target.contains(pos))
                sendRealBlock(player, pos);
        }
        // POLYMER WORKAROUND: Resend every tick so clicks / client block updates
        // cannot restore the real fluid/ice underneath.
        for (BlockPos pos : target) {
            sendFakeFooting(player, pos);
        }

        active.put(player.getUUID(), new HashSet<>(target));
    }

    public void onDisconnect(ServerPlayer player) {
        active.remove(player.getUUID());
    }

    public void clear(ServerPlayer player) {
        Set<BlockPos> previous = active.remove(player.getUUID());
        if (previous == null)
            return;

        for (BlockPos pos : previous) {
            sendRealBlock(player, pos);
        }
    }

    public static Set<BlockPos> footprint(ServerLevel level, BlockPos center, int horizontalRadius,
            Predicate<BlockState> canOverlay) {
        Set<BlockPos> positions = new HashSet<>();
        if (center == null)
            return positions;

        for (int dx = -horizontalRadius; dx <= horizontalRadius; dx++) {
            for (int dz = -horizontalRadius; dz <= horizontalRadius; dz++) {
                BlockPos pos = center.offset(dx, 0, dz);
                if (canOverlay.test(level.getBlockState(pos)))
                    positions.add(pos.immutable());
            }
        }
        return positions;
    }

    private void sendFakeFooting(ServerPlayer player, BlockPos pos) {
        player.connection.send(new ClientboundBlockUpdatePacket(pos, fakeState.get(player, pos)));
    }

    private static void sendRealBlock(ServerPlayer player, BlockPos pos) {
        BlockState state = ((ServerLevel) player.level()).getBlockState(pos);
        player.connection.send(new ClientboundBlockUpdatePacket(pos, state));
    }
}
