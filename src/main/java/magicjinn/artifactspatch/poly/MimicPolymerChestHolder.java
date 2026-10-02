package magicjinn.artifactspatch.poly;

import artifacts.entity.MimicEntity;
import com.mojang.math.Transformation;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.elements.InteractionElement;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.world.entity.Display;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;

// POLYMER WORKAROUND: Hinged mimic lid + bottom ItemDisplays with opaque pack models (see MimicDisplayItems).
public final class MimicPolymerChestHolder extends ElementHolder {
	// Model geometry spans 12/16 of a block; ~1.125× ≈ mimic width (0.875 m).
	private static final float PART_MODEL_SCALE = 1.125f;
	private static final Vector3f PART_SCALE = new Vector3f(PART_MODEL_SCALE, PART_MODEL_SCALE, PART_MODEL_SCALE);
	// Sit inside the 0.875 m interaction box (was 0.1875: mouth floated above debug hitbox).
	private static final Vector3f DISPLAY_BASE_LIFT = new Vector3f(0.0f, -0.125f, 0.0f);
	private static final Vector3f MOUTH_PIVOT = new Vector3f(0.0f, 0.38f, 0.02f);
	private static final Vector3f BOTTOM_FROM_PIVOT = new Vector3f(0.0f, -0.14f, -0.26f);
	private static final Vector3f LID_FROM_PIVOT = new Vector3f(0.0f, 0.12f, -0.26f);

	private final MimicEntity mimic;
	private final ItemDisplayElement bottom;
	private final ItemDisplayElement lid;

	public MimicPolymerChestHolder(MimicEntity mimic) {
		this.mimic = mimic;
		this.bottom = createPart(MimicDisplayItems.BOTTOM);
		this.lid = createPart(MimicDisplayItems.LID);
		var hitbox = InteractionElement.redirect(mimic);
		hitbox.setSize(MimicPolymerEntity.MIMIC_HITBOX_SIZE, MimicPolymerEntity.MIMIC_HITBOX_SIZE);
		hitbox.setResponse(true);
		addElement(hitbox);
		addElement(bottom);
		addElement(lid);
	}

	private static ItemDisplayElement createPart(net.minecraft.world.item.Item item) {
		var element = new ItemDisplayElement(new ItemStack(item));
		element.setItemDisplayContext(ItemDisplayContext.FIXED);
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
				.translate(DISPLAY_BASE_LIFT)
				.translate(MOUTH_PIVOT)
				.rotateX(pitchRadians)
				.translate(offsetFromPivot)
				.scale(PART_SCALE);
		return new Transformation(matrix);
	}
}
