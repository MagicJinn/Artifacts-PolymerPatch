package magicjinn.artifactspolymer;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ClientboundPlayChannelEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import artifacts.Artifacts;
import artifacts.network.payload.UpdateConfigValuePacket;
import eu.pb4.polymer.core.api.utils.PolymerUtils;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import magicjinn.artifactspolymer.polymer.AquaDashersPolymerPatch;
import magicjinn.artifactspolymer.polymer.CharmOfSinkingPolymerPatch;
import magicjinn.artifactspolymer.polymer.CloudInABottlePolymerPatch;
import magicjinn.artifactspolymer.polymer.EverlastingFoodPolymerPatch;
import magicjinn.artifactspolymer.polymer.HeliumFlamingoPolymerPatch;
import magicjinn.artifactspolymer.polymer.NonSlipIceBlocks;
import magicjinn.artifactspolymer.polymer.SnowshoesPolymerPatch;
import magicjinn.artifactspolymer.polymer.SteadfastSpikesPolymerPatch;
import magicjinn.artifactspolymer.polymer.StriderShoesPolymerPatch;
import magicjinn.artifactspolymer.polymer.SwimSpeedPolymerPatch;
import magicjinn.artifactspolymer.resourcepack.ResourcePackSetup;

public class ArtifactsPolymerPatch implements ModInitializer {
	public static final String MOD_ID = "artifacts-polymer-patch";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static final Component ARTIFACTS_CLIENT_KICK = Component.literal(
			"This server runs Artifacts through Polymer for vanilla clients only. Remove the Artifacts mod from your client to join.");

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing Artifacts Polymer Patch!");

		PolymerResourcePackUtils.addModAssets(Artifacts.MOD_ID);
		PolymerResourcePackUtils.addModAssets(MOD_ID);

		// POLYMER WORKAROUND: Register textured ice stand-ins before pack generation
		// so polymer-blocks includes their models in the server resource pack.
		NonSlipIceBlocks.register();
		ResourcePackSetup.setup();

		// Mark all worldgen features as server-side
		PolymerUtils.markAsServerOnlyRegistry(Registries.FEATURE);

		EverlastingFoodPolymerPatch.patch();

		// Kick Artifacts clients. canSend is reliable after channel REGISTER. JOIN is a
		// backup.
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			server.execute(() -> kickIfArtifactsClient(handler.player));
		});
		ClientboundPlayChannelEvents.REGISTER.register((handler, sender, server, channels) -> {
			kickIfArtifactsClient(handler.player);
		});

		ServerPlayConnectionEvents.DISCONNECT.register((listener, server) -> {
			clearPlayerState(listener.player);
		});

		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			clearPlayerStateOnRespawn(oldPlayer);
		});

		// Integrated server keeps the JVM across world loads. Drop static maps so
		// boss bars / footing / spoof state cannot leak into the next world.
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> clearAllPlayerState());
	}

	/** Artifacts registers UpdateConfigValuePacket only with its client mod. */
	private static void kickIfArtifactsClient(ServerPlayer player) {
		if (player.connection == null || !ServerPlayNetworking.canSend(player, UpdateConfigValuePacket.TYPE))
			return;

		LOGGER.info("Kicking {}: Artifacts client is not allowed on this Polymer server",
				player.getGameProfile().name());
		player.connection.disconnect(ARTIFACTS_CLIENT_KICK);
	}

	private static void clearPlayerState(ServerPlayer player) {
		safeCleanup("aqua_dashers", () -> AquaDashersPolymerPatch.onDisconnect(player));
		safeCleanup("charm_of_sinking", () -> CharmOfSinkingPolymerPatch.onDisconnect(player));
		safeCleanup("strider_shoes", () -> StriderShoesPolymerPatch.onDisconnect(player));
		safeCleanup("snowshoes", () -> SnowshoesPolymerPatch.onDisconnect(player));
		safeCleanup("steadfast_spikes", () -> SteadfastSpikesPolymerPatch.onDisconnect(player));
		safeCleanup("cloud_in_a_bottle", () -> CloudInABottlePolymerPatch.onDisconnect(player));
		safeCleanup("helium_flamingo", () -> HeliumFlamingoPolymerPatch.onDisconnect(player));
		safeCleanup("swim_speed", () -> SwimSpeedPolymerPatch.onDisconnect(player));
	}

	private static void clearPlayerStateOnRespawn(ServerPlayer oldPlayer) {
		safeCleanup("aqua_dashers", () -> AquaDashersPolymerPatch.onRespawn(oldPlayer));
		safeCleanup("charm_of_sinking", () -> CharmOfSinkingPolymerPatch.onRespawn(oldPlayer));
		safeCleanup("strider_shoes", () -> StriderShoesPolymerPatch.onRespawn(oldPlayer));
		safeCleanup("snowshoes", () -> SnowshoesPolymerPatch.onRespawn(oldPlayer));
		safeCleanup("steadfast_spikes", () -> SteadfastSpikesPolymerPatch.onRespawn(oldPlayer));
		safeCleanup("cloud_in_a_bottle", () -> CloudInABottlePolymerPatch.onRespawn(oldPlayer));
		safeCleanup("helium_flamingo", () -> HeliumFlamingoPolymerPatch.onRespawn(oldPlayer));
		safeCleanup("swim_speed", () -> SwimSpeedPolymerPatch.onRespawn(oldPlayer));
	}

	private static void clearAllPlayerState() {
		safeCleanup("aqua_dashers", AquaDashersPolymerPatch::clearAll);
		safeCleanup("charm_of_sinking", CharmOfSinkingPolymerPatch::clearAll);
		safeCleanup("strider_shoes", StriderShoesPolymerPatch::clearAll);
		safeCleanup("snowshoes", SnowshoesPolymerPatch::clearAll);
		safeCleanup("steadfast_spikes", SteadfastSpikesPolymerPatch::clearAll);
		safeCleanup("cloud_in_a_bottle", CloudInABottlePolymerPatch::clearAll);
		safeCleanup("helium_flamingo", HeliumFlamingoPolymerPatch::clearAll);
		safeCleanup("swim_speed", SwimSpeedPolymerPatch::clearAll);
	}

	private static void safeCleanup(String name, Runnable cleanup) {
		try {
			cleanup.run();
		} catch (Throwable t) {
			LOGGER.error("Polymer patch cleanup failed: {}", name, t);
		}
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
