package magicjinn.artifactspatch.res;

import java.util.Set;

/**
 * Artifacts items that ship a separate cuboid {@code models/item/*_held.json} (not flat generated icons).
 */
public final class HeldCuboidItemModels {
	public static final String UMBRELLA = "umbrella";

	/** Item ids whose {@code items/*.json} need hand display_context wiring in the Polymer pack. */
	public static final Set<String> HELD_DISPLAY_CONTEXT_ITEMS = Set.of(UMBRELLA);

	/** {@code models/item/*.json} files that need single-atlas particle fixes in the generated pack. */
	public static final Set<String> HELD_MODEL_FILES_WITH_PARTICLE_FIX = Set.of(
			"umbrella_held.json"
	);

	private HeldCuboidItemModels() {
	}
}
