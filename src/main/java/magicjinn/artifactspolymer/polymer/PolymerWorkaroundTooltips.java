package magicjinn.artifactspolymer.polymer;

import java.util.List;
import java.util.Map;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class PolymerWorkaroundTooltips {
    private static final Map<String, String> NOTES = Map.ofEntries(
            Map.entry("aqua_dashers", "Shows waterlogged barriers while sprinting to avoid desync."),
            Map.entry("strider_shoes", "Shows magma underfoot on lava so walking stays in sync."),
            Map.entry("snowshoes", "Shows snow blocks in powder snow so walking stays in sync."),
            Map.entry("steadfast_spikes",
                    "Ice blocks are replaced with non-slippery ice so you don't slip out of sync."),
            Map.entry("digging_claws", "Triggers block breaking early on the server."),
            Map.entry("umbrella", "Held upright like a spear so the canopy looks right."));

    /** Appends the tooltip for the given item stack. */
    public static void addMatchingTooltip(List<Component> tooltip, ItemStack stack) {
        if (stack.isEmpty())
            return;

        String note = NOTES.get(BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath());
        if (note == null)
            return;

        tooltip.add(Component.literal(note).withStyle(ChatFormatting.DARK_GRAY));
    }
}
