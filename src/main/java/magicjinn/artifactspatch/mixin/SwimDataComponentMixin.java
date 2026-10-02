package magicjinn.artifactspatch.mixin;

import artifacts.fabric.component.SwimDataComponent;
import artifacts.network.payload.UpdateSwimFlyingPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.spongepowered.asm.mixin.Mixin;

// POLYMER WORKAROUND: Swim state is server-driven; syncing Cardinal Components swim data to clients without
// the Artifacts swim packet is unnecessary and can confuse vanilla clients.
@Mixin(SwimDataComponent.class)
public abstract class SwimDataComponentMixin implements AutoSyncedComponent {
	@Override
	public boolean shouldSyncWith(ServerPlayer player) {
		return ServerPlayNetworking.canSend(player, UpdateSwimFlyingPacket.TYPE);
	}
}
