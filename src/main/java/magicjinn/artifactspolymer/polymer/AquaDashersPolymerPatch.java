package magicjinn.artifactspolymer.polymer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import artifacts.component.SwimData;
import artifacts.platform.PlatformServices;
import artifacts.registry.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.world.level.material.FluidState;

public class AquaDashersPolymerPatch {
    // TODO: In 26.2, experiment with:
    // When the player is above water and sprinting, set the player's gravity to 0,
    // and decrease their air drag to match on ground friction. Air drag and
    // friction are not editable in 26.1.2

    private static final int HORIZONTAL_RADIUS = 1;
    private static final BlockState FAKE_FOOTING = Blocks.BARRIER.defaultBlockState()
            .setValue(BlockStateProperties.WATERLOGGED, true);

    private static final Map<UUID, Set<BlockPos>> ACTIVE = new HashMap<>();

    public static void tick(ServerPlayer player) {
        if (!PolymerClientChecks.lacksArtifactsClient(player)) {
            // Player has the artifacts client, so we don't need to do anything
            clear(player);
            return;
        }

        // If the player is currently above water and sprinting, place a 3x3 area of
        // waterlogged barrier blocks under them (packet only)
        if (!isWaterSprinting(player)) {
            clear(player);
            return;
        }

        Set<BlockPos> target = footprint(player);
        if (target.isEmpty()) {
            clear(player);
            return;
        }

        Set<BlockPos> previous = ACTIVE.getOrDefault(player.getUUID(), Set.of());
        for (BlockPos pos : previous) {
            if (!target.contains(pos))
                sendRealBlock(player, pos);
        }
        for (BlockPos pos : target) {
            if (!previous.contains(pos))
                sendFakeFooting(player, pos);
        }
        ACTIVE.put(player.getUUID(), new HashSet<>(target));
    }

    public static void onDisconnect(ServerPlayer player) {
        clear(player);
    }

    private static void clear(ServerPlayer player) {
        Set<BlockPos> previous = ACTIVE.remove(player.getUUID());
        if (previous == null)
            return;

        for (BlockPos pos : previous) {
            sendRealBlock(player, pos);
        }
    }

    private static Set<BlockPos> footprint(ServerPlayer player) {
        BlockPos center = waterDeckCenter(player);
        if (center == null)
            return Set.of();

        Set<BlockPos> positions = new HashSet<>();
        ServerLevel level = (ServerLevel) player.level();
        for (int dx = -HORIZONTAL_RADIUS; dx <= HORIZONTAL_RADIUS; dx++) {
            for (int dz = -HORIZONTAL_RADIUS; dz <= HORIZONTAL_RADIUS; dz++) {
                BlockPos pos = center.offset(dx, 0, dz);
                if (canOverlay(level.getBlockState(pos)))
                    positions.add(pos.immutable());
            }
        }
        return positions;
    }

    private static BlockPos waterDeckCenter(ServerPlayer player) {
        FluidState fluid = fluidAtFeet(player);
        if (fluid == null || !fluid.is(FluidTags.WATER))
            return null;

        BlockPos fluidPos = BlockPos.containing(player.getX(), player.getY() - 0.2, player.getZ());
        FluidState at = player.level().getFluidState(fluidPos);
        if (at.isEmpty()) {
            fluidPos = player.blockPosition();
            at = player.level().getFluidState(fluidPos);
        }
        if (!at.is(FluidTags.WATER))
            return null;

        double surfaceY = fluidPos.getY() + at.getHeight(player.level(), fluidPos);
        return BlockPos.containing(player.getX(), surfaceY - 0.125, player.getZ());
    }

    private static boolean canOverlay(BlockState state) {
        if (state.is(Blocks.WATER))
            return true;

        if (state.isAir())
            return true;

        return state.getFluidState().is(FluidTags.WATER);
    }

    private static void sendFakeFooting(ServerPlayer player, BlockPos pos) {
        player.connection.send(new ClientboundBlockUpdatePacket(pos, FAKE_FOOTING));
    }

    private static void sendRealBlock(ServerPlayer player, BlockPos pos) {
        BlockState state = ((ServerLevel) player.level()).getBlockState(pos);
        player.connection.send(new ClientboundBlockUpdatePacket(pos, state));
    }

    public static boolean isWaterSprinting(Player player) {
        if (!(player instanceof LivingEntity living))
            return false;

        if (!player.isSprinting())
            return false;

        SwimData swimData = PlatformServices.getPlatformHelper().getSwimData(living);
        if (swimData == null || swimData.shouldBreakSurfaceTension())
            return false;

        if (!ModDataComponents.FLUID_COLLISION.on(living).findAny())
            return false;

        FluidState fluid = fluidAtFeet(player);
        return fluid != null && fluid.is(FluidTags.WATER);
    }

    private static FluidState fluidAtFeet(Player player) {
        BlockPos pos = BlockPos.containing(player.getX(), player.getY() - 0.2, player.getZ());
        FluidState fluid = player.level().getFluidState(pos);
        if (!fluid.isEmpty())
            return fluid;

        pos = player.blockPosition();
        return player.level().getFluidState(pos);
    }
}
