package magicjinn.artifactspolymer.polymer;

import java.util.Set;

import artifacts.component.SwimData;
import artifacts.platform.PlatformServices;
import artifacts.registry.ModDataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;

public class AquaDashersPolymerPatch {
    // TODO: In 26.2, experiment with:
    // When the player is above water and sprinting, set the player's gravity to 0,
    // and decrease their air drag to match on ground friction. Air drag and
    // friction are not editable in 26.1.2

    // POLYMER WORKAROUND: Waterlogged barriers give vanilla clients solid footing
    // while sprinting on water (Artifacts fluid collision is server-only
    // otherwise).
    private static final BlockState FAKE_FOOTING = Blocks.BARRIER.defaultBlockState()
            .setValue(BlockStateProperties.WATERLOGGED, true);

    private static final PacketBootFooting FOOTING = new PacketBootFooting(FAKE_FOOTING);

    public static void tick(ServerPlayer player) {
        boolean ready = canWaterWalk(player);
        FOOTING.tick(player, ready,
                ready ? FluidDeckFooting.decks(player, FluidTags.WATER, 1, AquaDashersPolymerPatch::canOverlay)
                        : Set.of());
    }

    public static void onDisconnect(ServerPlayer player) {
        FOOTING.onDisconnect(player);
    }

    private static boolean canOverlay(BlockState state) {
        if (state.is(Blocks.WATER))
            return true;

        if (state.isAir())
            return true;

        return state.getFluidState().is(FluidTags.WATER);
    }

    /** Sprint + aqua ability, not necessarily already standing in water. */
    public static boolean canWaterWalk(Player player) {
        if (!(player instanceof LivingEntity living))
            return false;

        if (!player.isSprinting())
            return false;

        SwimData swimData = PlatformServices.getPlatformHelper().getSwimData(living);
        if (swimData == null || swimData.shouldBreakSurfaceTension())
            return false;

        var water = Fluids.WATER.defaultFluidState();
        return ModDataComponents.FLUID_COLLISION.on(living)
                .filter(flCollision -> flCollision.matchesFluid(water) && flCollision.condition().test(living))
                .findAny();
    }
}
