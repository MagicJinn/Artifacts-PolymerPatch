package magicjinn.artifactspatch.poly;

import artifacts.entity.MimicEntity;
import eu.pb4.polymer.core.api.entity.PolymerEntity;
import eu.pb4.polymer.virtualentity.api.attachment.IdentifiedUniqueEntityAttachment;
import eu.pb4.polymer.virtualentity.api.data.InteractionEntityData;
import magicjinn.artifactspatch.ArtifactsPolymerPatch;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;

// POLYMER WORKAROUND: Visual stand-in for MimicEntity on clients without Artifacts. Wire type is INTERACTION
// (invisible, sized hitbox); a static vanilla chest BlockDisplay is the placeholder visual.
// Tracked data is cleared so polymer clients do not decode mimic fields.
public final class MimicPolymerEntity implements PolymerEntity {
	/** Matches ModEntityTypes.MIMIC EntityType.Builder.sized(0.875f, 0.875f). */
	public static final float MIMIC_HITBOX_SIZE = 0.875f;

	private final MimicEntity mimic;

	public MimicPolymerEntity(MimicEntity mimic) {
		this.mimic = mimic;
		this.attachPlaceholderVisual();
	}

	private void attachPlaceholderVisual() {
		if (mimic.level().isClientSide()) {
			return;
		}

		var holder = new MimicPolymerChestPlaceholder(mimic);
		IdentifiedUniqueEntityAttachment.of(ArtifactsPolymerPatch.id("mimic_chest"), holder, mimic);
	}

	@Override
	public EntityType<?> getPolymerEntityType(PacketContext context) {
		// POLYMER WORKAROUND: MARKER has no client pick box; INTERACTION is invisible but targetable at set width/height.
		return EntityType.INTERACTION;
	}

	@Override
	public boolean sendEmptyTrackerUpdates(ServerPlayer player) {
		return true;
	}

	@Override
	public void modifyRawTrackedData(
			java.util.List<SynchedEntityData.DataValue<?>> data,
			ServerPlayer player,
			boolean initial
	) {
		if (!PolymerClientChecks.lacksArtifactsClient(player)) {
			return;
		}
		// POLYMER WORKAROUND: Mimic is a living entity server-side; polymer clients see interaction with no custom fields.
		data.clear();
		data.add(SynchedEntityData.DataValue.create(InteractionEntityData.WIDTH, MIMIC_HITBOX_SIZE));
		data.add(SynchedEntityData.DataValue.create(InteractionEntityData.HEIGHT, MIMIC_HITBOX_SIZE));
		data.add(SynchedEntityData.DataValue.create(InteractionEntityData.RESPONSE, true));
	}
}
