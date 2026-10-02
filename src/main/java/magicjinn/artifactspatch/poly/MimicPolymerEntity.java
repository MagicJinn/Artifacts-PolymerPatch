package magicjinn.artifactspatch.poly;

import artifacts.entity.MimicEntity;
import magicjinn.artifactspatch.ArtifactsPolymerPatch;
import eu.pb4.polymer.core.api.entity.PolymerEntity;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.IdentifiedUniqueEntityAttachment;
import eu.pb4.polymer.virtualentity.api.elements.BlockDisplayElement;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

// POLYMER WORKAROUND: Visual stand-in for MimicEntity on clients without Artifacts. Wire type is MARKER
// (invisible); chest is a virtual BlockDisplay. Tracked data is cleared so polymer clients do not decode mimic fields.
public final class MimicPolymerEntity implements PolymerEntity {
	private static final float MIMIC_SIZE = 14 / 16F;

	private final MimicEntity mimic;

	public MimicPolymerEntity(MimicEntity mimic) {
		this.mimic = mimic;
		this.attachChestDisplay();
	}

	private void attachChestDisplay() {
		if (mimic.level().isClientSide()) {
			return;
		}

		var holder = new ElementHolder();
		var chest = new BlockDisplayElement(Blocks.CHEST.defaultBlockState());
		chest.setOffset(new Vec3(-0.5, 0, -0.5));
		chest.setScale(new Vector3f(MIMIC_SIZE, MIMIC_SIZE, MIMIC_SIZE));
		holder.addElement(chest);

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
