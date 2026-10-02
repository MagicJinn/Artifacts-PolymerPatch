package dev.magicjinn.artifactspatch.worn;

import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public record WornArtifactLayout(Vec3 offset, Vector3f scale, ItemDisplayContext displayContext) {
	public static WornArtifactLayout forGroup(String group) {
		return switch (group) {
			case "head" -> new WornArtifactLayout(new Vec3(0, 1.45, 0), new Vector3f(0.55f), ItemDisplayContext.HEAD);
			case "chest" -> new WornArtifactLayout(new Vec3(0, 1.15, -0.12), new Vector3f(0.5f), ItemDisplayContext.FIXED);
			case "legs" -> new WornArtifactLayout(new Vec3(0, 0.82, -0.08), new Vector3f(0.45f), ItemDisplayContext.FIXED);
			case "feet" -> new WornArtifactLayout(new Vec3(0, 0.05, 0), new Vector3f(0.55f), ItemDisplayContext.FIXED);
			case "hand" -> new WornArtifactLayout(new Vec3(-0.35, 1.05, 0), new Vector3f(0.45f), ItemDisplayContext.THIRD_PERSON_RIGHT_HAND);
			case "offhand" -> new WornArtifactLayout(new Vec3(0.35, 1.05, 0), new Vector3f(0.45f), ItemDisplayContext.THIRD_PERSON_LEFT_HAND);
			default -> new WornArtifactLayout(new Vec3(0, 1.0, 0), new Vector3f(0.45f), ItemDisplayContext.FIXED);
		};
	}
}
