package magicjinn.artifactspatch.mixin;

import artifacts.Artifacts;
import artifacts.entity.MimicEntity;
import artifacts.fabric.registry.FabricRegister;
import artifacts.registry.ModEntityTypes;
import artifacts.registry.Register;
import artifacts.registry.RegistryHolder;
import eu.pb4.polymer.rsm.api.RegistrySyncUtils;
import magicjinn.artifactspatch.poly.MimicPolymerEntity;
import magicjinn.artifactspatch.poly.PolyArtifactsItem;
import eu.pb4.polymer.core.api.entity.PolymerEntityUtils;
import eu.pb4.polymer.core.api.item.PolymerCreativeModeTabUtils;
import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.core.api.other.PolymerComponent;
import eu.pb4.polymer.core.api.other.PolymerConsumeEffect;
import eu.pb4.polymer.core.api.other.PolymerMobEffect;
import eu.pb4.polymer.core.api.other.PolymerSoundEvent;
import eu.pb4.polymer.core.api.utils.PolymerSyncedObject;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.consume_effects.ConsumeEffect;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.core.component.DataComponentType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FabricRegister.class)
public abstract class FabricRegisterMixin<R> {
	@Inject(method = "bind", at = @At("RETURN"))
	private <T extends R> void artifacts$polymerize(RegistryHolder<R, T> holder, CallbackInfo ci) {
		if (!holder.isBound()) {
			return;
		}

		@SuppressWarnings("unchecked")
		var register = (Register<R>) (Object) this;
		ResourceKey<Registry<R>> registryKey = register.getRegistry();
		R value = holder.value();

		if (registryKey.equals(Registries.ITEM)) {
			// POLYMER WORKAROUND: Register per-item polymer overlays when Artifacts binds registries (see PolyArtifactsItem).
			Item item = (Item) value;
			PolymerItem.registerOverlay(item, new PolyArtifactsItem(item));
		} else if (registryKey.equals(Registries.ENTITY_TYPE)) {
			@SuppressWarnings("unchecked")
			EntityType<?> entityType = (EntityType<?>) value;
			if (entityType == ModEntityTypes.MIMIC.get()) {
				// POLYMER WORKAROUND: Mimic uses INTERACTION hitbox + static chest BlockDisplay; see MimicPolymerEntity.
				PolymerEntityUtils.registerType(entityType);
				PolymerEntityUtils.registerOverlay((EntityType<MimicEntity>) entityType, MimicPolymerEntity::new);
			} else {
				// POLYMER WORKAROUND: Other Artifacts entities have no client mod; hide as vanilla armor stands on the wire.
				PolymerEntityUtils.registerOverlay(entityType, entity -> context -> EntityType.ARMOR_STAND);
			}
		} else if (registryKey.equals(Registries.SOUND_EVENT)) {
			PolymerSoundEvent.registerOverlay((SoundEvent) value);
		} else if (registryKey.equals(Registries.DATA_COMPONENT_TYPE)) {
			PolymerComponent.registerDataComponent((DataComponentType<?>) value);
		} else if (registryKey.equals(Registries.MOB_EFFECT)) {
			PolymerMobEffect.registerOverlay((MobEffect) value);
		} else if (registryKey.equals(Registries.ATTRIBUTE)) {
			@SuppressWarnings("unchecked")
			net.minecraft.core.Holder<Attribute> attributeHolder = (net.minecraft.core.Holder<Attribute>) holder;
			PolymerEntityUtils.registerAttribute(attributeHolder);
		} else if (registryKey.equals(Registries.GAME_EVENT)) {
			PolymerSyncedObject.setSyncedObject(
					net.minecraft.core.registries.BuiltInRegistries.GAME_EVENT,
					(GameEvent) value,
					(_, _) -> GameEvent.ENTITY_ACTION.value()
			);
		} else if (registryKey.equals(Registries.CREATIVE_MODE_TAB)) {
			var tab = (CreativeModeTab) value;
			PolymerCreativeModeTabUtils.registerPolymerCreativeModeTab(holder.unwrapKey().orElseThrow().identifier(), tab);
		} else if (registryKey.equals(Registries.LOOT_CONDITION_TYPE)
				|| registryKey.equals(Registries.LOOT_FUNCTION_TYPE)
				|| registryKey.equals(Registries.PLACEMENT_MODIFIER_TYPE)) {
			@SuppressWarnings("unchecked")
			net.minecraft.core.Registry<Object> registry = (net.minecraft.core.Registry<Object>) net.minecraft.core.registries.BuiltInRegistries.REGISTRY.getValue(registryKey.identifier());
			PolymerSyncedObject.setSyncedObject(registry, value, (_, _) -> null);
		} else if (registryKey.equals(Registries.CONSUME_EFFECT_TYPE)) {
			PolymerConsumeEffect.registerConsumeEffect((ConsumeEffect.Type<?>) value);
		}

		holder.unwrapKey().ifPresent(key -> {
			if (!Artifacts.MOD_ID.equals(key.identifier().getNamespace())) {
				return;
			}
			// POLYMER WORKAROUND: Mark Artifacts registry entries as server-only where needed so vanilla clients
			// are not sent unknown ids (paired with polymer overlays for visible content).
			@SuppressWarnings("unchecked")
			Registry<R> registry = (Registry<R>) BuiltInRegistries.REGISTRY.getValue(registryKey.identifier());
			if (registry != null) {
				RegistrySyncUtils.setServerEntry(registry, value);
			}
		});
	}
}
