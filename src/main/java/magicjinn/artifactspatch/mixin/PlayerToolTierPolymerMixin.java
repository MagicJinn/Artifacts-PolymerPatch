package magicjinn.artifactspatch.mixin;

import artifacts.component.ability.ToolTierUpgrade;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Mirrors Artifacts' fabric tool-tier mixin so dedicated-server logic stays consistent for Polymer players.
 * (Artifacts fabric mixin; same behavior for harvest checks.)
 */
@Mixin(Player.class)
public abstract class PlayerToolTierPolymerMixin {
	@ModifyReturnValue(method = "hasCorrectToolForDrops", at = @At("RETURN"))
	private boolean artifactsPatch$diggingClawsToolTier(boolean original, BlockState state) {
		return original || ToolTierUpgrade.canHarvestWithTier((LivingEntity) (Object) this, state);
	}
}
