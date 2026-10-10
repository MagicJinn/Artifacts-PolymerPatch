package magicjinn.artifactspolymer;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import artifacts.Artifacts;
import eu.pb4.polymer.core.api.utils.PolymerUtils;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import magicjinn.artifactspolymer.polymer.AquaDashersPolymerPatch;
import magicjinn.artifactspolymer.polymer.CloudInABottlePolymerPatch;
import magicjinn.artifactspolymer.polymer.EverlastingFoodPolymerPatch;
import magicjinn.artifactspolymer.polymer.HeliumFlamingoPolymerPatch;
import magicjinn.artifactspolymer.polymer.NonSlipIceBlocks;
import magicjinn.artifactspolymer.polymer.SnowshoesPolymerPatch;
import magicjinn.artifactspolymer.polymer.SteadfastSpikesPolymerPatch;
import magicjinn.artifactspolymer.polymer.StriderShoesPolymerPatch;
import magicjinn.artifactspolymer.resourcepack.ResourcePackSetup;

public class ArtifactsPolymerPatch implements ModInitializer {
	public static final String MOD_ID = "artifacts-polymer-patch";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

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

		ServerPlayConnectionEvents.DISCONNECT.register((listener, server) -> {
			AquaDashersPolymerPatch.onDisconnect(listener.player);
			StriderShoesPolymerPatch.onDisconnect(listener.player);
			SnowshoesPolymerPatch.onDisconnect(listener.player);
			SteadfastSpikesPolymerPatch.onDisconnect(listener.player);
			CloudInABottlePolymerPatch.onDisconnect(listener.player);
			HeliumFlamingoPolymerPatch.onDisconnect(listener.player);
		});

	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
