package magicjinn.artifactspatch.res;

import java.util.Set;

/**
 * Artifacts items that ship a separate cuboid {@code models/item/*_held.json} (not flat generated icons).
 */
public final class HeldCuboidItemModels {
	public static final String UMBRELLA = "umbrella";

	/** Item ids whose {@code items/*.json} need hand display_context wiring in the Polymer pack. */
	public static final Set<String> HELD_DISPLAY_CONTEXT_ITEMS = Set.of(UMBRELLA);

	/** Cuboid held models baked through {@code CuboidItemModelWrapper} (blocks atlas only). */
	public static final Set<String> HELD_CUBOID_MODEL_FILES = Set.of(
			"umbrella_held.json",
			"umbrella_held_blocking.json"
	);

	public static final String HELD_TEXTURE_PATH = "umbrella_held";

	private HeldCuboidItemModels() {
	}
}
