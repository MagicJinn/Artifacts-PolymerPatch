package magicjinn.artifactspatch.poly;

import artifacts.registry.ModAttributes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// POLYMER WORKAROUND: Steadfast Spikes slip resistance is applied in Artifacts' travel friction mixin.
// Vanilla/Polymer clients keep ice slipperiness for prediction. Until MC 26.2 friction_modifier, spoof
// underfoot as light-blue wool (friction 0.6) via ClientboundBlockUpdatePacket. ServerLevel untouched.
// TODO(26.2): replace wool spoof with a synced friction_modifier attribute spoof (no fake blocks).
public final class SteadfastSpikesClientIceSupport {
	private static final BlockState FAKE_FOOTING = Blocks.LIGHT_BLUE_WOOL.defaultBlockState();

	private static final Map<UUID, BlockPos> ACTIVE = new HashMap<>();

	private SteadfastSpikesClientIceSupport() {
	}

	public static void tick(ServerPlayer player) {
		if (!PolymerClientChecks.lacksArtifactsClient(player)) {
			clear(player);
			return;
		}
		if (!hasSlipResistance(player)) {
			clear(player);
			return;
		}

		BlockPos target = iceUnderFeet(player);
		if (target == null) {
			clear(player);
			return;
		}

		BlockPos previous = ACTIVE.get(player.getUUID());
		if (previous != null && !previous.equals(target)) {
			sendRealBlock(player, previous);
		}
		if (previous == null || !previous.equals(target)) {
			sendFakeFooting(player, target);
		}
		ACTIVE.put(player.getUUID(), target.immutable());
	}

	public static void onDisconnect(ServerPlayer player) {
		clear(player);
	}

	private static void clear(ServerPlayer player) {
		BlockPos previous = ACTIVE.remove(player.getUUID());
		if (previous == null) {
			return;
		}
		sendRealBlock(player, previous);
	}

	private static BlockPos iceUnderFeet(ServerPlayer player) {
		BlockPos pos = player.getBlockPosBelowThatAffectsMyMovement();
		BlockState state = player.level().getBlockState(pos);
		if (!state.is(BlockTags.ICE)) {
			pos = player.getOnPos();
			state = player.level().getBlockState(pos);
		}
		if (!state.is(BlockTags.ICE)) {
			return null;
		}
		return pos.immutable();
	}

	private static void sendFakeFooting(ServerPlayer player, BlockPos pos) {
		player.connection.send(new ClientboundBlockUpdatePacket(pos, FAKE_FOOTING));
	}

	private static void sendRealBlock(ServerPlayer player, BlockPos pos) {
		BlockState state = ((ServerLevel) player.level()).getBlockState(pos);
		player.connection.send(new ClientboundBlockUpdatePacket(pos, state));
	}

	private static boolean hasSlipResistance(ServerPlayer player) {
		if (!player.getAttributes().hasAttribute(ModAttributes.SLIP_RESISTANCE)) {
			return false;
		}
		return player.getAttributeValue(ModAttributes.SLIP_RESISTANCE) > 0.0001;
	}
}
