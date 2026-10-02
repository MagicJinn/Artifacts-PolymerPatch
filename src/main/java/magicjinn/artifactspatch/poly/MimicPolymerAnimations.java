package magicjinn.artifactspatch.poly;

// POLYMER WORKAROUND: Mirrors artifacts.client.mimic.MimicModel#setChestRotations for virtual display entities.
public final class MimicPolymerAnimations {
	private static final float DEG_TO_RAD = 0.017453292f;

	private MimicPolymerAnimations() {
	}

	public static float animationTicks(int ticksInAir) {
		if (ticksInAir <= 0) {
			return 0.0f;
		}
		return ticksInAir - 0.5f;
	}

	public static float lidPitchRadians(float animationTicks) {
		if (animationTicks <= 0.0f) {
			return 0.0f;
		}
		return Math.max(-60.0f, animationTicks * -6.0f) * DEG_TO_RAD;
	}

	public static float bottomPitchRadians(float animationTicks) {
		if (animationTicks <= 0.0f) {
			return 0.0f;
		}
		return Math.min(30.0f, animationTicks * 3.0f) * DEG_TO_RAD;
	}
}
