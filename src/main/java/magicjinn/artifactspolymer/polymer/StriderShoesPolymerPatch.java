package magicjinn.artifactspolymer.polymer;

import java.util.Set;

import artifacts.component.SwimData;
import artifacts.platform.PlatformServices;
import artifacts.registry.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class StriderShoesPolymerPatch {
    // POLYMER WORKAROUND: Magma packets give vanilla clients solid footing while
    // sneaking on lava (Artifacts fluid collision is server-only otherwise).
    private static final PacketBootFooting FOOTING = new PacketBootFooting(Blocks.MAGMA_BLOCK.defaultBlockState());

    public static void tick(ServerPlayer player) {
        boolean ready = canLavaWalk(player);
        if (!ready) {
            FOOTING.tick(player, false, Set.of());
            return;
        }

        // POLYMER WORKAROUND: On lava 1x1 is enough. Approaching uses a star so
        // clients can step up without flooding a large area with fake magma.
        Set<BlockPos> target = FluidDeckFooting.standingOn(player, FluidTags.LAVA)
                ? FluidDeckFooting.singleUnder(player, FluidTags.LAVA, StriderShoesPolymerPatch::canOverlay)
                : FluidDeckFooting.starDecks(player, FluidTags.LAVA, StriderShoesPolymerPatch::canOverlay);
        FOOTING.tick(player, true, target);
    }

    public static void onDisconnect(ServerPlayer player) {
        FOOTING.onDisconnect(player);
    }

    private static boolean canOverlay(BlockState state) {
        if (state.is(Blocks.LAVA) || state.isAir())
            return true;

        return state.getFluidState().is(FluidTags.LAVA);
    }

    public static boolean canLavaWalk(Player player) {
        if (!(player instanceof LivingEntity living))
            return false;

        SwimData swimData = PlatformServices.getPlatformHelper().getSwimData(living);
        if (swimData == null || swimData.shouldBreakSurfaceTension())
            return false;

        return ModDataComponents.FLUID_COLLISION.on(living)
                .filter(flCollision -> flCollision.tag().isPresent()
                        && flCollision.tag().get().equals(FluidTags.LAVA)
                        && flCollision.condition().test(living))
                .findAny();
    }
}
