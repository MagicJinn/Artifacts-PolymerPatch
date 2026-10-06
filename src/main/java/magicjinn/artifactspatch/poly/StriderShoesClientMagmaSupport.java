package magicjinn.artifactspatch.poly;

import artifacts.event.ArtifactHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// POLYMER WORKAROUND: Strider shoes lava collision is server-side in Artifacts; vanilla/Polymer clients sink without
// local footing. Send per-viewer ClientboundBlockUpdatePacket magma fakes that never touch the ServerLevel —
// other players and mobs are unaffected.
public final class StriderShoesClientMagmaSupport {
	private static final BlockState FAKE_FOOTING = Blocks.MAGMA_BLOCK.defaultBlockState();

	private static final Map<UUID, Set<BlockPos>> ACTIVE = new HashMap<>();

	private StriderShoesClientMagmaSupport() {
	}

	public static void tick(ServerPlayer player) {
		if (!PolymerClientChecks.lacksArtifactsClient(player)) {
			clear(player);
			return;
		}
		if (!isLavaCrouching(player)) {
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
		BlockPos pos = lavaDeckCenter(player);
		if (pos == null) {
			return Set.of();
		}
		Set<BlockPos> positions = new HashSet<>();
		ServerLevel level = (ServerLevel) player.level();
		if (canOverlay(level.getBlockState(pos))) {
			positions.add(pos.immutable());
		}
		return positions;
	}

	private static BlockPos lavaDeckCenter(ServerPlayer player) {
		FluidState fluid = fluidAtFeet(player);
		if (fluid == null || !fluid.is(FluidTags.LAVA)) {
			return null;
		}
		BlockPos fluidPos = BlockPos.containing(player.getX(), player.getY() - 0.2, player.getZ());
		FluidState at = player.level().getFluidState(fluidPos);
		if (at.isEmpty()) {
			fluidPos = player.blockPosition();
			at = player.level().getFluidState(fluidPos);
		}
		if (!at.is(FluidTags.LAVA)) {
			return null;
		}
		double surfaceY = fluidPos.getY() + at.getHeight(player.level(), fluidPos);
		return BlockPos.containing(player.getX(), surfaceY - 0.125, player.getZ());
	}

	private static boolean canOverlay(BlockState state) {
		if (state.is(Blocks.LAVA)) {
			return true;
		}
		if (state.isAir()) {
			return true;
		}
		return state.getFluidState().is(FluidTags.LAVA);
	}

	private static void sendFakeFooting(ServerPlayer player, BlockPos pos) {
		player.connection.send(new ClientboundBlockUpdatePacket(pos, FAKE_FOOTING));
	}

	private static void sendRealBlock(ServerPlayer player, BlockPos pos) {
		BlockState state = ((ServerLevel) player.level()).getBlockState(pos);
		player.connection.send(new ClientboundBlockUpdatePacket(pos, state));
	}

	public static boolean isLavaCrouching(Player player) {
		if (!(player instanceof LivingEntity living)) {
			return false;
		}
		if (!player.isCrouching()) {
			return false;
		}
		FluidState fluid = fluidAtFeet(player);
		if (fluid == null || !fluid.is(FluidTags.LAVA)) {
			return false;
		}
		return ArtifactHooks.onFluidCollision(living, fluid);
	}

	private static FluidState fluidAtFeet(Player player) {
		BlockPos pos = BlockPos.containing(player.getX(), player.getY() - 0.2, player.getZ());
		FluidState fluid = player.level().getFluidState(pos);
		if (!fluid.isEmpty()) {
			return fluid;
		}
		pos = player.blockPosition();
		return player.level().getFluidState(pos);
	}
}
