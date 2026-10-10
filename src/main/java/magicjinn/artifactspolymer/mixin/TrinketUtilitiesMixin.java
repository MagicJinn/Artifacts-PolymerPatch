package magicjinn.artifactspolymer.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import artifacts.component.Equipable;
import artifacts.registry.ModDataComponents;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.callback.TrinketCallback;
import eu.pb4.trinkets.impl.TrinketUtilities;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gameevent.GameEvent;

@Mixin(TrinketUtilities.class)
public class TrinketUtilitiesMixin {
    // POLYMER WORKAROUND: Play equip sounds server-side at the wearer. Prefer
    // Artifacts' Equipable sound over Trinkets' generic fallback.
    @Inject(method = "playEquipmentSound", at = @At("HEAD"), cancellable = true)
    private static void playEquipSoundAtPlayer(ItemStack stack, TrinketSlotAccess slot, LivingEntity entity,
            CallbackInfo ci) {
        if (stack.isEmpty()) {
            ci.cancel();
            return;
        }

        Equipable equipable = stack.get(ModDataComponents.EQUIPABLE.get());
        Holder<SoundEvent> sound = equipable != null
                ? equipable.equipSound()
                : TrinketCallback.getCallback(stack).getEquipSound(stack, slot, entity);
        if (sound == null) {
            ci.cancel();
            return;
        }

        entity.gameEvent(GameEvent.EQUIP);
        entity.level().playSound(
                null,
                entity.getX(), entity.getY(), entity.getZ(),
                sound,
                entity.getSoundSource(),
                1.0F,
                1.0F);
        ci.cancel();
    }
}
