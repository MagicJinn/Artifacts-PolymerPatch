package magicjinn.artifactspatch.mixin;

import artifacts.effect.MagnetismMobEffect;
import magicjinn.artifactspatch.poly.MagnetismPolymerSupport;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MagnetismMobEffect.class)
public class MagnetismMobEffectPolymerMixin {
	@Inject(method = "applyEffectTick", at = @At("RETURN"))
	private void artifacts$syncItemMotionForPolymerClients(
			ServerLevel level,
			LivingEntity entity,
			int amplifier,
			CallbackInfoReturnable<Boolean> cir
	) {
		if (entity instanceof ServerPlayer player) {
			MagnetismPolymerSupport.syncPulledItems(level, player, amplifier);
		}
	}
}
