package magicjinn.artifactspatch.poly;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import eu.pb4.polymer.blocks.impl.PolymerBlocksInternal;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.polymer.resourcepack.api.ResourcePackBuilder;
import magicjinn.artifactspatch.ArtifactsPolymerPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

// POLYMER WORKAROUND: BlockDisplay sends raw BlockStates; ensure polymer pack blockstate variants map them to mimic models.
public final class MimicDisplayBlockPack {
	private MimicDisplayBlockPack() {
	}

	public static void register() {
		PolymerResourcePackUtils.RESOURCE_PACK_CREATION_EVENT.register(builder ->
				builder.addPreFinishTask(MimicDisplayBlockPack::injectDisplayBlockModels)
		);
	}

	private static void injectDisplayBlockModels(ResourcePackBuilder builder) {
		BlockState bottom = MimicDisplayBlocks.bottomVisual();
		BlockState lid = MimicDisplayBlocks.lidVisual();
		if (bottom.is(Blocks.AIR)) {
			ArtifactsPolymerPatch.LOGGER.warn("Mimic bottom display block not allocated; skipping pack blockstate overrides");
		} else {
			mergeVariant(builder, bottom, ArtifactsPolymerPatch.id("block/mimic_bottom"));
		}
		if (lid.is(Blocks.AIR)) {
			ArtifactsPolymerPatch.LOGGER.warn("Mimic lid display block not allocated; skipping pack blockstate overrides");
		} else {
			mergeVariant(builder, lid, ArtifactsPolymerPatch.id("block/mimic_lid"));
		}
	}

	private static void mergeVariant(ResourcePackBuilder builder, BlockState state, Identifier modelId) {
		var blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
		var path = "assets/" + blockId.getNamespace() + "/blockstates/" + blockId.getPath() + ".json";
		var variantKey = PolymerBlocksInternal.generateStateName(state);

		var apply = new JsonArray();
		var modelEntry = new JsonObject();
		modelEntry.addProperty("model", modelId.toString());
		apply.add(modelEntry);

		JsonObject root = new JsonObject();
		JsonObject variants = new JsonObject();

		var existing = builder.getStringDataOrSource(path);
		if (existing != null && !existing.isEmpty()) {
			root = JsonParser.parseString(existing).getAsJsonObject();
			if (root.has("variants")) {
				variants = root.getAsJsonObject("variants").deepCopy();
			}
		}

		variants.add(variantKey, apply);
		root.add("variants", variants);
		builder.addStringData(path, root.toString());
	}
}
