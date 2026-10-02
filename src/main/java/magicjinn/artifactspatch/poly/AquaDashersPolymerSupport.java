package magicjinn.artifactspatch.poly;

import artifacts.component.SwimData;
import artifacts.platform.PlatformServices;
import artifacts.registry.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

/**
 * Detects Aqua-Dashers water sprinting (matches Artifacts fluid-collision + swim data checks).
 */
public final class AquaDashersPolymerSupport {
	private AquaDashersPolymerSupport() {
	}

	public static boolean isWaterSprinting(Player player) {
		if (!(player instanceof LivingEntity living)) {
			return false;
		}
		if (!player.isSprinting()) {
			return false;
		}
		SwimData swimData = PlatformServices.getPlatformHelper().getSwimData(living);
		if (swimData == null || swimData.shouldBreakSurfaceTension()) {
			return false;
		}
		if (!ModDataComponents.FLUID_COLLISION.on(living).findAny()) {
			return false;
		}
		FluidState fluid = fluidAtFeet(player);
		return fluid != null && fluid.is(FluidTags.WATER);
	}

	public static void stabilizeOnWaterSurface(Player player) {
		FluidState fluid = fluidAtFeet(player);
		if (fluid == null || fluid.isEmpty()) {
			return;
		}

		BlockPos blockPos = player.blockPosition();
		double surfaceY = blockPos.getY() + fluid.getHeight(player.level(), blockPos);
		Vec3 motion = player.getDeltaMovement();

		double y = player.getY();
		if (y < surfaceY - 0.02) {
			y = surfaceY;
		} else if (y > surfaceY + 0.12) {
			player.setDeltaMovement(motion.x, Math.min(motion.y, -0.05), motion.z);
		} else {
			y = surfaceY;
			player.setDeltaMovement(motion.x, 0, motion.z);
		}

		if (Math.abs(player.getY() - y) > 0.0005) {
			player.setPos(player.getX(), y, player.getZ());
		}

		player.setOnGround(true);
		player.resetFallDistance();
		player.fallDistance = 0;
		player.setSwimming(false);
	}

	private static FluidState fluidAtFeet(Player player) {
		BlockPos pos = BlockPos.containing(player.getX(), player.getY() - 0.2, player.getZ());
		FluidState fluid = player.level().getFluidState(pos);
		if (!fluid.isEmpty()) {
			return fluid;
		}
		pos = player.blockPosition();
		return player.level().getFluidState(pos);
	}
}
