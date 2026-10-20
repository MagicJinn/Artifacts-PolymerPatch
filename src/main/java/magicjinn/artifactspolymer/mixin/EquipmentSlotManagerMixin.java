package magicjinn.artifactspolymer.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import artifacts.equipment.EquipmentSlotManager;
import net.minecraft.world.entity.Entity;

@Mixin(EquipmentSlotManager.class)
public class EquipmentSlotManagerMixin {
    // POLYMER WORKAROUND: Artifacts passes the wearer as Level.playSound's "except"
    // player so the client mod can play the equip sound locally. Vanilla Polymer
    // clients never get that packet. Broadcast to everyone including the wearer.
    @ModifyArg(method = "playEquipSound", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;playSound(Lnet/minecraft/world/entity/Entity;DDDLnet/minecraft/core/Holder;Lnet/minecraft/sounds/SoundSource;FF)V"), index = 0)
    private static Entity includeWearerInEquipSound(Entity except) {
        return null;
    }
}
