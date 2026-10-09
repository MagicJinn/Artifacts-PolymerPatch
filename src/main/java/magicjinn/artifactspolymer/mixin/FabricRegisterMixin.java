package magicjinn.artifactspolymer.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import artifacts.entity.MimicEntity;
import artifacts.fabric.registry.FabricRegister;
import artifacts.registry.Register;
import artifacts.registry.RegistryHolder;
import eu.pb4.polymer.core.api.entity.PolymerEntityUtils;
import eu.pb4.polymer.core.api.item.PolymerCreativeModeTabUtils;
import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.core.api.other.PolymerComponent;
import eu.pb4.polymer.core.api.other.PolymerConsumeEffect;
import eu.pb4.polymer.core.api.other.PolymerMobEffect;
import eu.pb4.polymer.core.api.other.PolymerSoundEvent;
import eu.pb4.polymer.core.api.utils.PolymerSyncedObject;
import magicjinn.artifactspolymer.ArtifactsPolymerPatch;
import magicjinn.artifactspolymer.polymer.ArtifactsPolymerItem;
import magicjinn.artifactspolymer.polymer.mimic.MimicPolymerEntity;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.consume_effects.ConsumeEffect;
import net.minecraft.world.level.gameevent.GameEvent;

@Mixin(FabricRegister.class)
public class FabricRegisterMixin<R> {
    @Inject(method = "bind", at = @At("RETURN"))
    // Class to convert all succesful registry bindings to Polymer equivalents.
    // Don't ask me how it works, I don't know.
    private <T extends R> void polymerize(RegistryHolder<R, T> holder, CallbackInfo ci) {
        if (!holder.isBound())
            return;

        @SuppressWarnings("unchecked")
        Register<R> register = (Register<R>) (Object) this;
        ResourceKey<Registry<R>> registryKey = register.getRegistry();
        R value = holder.value();

        if (registryKey.equals(Registries.ITEM)) { // Items
            Item item = (Item) value;
            PolymerItem.registerOverlay(item, new ArtifactsPolymerItem(item));
        } else if (registryKey.equals(Registries.ENTITY_TYPE)) { // Entities
            EntityType<?> entityType = (EntityType<?>) value;
            PolymerEntityUtils.registerOverlay(entityType, entity -> {
                if (entity instanceof MimicEntity mimic)
                    return new MimicPolymerEntity(mimic);

                return context -> EntityType.ARMOR_STAND;
            });
        } else if (registryKey.equals(Registries.SOUND_EVENT)) { // Sound Events
            PolymerSoundEvent.registerOverlay((SoundEvent) value);
        } else if (registryKey.equals(Registries.DATA_COMPONENT_TYPE)) { // Data Component Types
            PolymerComponent.registerDataComponent((DataComponentType<?>) value);
        } else if (registryKey.equals(Registries.MOB_EFFECT)) { // Mob Effects
            PolymerMobEffect.registerOverlay((MobEffect) value);
        } else if (registryKey.equals(Registries.ATTRIBUTE)) { // Attributes
            @SuppressWarnings("unchecked")
            Holder<Attribute> attributeHolder = (Holder<Attribute>) holder;
            PolymerEntityUtils.registerAttribute(attributeHolder);
        } else if (registryKey.equals(Registries.GAME_EVENT)) { // Game Events
            PolymerSyncedObject.setSyncedObject(BuiltInRegistries.GAME_EVENT,
                    (GameEvent) value,
                    (_, _) -> GameEvent.ENTITY_ACTION.value());
        } else if (registryKey.equals(Registries.CREATIVE_MODE_TAB)) {
            var tab = (CreativeModeTab) value;
            PolymerCreativeModeTabUtils.registerPolymerCreativeModeTab(holder.unwrapKey().orElseThrow().identifier(),
                    tab);
        } else if (registryKey.equals(Registries.LOOT_CONDITION_TYPE)
                || registryKey.equals(Registries.LOOT_FUNCTION_TYPE)
                || registryKey.equals(Registries.PLACEMENT_MODIFIER_TYPE)) {
            @SuppressWarnings("unchecked")
            Registry<Object> registry = (Registry<Object>) BuiltInRegistries.REGISTRY
                    .getValue(registryKey.identifier());
            PolymerSyncedObject.setSyncedObject(registry, value, (_, _) -> null);
        } else if (registryKey.equals(Registries.CONSUME_EFFECT_TYPE)) {
            PolymerConsumeEffect.registerConsumeEffect((ConsumeEffect.Type<?>) value);
        } else if (registryKey.equals(Registries.FEATURE)) {
            // Worldgen features are all server-side. Nothing for Polymer clients to sync.
        } else {
            ArtifactsPolymerPatch.LOGGER.error("Guhh? Unknown registry key: " + registryKey);
        }
    }
}
