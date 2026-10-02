package magicjinn.artifactspatch.mixin;

import artifacts.component.ability.ToolTierUpgrade;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// POLYMER WORKAROUND: Artifacts' Fabric tool-tier mixin is client-only; vanilla clients gate harvest on local
// tool tier. Mirror digging claws tier upgrade on the server so drops match Artifacts behavior.
@Mixin(Player.class)
public abstract class PlayerToolTierPolymerMixin {
	@ModifyReturnValue(method = "hasCorrectToolForDrops", at = @At("RETURN"))
	private boolean artifactsPatch$diggingClawsToolTier(boolean original, BlockState state) {
		return original || ToolTierUpgrade.canHarvestWithTier((LivingEntity) (Object) this, state);
	}
}
