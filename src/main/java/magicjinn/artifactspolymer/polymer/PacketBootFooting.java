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
    /**
     * Re-push the full fake set this often so a client dig/update cannot leave
     * the real fluid/ice visible for long. New/removed positions still sync
     * every tick.
     */
    private static final int FULL_REFRESH_INTERVAL = 10;

    @FunctionalInterface
    public interface FakeState {
        BlockState get(ServerPlayer player, BlockPos pos);
    }

    private final Map<UUID, Set<BlockPos>> active = new HashMap<>();
    private final Map<UUID, Integer> ticksSinceFullRefresh = new HashMap<>();
    private final FakeState fakeState;

    public PacketBootFooting(BlockState fakeState) {
        this((player, pos) -> fakeState);
    }

    public PacketBootFooting(FakeState fakeState) {
        this.fakeState = fakeState;
    }

    public void tick(ServerPlayer player, boolean conditionsMet, Set<BlockPos> target) {
        if (!conditionsMet || target.isEmpty()) {
            clear(player);
            return;
        }

        UUID id = player.getUUID();
        Set<BlockPos> previous = active.getOrDefault(id, Set.of());
        for (BlockPos pos : previous) {
            if (!target.contains(pos))
                sendRealBlock(player, pos);
        }

        int age = ticksSinceFullRefresh.getOrDefault(id, FULL_REFRESH_INTERVAL);
        boolean fullRefresh = age >= FULL_REFRESH_INTERVAL;
        for (BlockPos pos : target) {
            if (fullRefresh || !previous.contains(pos))
                sendFakeFooting(player, pos);
        }

        ticksSinceFullRefresh.put(id, fullRefresh ? 0 : age + 1);
        active.put(id, new HashSet<>(target));
    }

    public void onDisconnect(ServerPlayer player) {
        // Restore real blocks while the connection may still accept packets, then drop
        // state.
        clear(player);
    }

    /** Tear down fake footing (unequip, disconnect, respawn). */
    public void clear(ServerPlayer player) {
        UUID id = player.getUUID();
        ticksSinceFullRefresh.remove(id);
        Set<BlockPos> previous = active.remove(id);
        if (previous == null)
            return;

        for (BlockPos pos : previous) {
            sendRealBlock(player, pos);
        }
    }

    /**
     * Drop all players (server stop).
     */
    public void clearAll() {
        active.clear();
        ticksSinceFullRefresh.clear();
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
                    positions.add(pos);
            }
        }
        return positions;
    }

    private void sendFakeFooting(ServerPlayer player, BlockPos pos) {
        if (player.connection == null)
            return;

        player.connection.send(new ClientboundBlockUpdatePacket(pos, fakeState.get(player, pos)));
    }

    private static void sendRealBlock(ServerPlayer player, BlockPos pos) {
        if (player.connection == null)
            return;

        BlockState state = ((ServerLevel) player.level()).getBlockState(pos);
        player.connection.send(new ClientboundBlockUpdatePacket(pos, state));
    }
}
