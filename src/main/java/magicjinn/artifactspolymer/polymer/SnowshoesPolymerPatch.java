package magicjinn.artifactspolymer.polymer;

import java.util.Set;

import artifacts.registry.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class SnowshoesPolymerPatch {
    // POLYMER WORKAROUND: Powder snow is walkable server-side via Artifacts, but
    // vanilla clients still sink visually. Overlay snow blocks while standing in it.
    private static final PacketBootFooting FOOTING = new PacketBootFooting(Blocks.SNOW_BLOCK.defaultBlockState());

    public static void tick(ServerPlayer player) {
        boolean active = canWalkOnPowderSnow(player);
        BlockPos center = active ? powderSnowCenter(player) : null;
        Set<BlockPos> target = active
                ? PacketBootFooting.footprint((ServerLevel) player.level(), center, 1,
                        SnowshoesPolymerPatch::canOverlay)
                : Set.of();
        FOOTING.tick(player, active, target);
    }

    public static void onDisconnect(ServerPlayer player) {
        FOOTING.onDisconnect(player);
    }

    private static BlockPos powderSnowCenter(Player player) {
        BlockPos pos = BlockPos.containing(player.getX(), player.getY() - 0.2, player.getZ());
        if (canOverlay(player.level().getBlockState(pos)))
            return pos;

        pos = player.blockPosition();
        if (canOverlay(player.level().getBlockState(pos)))
            return pos;

        return null;
    }

    private static boolean canOverlay(BlockState state) {
        return state.is(Blocks.POWDER_SNOW);
    }

    public static boolean canWalkOnPowderSnow(Player player) {
        if (!(player instanceof LivingEntity living))
            return false;

        if (!ModDataComponents.WALK_ON_POWDER_SNOW.on(living).findAny())
            return false;

        return powderSnowCenter(player) != null;
    }
}
