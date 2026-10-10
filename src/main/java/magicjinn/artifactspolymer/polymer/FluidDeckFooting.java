package magicjinn.artifactspolymer.polymer;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

/**
 * POLYMER WORKAROUND: Finds fluid-surface deck positions under / beside the
 * player, including preemptive blocks so vanilla clients can step up from slabs
 * onto water/lava (Artifacts clients handle this natively).
 */
final class FluidDeckFooting {
    private static final int SCAN_RADIUS = 2;
    private static final int[][] STAR = { { 0, 0 }, { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };

    /** Square scan used by Aqua Dashers (3x3 footprint around each deck). */
    static Set<BlockPos> decks(Player player, TagKey<Fluid> fluidTag, int footprintRadius,
            Predicate<BlockState> canOverlay) {
        Set<BlockPos> positions = new HashSet<>();
        ServerLevel level = (ServerLevel) player.level();
        double standY = player.getY();
        float step = player.maxUpStep();

        BlockPos origin = player.blockPosition();
        for (int dx = -SCAN_RADIUS; dx <= SCAN_RADIUS; dx++) {
            for (int dz = -SCAN_RADIUS; dz <= SCAN_RADIUS; dz++) {
                addDeckAt(level, origin.offset(dx, 0, dz), standY, step, fluidTag, footprintRadius, canOverlay,
                        positions);
            }
        }

        return positions;
    }

    /**
     * Strider approach: center + cardinals only, for step-up onto lava.
     * Once already on lava, use {@link #singleUnder}.
     */
    static Set<BlockPos> starDecks(Player player, TagKey<Fluid> fluidTag, Predicate<BlockState> canOverlay) {
        Set<BlockPos> positions = new HashSet<>();
        ServerLevel level = (ServerLevel) player.level();
        double standY = player.getY();
        float step = player.maxUpStep();
        BlockPos origin = player.blockPosition();

        for (int[] offset : STAR) {
            addDeckAt(level, origin.offset(offset[0], 0, offset[1]), standY, step, fluidTag, 0, canOverlay,
                    positions);
        }

        return positions;
    }

    /** Strider on lava: single block under feet. */
    static Set<BlockPos> singleUnder(Player player, TagKey<Fluid> fluidTag, Predicate<BlockState> canOverlay) {
        Set<BlockPos> positions = new HashSet<>();
        ServerLevel level = (ServerLevel) player.level();
        BlockPos deck = deckAtFeet(player, fluidTag);
        if (deck != null && canOverlay.test(level.getBlockState(deck)))
            positions.add(deck.immutable());
        return positions;
    }

    static boolean standingOn(Player player, TagKey<Fluid> fluidTag) {
        return deckAtFeet(player, fluidTag) != null;
    }

    private static void addDeckAt(ServerLevel level, BlockPos column, double standY, float step,
            TagKey<Fluid> fluidTag, int footprintRadius, Predicate<BlockState> canOverlay, Set<BlockPos> out) {
        for (int dy = -1; dy <= 1; dy++) {
            BlockPos fluidPos = column.offset(0, dy, 0);
            FluidState fluid = level.getFluidState(fluidPos);
            if (fluid.isEmpty() || !fluid.is(fluidTag))
                continue;

            double surfaceY = fluidPos.getY() + fluid.getHeight(level, fluidPos);
            if (surfaceY < standY - 0.5 || surfaceY > standY + step + 0.1)
                continue;

            BlockPos deck = BlockPos.containing(
                    fluidPos.getX() + 0.5,
                    surfaceY - 0.125,
                    fluidPos.getZ() + 0.5);
            out.addAll(PacketBootFooting.footprint(level, deck, footprintRadius, canOverlay));
        }
    }

    private static BlockPos deckAtFeet(Player player, TagKey<Fluid> fluidTag) {
        BlockPos fluidPos = BlockPos.containing(player.getX(), player.getY() - 0.2, player.getZ());
        FluidState at = player.level().getFluidState(fluidPos);
        if (at.isEmpty()) {
            fluidPos = player.blockPosition();
            at = player.level().getFluidState(fluidPos);
        }
        if (!at.is(fluidTag))
            return null;

        double surfaceY = fluidPos.getY() + at.getHeight(player.level(), fluidPos);
        return BlockPos.containing(player.getX(), surfaceY - 0.125, player.getZ());
    }
}
