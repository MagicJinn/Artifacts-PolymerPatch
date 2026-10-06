package magicjinn.artifactspatch.res;

/**
 * Artifacts items that ship a separate cuboid {@code models/item/*_held.json} (not flat generated icons).
 */
public final class HeldCuboidItemModels {
	public static final String UMBRELLA = "umbrella";

	/** Cuboid held model that needs the oak_log particle remapped to the items-atlas sprite. */
	public static final String HELD_TEXTURE_MODEL = "umbrella_held.json";

	/** Items-atlas sprite used by held cuboid faces ({@code textures/item/umbrella_held.png}). */
	public static final String HELD_ITEM_ATLAS_SPRITE = "artifacts:item/umbrella_held";

	private HeldCuboidItemModels() {
	}
}
