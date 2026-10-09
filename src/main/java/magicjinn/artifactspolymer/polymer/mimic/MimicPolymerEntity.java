package magicjinn.artifactspolymer.polymer.mimic;

import artifacts.entity.MimicEntity;
import eu.pb4.factorytools.api.virtualentity.emuvanilla.poly.SimpleEntityModel;
import eu.pb4.polymer.core.api.entity.PolymerEntity;
import eu.pb4.polymer.virtualentity.api.attachment.IdentifiedUniqueEntityAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.UniqueIdentifiableAttachment;
import magicjinn.artifactspolymer.polymer.PolymerClientChecks;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.EntityType;

public final class MimicPolymerEntity implements PolymerEntity {
    public MimicPolymerEntity(MimicEntity mimic) {
        if (UniqueIdentifiableAttachment.get(mimic, MimicPolymerModelInstances.ATTACHMENT_ID) == null) {
            IdentifiedUniqueEntityAttachment.ofTicking(
                    MimicPolymerModelInstances.ATTACHMENT_ID,
                    new MimicSimpleEntityModel(mimic),
                    mimic);
        }
    }

    @Override
    public EntityType<?> getPolymerEntityType(PacketContext context) {
        return EntityType.ITEM_DISPLAY;
    }

    /**
     * Hides virtual mesh from Artifacts clients that already render the real Mimic.
     */
    private static final class MimicSimpleEntityModel extends SimpleEntityModel<MimicEntity> {
        private MimicSimpleEntityModel(MimicEntity entity) {
            super(entity, MimicPolymerModelInstances.MIMIC_POLY_MODEL);
        }

        @Override
        public boolean startWatching(ServerGamePacketListenerImpl player) {
            if (!PolymerClientChecks.lacksArtifactsClient(player.player))
                return false;

            return super.startWatching(player);
        }
    }
}
