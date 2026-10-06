package magicjinn.artifactspatch.poly;

import artifacts.component.ability.DoubleJump;
import artifacts.registry.ModDataComponents;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// POLYMER WORKAROUND: Cloud in a Bottle double jump is driven by Artifacts' client input handler
// (CloudInABottleInputHandler -> DoubleJumpPacket). Vanilla/Polymer clients never send that packet.
// Mirror the same press/release edge detection on the server using getLastClientInput().jump(),
// then call Artifacts' DoubleJump.jump (sound included) and sync motion to the client.
public final class CloudInABottlePolymerSupport {
	private static final Map<UUID, State> STATES = new HashMap<>();

	private CloudInABottlePolymerSupport() {
	}

	public static void tick(ServerPlayer player) {
		if (!PolymerClientChecks.lacksArtifactsClient(player)) {
			STATES.remove(player.getUUID());
			return;
		}
		if (!ModDataComponents.DOUBLE_JUMP.on(player).findAny()) {
			STATES.remove(player.getUUID());
			return;
		}

		State state = STATES.computeIfAbsent(player.getUUID(), _ -> new State());
		boolean jump = player.getLastClientInput().jump();

		if (isGroundedForDoubleJump(player)) {
			state.hasReleasedJumpKey = false;
			state.canDoubleJump = true;
		} else if (!jump) {
			state.hasReleasedJumpKey = true;
		} else if (!player.getAbilities().flying && state.canDoubleJump && state.hasReleasedJumpKey) {
			state.canDoubleJump = false;
			DoubleJump.jump(player);
			player.connection.send(new ClientboundSetEntityMotionPacket(player));
		}
	}

	public static void onDisconnect(ServerPlayer player) {
		STATES.remove(player.getUUID());
	}

	private static boolean isGroundedForDoubleJump(ServerPlayer player) {
		if (!(player.onGround() || player.onClimbable())) {
			return false;
		}
		if (!player.isInWater()) {
			return true;
		}
		// Match Artifacts client: water counts as grounded unless Charm of Sinking is active.
		return ModDataComponents.SINKING.on(player).findAny();
	}

	private static final class State {
		boolean canDoubleJump;
		boolean hasReleasedJumpKey;
	}
}
