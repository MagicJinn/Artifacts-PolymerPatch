package magicjinn.artifactspatch;

import artifacts.Artifacts;
import magicjinn.artifactspatch.command.ArtifactsDebugCommand;
import magicjinn.artifactspatch.poly.PolymerClientChecks;
import magicjinn.artifactspatch.poly.PolymerPlayerAttributeSync;
import magicjinn.artifactspatch.poly.AquaDashersClientBarrierSupport;
import magicjinn.artifactspatch.poly.TogglePolymerSupport;
import magicjinn.artifactspatch.res.ResourcePackSetup;
import eu.pb4.polymer.core.api.utils.PolymerUtils;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.registries.Registries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArtifactsPolymerPatch implements ModInitializer {
	public static final String MOD_ID = "artifacts-polymer-patch";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		PolymerResourcePackUtils.addModAssets(Artifacts.MOD_ID);
		PolymerResourcePackUtils.addModAssets(MOD_ID);
		ResourcePackSetup.register();

		// POLYMER WORKAROUND: Equipment modifiers from Artifacts are server-side; push attribute packets on join
		// so vanilla clients see digging claws / similar bonuses for UI and break-speed estimates.
		ServerPlayConnectionEvents.JOIN.register((handler, _, server) -> {
			var player = handler.player;
			if (!PolymerClientChecks.lacksArtifactsClient(player)) {
				return;
			}
			server.execute(() -> {
				TogglePolymerSupport.enableEquippedToggles(player);
				PolymerPlayerAttributeSync.sync(player);
			});
		});

		// POLYMER WORKAROUND: Artifacts campsite features are server worldgen only; hide from client registry sync.
		PolymerUtils.markAsServerOnlyRegistry(Registries.FEATURE);
		TogglePolymerSupport.register();
		ServerPlayConnectionEvents.DISCONNECT.register((handler, _) ->
				AquaDashersClientBarrierSupport.onDisconnect(handler.player));
		ArtifactsDebugCommand.register();
		LOGGER.info("Artifacts Polymer patch initialized");
	}

	public static net.minecraft.resources.Identifier id(String path) {
		return net.minecraft.resources.Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
