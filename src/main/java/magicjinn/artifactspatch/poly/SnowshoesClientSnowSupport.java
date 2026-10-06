package magicjinn.artifactspatch.poly;

import artifacts.registry.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// POLYMER WORKAROUND: Snowshoes powder-snow walk is server-side in Artifacts; vanilla/Polymer clients sink without
// local footing. Send per-viewer ClientboundBlockUpdatePacket snow-block fakes that never touch the ServerLevel —
// other players and mobs are unaffected.
public final class SnowshoesClientSnowSupport {
	private static final int HORIZONTAL_RADIUS = 1;
	private static final BlockState FAKE_FOOTING = Blocks.SNOW_BLOCK.defaultBlockState();

	private static final Map<UUID, Set<BlockPos>> ACTIVE = new HashMap<>();

	private SnowshoesClientSnowSupport() {
	}

	public static void tick(ServerPlayer player) {
		if (!PolymerClientChecks.lacksArtifactsClient(player)) {
			clear(player);
			return;
		}
		if (!canWalkOnPowderSnow(player)) {
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
			if (!target.contains(pos)) {
				sendRealBlock(player, pos);
			}
		}
		for (BlockPos pos : target) {
			if (!previous.contains(pos)) {
				sendFakeFooting(player, pos);
			}
		}
		ACTIVE.put(player.getUUID(), new HashSet<>(target));
	}

	public static void onDisconnect(ServerPlayer player) {
		clear(player);
	}

	private static void clear(ServerPlayer player) {
		Set<BlockPos> previous = ACTIVE.remove(player.getUUID());
		if (previous == null) {
			return;
		}
		for (BlockPos pos : previous) {
			sendRealBlock(player, pos);
		}
	}

	private static Set<BlockPos> footprint(ServerPlayer player) {
		BlockPos center = powderSnowCenter(player);
		if (center == null) {
			return Set.of();
		}
		Set<BlockPos> positions = new HashSet<>();
		ServerLevel level = (ServerLevel) player.level();
		for (int dx = -HORIZONTAL_RADIUS; dx <= HORIZONTAL_RADIUS; dx++) {
			for (int dz = -HORIZONTAL_RADIUS; dz <= HORIZONTAL_RADIUS; dz++) {
				BlockPos pos = center.offset(dx, 0, dz);
				if (canOverlay(level.getBlockState(pos))) {
					positions.add(pos.immutable());
				}
			}
		}
		return positions;
	}

	private static BlockPos powderSnowCenter(ServerPlayer player) {
		BlockPos pos = BlockPos.containing(player.getX(), player.getY() - 0.1, player.getZ());
		if (!player.level().getBlockState(pos).is(Blocks.POWDER_SNOW)) {
			pos = player.getOnPos();
		}
		if (!player.level().getBlockState(pos).is(Blocks.POWDER_SNOW)) {
			return null;
		}
		return pos.immutable();
	}

	private static boolean canOverlay(BlockState state) {
		return state.is(Blocks.POWDER_SNOW);
	}

	private static void sendFakeFooting(ServerPlayer player, BlockPos pos) {
		player.connection.send(new ClientboundBlockUpdatePacket(pos, FAKE_FOOTING));
	}

	private static void sendRealBlock(ServerPlayer player, BlockPos pos) {
		BlockState state = ((ServerLevel) player.level()).getBlockState(pos);
		player.connection.send(new ClientboundBlockUpdatePacket(pos, state));
	}

	public static boolean canWalkOnPowderSnow(Player player) {
		if (!(player instanceof LivingEntity living)) {
			return false;
		}
		return ModDataComponents.WALK_ON_POWDER_SNOW.on(living).findAny();
	}
}
