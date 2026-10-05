package magicjinn.artifactspatch.poly;

import artifacts.entity.MimicEntity;
import artifacts.registry.ModItems;
import com.mojang.datafixers.util.Pair;
import eu.pb4.polymer.core.api.entity.PolymerEntity;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.IdentifiedUniqueEntityAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.UniqueIdentifiableAttachment;
import eu.pb4.polymer.virtualentity.api.data.InteractionEntityData;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import magicjinn.artifactspatch.ArtifactsPolymerPatch;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Vector3f;

import java.util.List;

/**
 * POLYMER WORKAROUND: Mimic has a complex client model. Use an interaction entity for the hitbox
 * plus an item display that swaps between a chest (ground/idle) and the mimic spawn egg (in air).
 */
public final class PolyMimicEntity implements PolymerEntity {
	private static final Identifier VISUAL_ATTACHMENT = ArtifactsPolymerPatch.id("mimic_visual");
	/** FIXED item models ship at 0.5 scale; 2x restores full block size. */
	private static final Vector3f DISPLAY_SCALE = new Vector3f(2f, 2f, 2f);
	/** FIXED models are centered on the entity origin; lift so the bottom sits on the ground. */
	private static final Vector3f DISPLAY_TRANSLATION = new Vector3f(0f, 0.5f, 0f);

	private final MimicEntity mimic;

	public PolyMimicEntity(MimicEntity mimic) {
		this.mimic = mimic;
		if (UniqueIdentifiableAttachment.get(mimic, VISUAL_ATTACHMENT) == null) {
			IdentifiedUniqueEntityAttachment.ofTicking(VISUAL_ATTACHMENT, new MimicVisualHolder(mimic), mimic);
		}
	}

	@Override
	public EntityType<?> getPolymerEntityType(PacketContext context) {
		// Interaction is invisible and exposes a real client hitbox (armor stand / item_display do not).
		return EntityType.INTERACTION;
	}

	@Override
	public void modifyRawTrackedData(List<SynchedEntityData.DataValue<?>> data, ServerPlayer player, boolean initial) {
		data.add(SynchedEntityData.DataValue.create(InteractionEntityData.WIDTH, this.mimic.getBbWidth()));
		data.add(SynchedEntityData.DataValue.create(InteractionEntityData.HEIGHT, this.mimic.getBbHeight()));
		data.add(SynchedEntityData.DataValue.create(InteractionEntityData.RESPONSE, true));
	}

	@Override
	public List<Pair<EquipmentSlot, ItemStack>> getPolymerVisibleEquipment(
			List<Pair<EquipmentSlot, ItemStack>> items,
			ServerPlayer player
	) {
		return List.of();
	}

	private static final class MimicVisualHolder extends ElementHolder {
		private final MimicEntity mimic;
		private final ItemDisplayElement display;
		private boolean showingSpawnEgg;

		private MimicVisualHolder(MimicEntity mimic) {
			this.mimic = mimic;
			this.display = new ItemDisplayElement();
			this.display.setItemDisplayContext(ItemDisplayContext.FIXED);
			this.display.setScale(DISPLAY_SCALE);
			this.display.setTranslation(DISPLAY_TRANSLATION);
			this.display.setInterpolationDuration(0);
			this.display.setTeleportDuration(2);
			this.display.setVisibilityPredicate(PolymerClientChecks::lacksArtifactsClient);
			applyVisual(mimic.ticksInAir > 0);
			this.display.setYaw(mimic.getYRot());
			addElement(this.display);
		}

		@Override
		protected void onTick() {
			boolean inAir = this.mimic.ticksInAir > 0;
			if (inAir != this.showingSpawnEgg) {
				applyVisual(inAir);
			}
			this.display.setYaw(this.mimic.getYRot());
		}

		private void applyVisual(boolean inAir) {
			this.showingSpawnEgg = inAir;
			if (inAir) {
				// Spawn egg item model is the open-mouthed 3D mimic.
				this.display.setItem(new ItemStack(ModItems.MIMIC_SPAWN_EGG.value()));
			} else {
				this.display.setItem(new ItemStack(Items.CHEST));
			}
		}
	}
}
