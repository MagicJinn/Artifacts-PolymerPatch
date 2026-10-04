package magicjinn.artifactspatch.res;

import java.util.Set;

/**
 * Artifacts items that ship a separate cuboid {@code models/item/*_held.json} (not flat generated icons).
 */
public final class HeldCuboidItemModels {
	public static final String UMBRELLA = "umbrella";

	/** Item ids whose {@code items/*.json} need hand display_context wiring in the Polymer pack. */
	public static final Set<String> HELD_DISPLAY_CONTEXT_ITEMS = Set.of(UMBRELLA);

	/** Cuboid held {@code models/item/*_held.json} files that need item-atlas texture cleanup in the pack. */
	public static final Set<String> HELD_CUBOID_MODEL_FILES = Set.of(
			"umbrella_held.json",
			"umbrella_held_blocking.json"
	);

	public static final String HELD_TEXTURE_PATH = "umbrella_held";

	/** Sprite id for held cuboid faces; stitched onto the blocks atlas for {@code CuboidItemModelWrapper}. */
	public static final String HELD_ITEM_ATLAS_SPRITE = "artifacts:item/" + HELD_TEXTURE_PATH;

	private HeldCuboidItemModels() {
	}
}
