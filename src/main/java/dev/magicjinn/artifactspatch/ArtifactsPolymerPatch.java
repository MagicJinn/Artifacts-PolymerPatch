package dev.magicjinn.artifactspatch;

import artifacts.Artifacts;
import dev.magicjinn.artifactspatch.res.ResourcePackSetup;
import dev.magicjinn.artifactspatch.worn.PlayerWornArtifactsDisplay;
import eu.pb4.polymer.core.api.utils.PolymerUtils;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.fabricmc.api.ModInitializer;
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

		PolymerUtils.markAsServerOnlyRegistry(Registries.FEATURE);
		PlayerWornArtifactsDisplay.register();
		LOGGER.info("Artifacts Polymer patch initialized");
	}

	public static net.minecraft.resources.Identifier id(String path) {
		return net.minecraft.resources.Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
