package magicjinn.artifactspolymer;

import net.fabricmc.api.ModInitializer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import artifacts.Artifacts;
import eu.pb4.polymer.core.api.utils.PolymerUtils;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import magicjinn.artifactspolymer.resourcepack.ResourcePackSetup;

public class ArtifactsPolymerPatch implements ModInitializer {
	public static final String MOD_ID = "artifacts-polymer-patch";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing Artifacts Polymer Patch!");

		PolymerResourcePackUtils.addModAssets(Artifacts.MOD_ID);
		PolymerResourcePackUtils.addModAssets(MOD_ID);

		ResourcePackSetup.setup();

		// Mark all worldgen features as server-side
		PolymerUtils.markAsServerOnlyRegistry(Registries.FEATURE);
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
