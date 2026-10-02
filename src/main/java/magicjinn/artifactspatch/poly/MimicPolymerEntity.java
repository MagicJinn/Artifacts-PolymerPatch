package magicjinn.artifactspatch.poly;

import artifacts.entity.MimicEntity;
import magicjinn.artifactspatch.ArtifactsPolymerPatch;
import eu.pb4.polymer.core.api.entity.PolymerEntity;
import eu.pb4.polymer.virtualentity.api.attachment.IdentifiedUniqueEntityAttachment;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;

// POLYMER WORKAROUND: Visual stand-in for MimicEntity on clients without Artifacts. Wire type is MARKER
// (invisible); hinged lid + bottom BlockDisplays follow MimicModel mouth angles from ticksInAir.
// Tracked data is cleared so polymer clients do not decode mimic fields.
public final class MimicPolymerEntity implements PolymerEntity {
	private final MimicEntity mimic;

	public MimicPolymerEntity(MimicEntity mimic) {
		this.mimic = mimic;
		this.attachChestDisplays();
	}

	private void attachChestDisplays() {
		if (mimic.level().isClientSide()) {
			return;
		}

		var holder = new MimicPolymerChestHolder(mimic);
		IdentifiedUniqueEntityAttachment.ofTicking(ArtifactsPolymerPatch.id("mimic_chest"), holder, mimic);
	}

	@Override
	public EntityType<?> getPolymerEntityType(PacketContext context) {
		return EntityType.MARKER;
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
		// POLYMER WORKAROUND: Mimic is a living entity server-side; polymer clients see a marker with no custom fields.
		data.clear();
	}
}
