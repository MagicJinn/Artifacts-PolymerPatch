package magicjinn.artifactspolymer.mixin;

import artifacts.registry.ModDataComponents;
import artifacts.registry.ModItems;
import eu.pb4.polymer.core.api.utils.PolymerUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnderpearlItem.class)
public abstract class EnderpearlItemWarpDriveMixin {
    // POLYMER WORKAROUND: Survival players predict the use of an ender pearl, but
    // with warp drive enabled, it doesn't consume the pearl, causing a desync. We
    // sync the inventory after the pearl is thrown with warp drive enabled.
    @Inject(method = "use", at = @At("RETURN"))
    private void onUse(Level level, Player player, InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> cir) {
        if (!(player instanceof ServerPlayer serverPlayer))
            return;

        InteractionResult result = cir.getReturnValue();
        if (result == null || !result.consumesAction())
            return;

        if (!hasWarpDrive(serverPlayer))
            return;

        PolymerUtils.reloadInventory(serverPlayer);
    }

    /**
     * Equipped Warp Drive (trinket / ability slot) or sitting in the player
     * inventory. Equipped is what skips pearl consume. Inventory covers a loose
     * item the player might equip mid-stack.
     */
    private static boolean hasWarpDrive(ServerPlayer player) {
        if (ModDataComponents.ENDER_PEARL_HUNGER_COST.on(player).findAny())
            return true;

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(ModItems.WARP_DRIVE))
                return true;
        }
        return false;
    }
}
