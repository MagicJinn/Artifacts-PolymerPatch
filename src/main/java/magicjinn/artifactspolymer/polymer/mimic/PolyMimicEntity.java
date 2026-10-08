package magicjinn.artifactspolymer.polymer.mimic;

import java.util.List;

import org.joml.Vector3f;
import org.joml.Vector3fc;

import com.mojang.datafixers.util.Pair;

import artifacts.entity.MimicEntity;
import artifacts.registry.ModItems;
import eu.pb4.factorytools.api.virtualentity.emuvanilla.poly.SimpleEntityModel;
import eu.pb4.polymer.core.api.entity.PolymerEntity;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.IdentifiedUniqueEntityAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.UniqueIdentifiableAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import magicjinn.artifactspolymer.ArtifactsPolymerPatch;
import magicjinn.artifactspolymer.polymer.PolymerClientChecks;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.Brightness;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;

public final class PolyMimicEntity implements PolymerEntity {
    // private final MimicEntity mimic;
    // TODO: Remove this?
    // private static final Identifier VISUAL_ATTACHMENT =
    // ArtifactsPolymerPatch.id("mimic_visual");
    // // Item displays initialize at 0.5f scale, so we need to scale them up to 1
    // private static final Vector3fc ITEM_DISPLAY_SCALE = new Vector3f(2f);
    // // Item displays initialize at origin, so to center them, we translate to 0.5
    // private static final Vector3fc ITEM_DISPLAY_TRANSLATION = new Vector3f(0f,
    // 0.5f, 0f);

    public PolyMimicEntity(MimicEntity mimic) {
        if (UniqueIdentifiableAttachment.get(mimic, MimicPolymerEntityModels.ATTACHMENT_ID) == null) {
            IdentifiedUniqueEntityAttachment.ofTicking(
                    MimicPolymerEntityModels.ATTACHMENT_ID,
                    new MimicSimpleEntityModel(mimic),
                    mimic);
        }
    }

    @Override
    public EntityType<?> getPolymerEntityType(PacketContext context) {
        return EntityType.ITEM_DISPLAY;
    }

    // Never show equipment
    @Override
    public List<Pair<EquipmentSlot, ItemStack>> getPolymerVisibleEquipment(
            List<Pair<EquipmentSlot, ItemStack>> items,
            ServerPlayer player) {
        return List.of();
    }

    /**
     * Hides virtual mesh from Artifacts clients that already render the real Mimic.
     */
    private static final class MimicSimpleEntityModel extends SimpleEntityModel<MimicEntity> {
        private MimicSimpleEntityModel(MimicEntity entity) {
            super(entity, MimicPolymerEntityModels.MIMIC_POLY_MODEL);
        }

        @Override
        public boolean startWatching(ServerGamePacketListenerImpl player) {
            if (!PolymerClientChecks.lacksArtifactsClient(player.player)) {
                return false;
            }
            return super.startWatching(player);
        }
    }

    // private static final class MimicVisualHolder extends ElementHolder {
    // private final MimicEntity mimic;
    // private final ItemDisplayElement display;
    // private boolean showMouthOpen;
    // private int lastPackedBrightness = Integer.MIN_VALUE;

    // public MimicVisualHolder(MimicEntity mimic) {
    // this.mimic = mimic;
    // this.display = new ItemDisplayElement();
    // this.display.setItemDisplayContext(ItemDisplayContext.FIXED);
    // this.display.setScale(ITEM_DISPLAY_SCALE);
    // this.display.setTranslation(ITEM_DISPLAY_TRANSLATION);
    // this.display.setInterpolationDuration(0);
    // this.display.setTeleportDuration(2);
    // this.display.setVisibilityPredicate(PolymerClientChecks::lacksArtifactsClient);
    // applyVisual(isInAir());
    // this.display.setYaw(mimic.getYRot());
    // updateBrightness();
    // addElement(this.display);
    // }

    // @Override
    // public void onTick() {
    // boolean inAir = isInAir();
    // if (inAir != showMouthOpen)
    // applyVisual(inAir);

    // display.setYaw(mimic.getYRot()); // No-op when unchanged
    // updateBrightness();
    // }

    // // POLYMER WORKAROUND: Use Spawn Egg as the open-mouthed mimic
    // private void applyVisual(boolean inAir) {
    // showMouthOpen = inAir;
    // if (inAir) {
    // // Spawn egg is a perfect mimic 3D model. Use it as the open-mouthed mimic.
    // this.display.setItem(new ItemStack(ModItems.MIMIC_SPAWN_EGG.value()));
    // } else {
    // // When on the ground, use a chest as the closed-mouthed mimic.
    // this.display.setItem(new ItemStack(Items.CHEST));
    // }
    // }

    // // POLYMER WORKAROUND: Display entities can clip into walls, but making them
    // // fullbright breaks illusion in caves. Mimic center is always in a non-solid
    // // block, so we can use this light level for the entire mimic
    // private void updateBrightness() {
    // BlockPos centerPos =
    // BlockPos.containing(this.mimic.getBoundingBox().getCenter());
    // Level level = this.mimic.level(); // Get mimic dimension
    // Brightness brightness = new Brightness(
    // level.getBrightness(LightLayer.BLOCK, centerPos),
    // level.getBrightness(LightLayer.SKY, centerPos));

    // int packedBrightness = brightness.pack();
    // // Only update brightness if it has changed
    // if (packedBrightness != lastPackedBrightness) {
    // lastPackedBrightness = packedBrightness;
    // this.display.setBrightness(brightness);
    // }
    // }

    // private boolean isInAir() {
    // return mimic.ticksInAir > 0;
    // }
    // }
}
