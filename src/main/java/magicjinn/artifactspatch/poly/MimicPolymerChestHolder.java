package magicjinn.artifactspatch.poly;

import artifacts.entity.MimicEntity;
import com.mojang.math.Transformation;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.elements.BlockDisplayElement;
import net.minecraft.world.entity.Display;
import net.minecraft.world.level.block.Blocks;
import org.joml.Matrix4f;
import org.joml.Vector3f;

// POLYMER WORKAROUND: Two-part hinged chest (lid + bottom) for Polymer clients; Artifacts uses MimicModel on the real client.
public final class MimicPolymerChestHolder extends ElementHolder {
	private static final float MIMIC_SCALE = 14f / 16f;
	private static final Vector3f PART_SCALE = new Vector3f(MIMIC_SCALE, MIMIC_SCALE * 0.5f, MIMIC_SCALE);
	// Shared mouth pivot (Artifacts MimicModel: lid and bottom share PartPose offset 0, 15, 7).
	private static final Vector3f MOUTH_PIVOT = new Vector3f(0.0f, 0.38f, 0.02f);
	private static final Vector3f BOTTOM_FROM_PIVOT = new Vector3f(0.0f, -0.14f, -0.26f);
	private static final Vector3f LID_FROM_PIVOT = new Vector3f(0.0f, 0.12f, -0.26f);

	private final MimicEntity mimic;
	private final BlockDisplayElement bottom;
	private final BlockDisplayElement lid;

	public MimicPolymerChestHolder(MimicEntity mimic) {
		this.mimic = mimic;
		this.bottom = createPart();
		this.lid = createPart();
		addElement(bottom);
		addElement(lid);
	}

	private static BlockDisplayElement createPart() {
		var element = new BlockDisplayElement(Blocks.CHEST.defaultBlockState());
		element.setBillboardMode(Display.BillboardConstraints.FIXED);
		element.setInterpolationDuration(1);
		element.setTeleportDuration(1);
		return element;
	}

	@Override
	public void tick() {
		super.tick();
		if (mimic.isRemoved()) {
			return;
		}
		float yaw = resolveYaw();
		bottom.setYaw(yaw);
		lid.setYaw(yaw);

		float air = MimicPolymerAnimations.animationTicks(mimic.ticksInAir);
		float lidPitch = MimicPolymerAnimations.lidPitchRadians(air);
		float bottomPitch = MimicPolymerAnimations.bottomPitchRadians(air);

		bottom.setTransformation(partTransform(BOTTOM_FROM_PIVOT, bottomPitch));
		lid.setTransformation(partTransform(LID_FROM_PIVOT, lidPitch));
		bottom.startInterpolationIfDirty();
		lid.startInterpolationIfDirty();
	}

	private float resolveYaw() {
		if (mimic.isDormant && mimic.facing != null) {
			return mimic.facing.toYRot();
		}
		return mimic.getYRot();
	}

	private static Transformation partTransform(Vector3f offsetFromPivot, float pitchRadians) {
		Matrix4f matrix = new Matrix4f()
				.translate(MOUTH_PIVOT)
				.rotateX(pitchRadians)
				.translate(offsetFromPivot)
				.scale(PART_SCALE);
		return new Transformation(matrix);
	}
}
