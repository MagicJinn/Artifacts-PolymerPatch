package magicjinn.artifactspolymer.polymer;

import java.util.Set;

import artifacts.registry.ModAttributes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

public class SteadfastSpikesPolymerPatch {
    // POLYMER WORKAROUND: Artifacts slip resistance is server-side. Vanilla clients
    // still apply ice friction. Overlay non-slip ice-lookalike Polymer blocks.
    // TODO: This can be fixed in 26.2 with the new attributes for friction etc.
    private static final PacketBootFooting FOOTING = new PacketBootFooting(
            (player, pos) -> NonSlipIceBlocks.clientFootingFor(player, player.level().getBlockState(pos)));

    public static void tick(ServerPlayer player) {
        boolean active = hasSlipResistance(player);
        BlockPos center = active ? iceCenter(player) : null;
        Set<BlockPos> target = active
                ? PacketBootFooting.footprint((ServerLevel) player.level(), center, 0,
                        SteadfastSpikesPolymerPatch::canOverlay)
                : Set.of();
        FOOTING.tick(player, active, target);
    }

    public static void onDisconnect(ServerPlayer player) {
        FOOTING.onDisconnect(player);
    }

    private static BlockPos iceCenter(Player player) {
        BlockPos pos = player.getOnPos();
        if (canOverlay(player.level().getBlockState(pos)))
            return pos;

        pos = BlockPos.containing(player.getX(), player.getY() - 0.2, player.getZ());
        if (canOverlay(player.level().getBlockState(pos)))
            return pos;

        return null;
    }

    private static boolean canOverlay(BlockState state) {
        return state.is(BlockTags.ICE);
    }

    public static boolean hasSlipResistance(Player player) {
        if (!(player instanceof LivingEntity living))
            return false;

        return living.getAttributeValue(ModAttributes.SLIP_RESISTANCE) > 0
                && iceCenter(player) != null;
    }
}
