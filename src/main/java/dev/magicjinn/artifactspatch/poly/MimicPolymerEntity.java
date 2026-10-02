package dev.magicjinn.artifactspatch.poly;

import artifacts.entity.MimicEntity;
import dev.magicjinn.artifactspatch.ArtifactsPolymerPatch;
import eu.pb4.polymer.core.api.entity.PolymerEntity;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.IdentifiedUniqueEntityAttachment;
import eu.pb4.polymer.virtualentity.api.elements.BlockDisplayElement;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

/**
 * Visual stand-in for {@link MimicEntity} on clients without Artifacts installed.
 * Uses a block display (chest) attached to the real mimic entity; the polymer base type is a marker armor stand.
 */
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
		// Align block display with mimic hitbox (static pose; no open/attack animation yet).
		chest.setOffset(new Vec3(-0.5, 0, -0.5));
		chest.setScale(new Vector3f(MIMIC_SIZE, MIMIC_SIZE, MIMIC_SIZE));
		holder.addElement(chest);

		IdentifiedUniqueEntityAttachment.ofTicking(ArtifactsPolymerPatch.id("mimic_chest"), holder, mimic);
	}

	@Override
	public EntityType<?> getPolymerEntityType(PacketContext context) {
		return EntityType.ARMOR_STAND;
	}

	@Override
	public boolean sendEmptyTrackerUpdates(ServerPlayer player) {
		return true;
	}

	@Override
	public void modifyRawTrackedData(List<SynchedEntityData.DataValue<?>> data, ServerPlayer player, boolean initial) {
		setByte(
				data,
				ArmorStand.DATA_CLIENT_FLAGS,
				(byte) (ArmorStand.CLIENT_FLAG_MARKER | ArmorStand.CLIENT_FLAG_NO_BASEPLATE | ArmorStand.CLIENT_FLAG_SMALL)
		);
	}

	private static void setByte(List<SynchedEntityData.DataValue<?>> data, EntityDataAccessor<Byte> accessor, byte value) {
		for (int i = 0; i < data.size(); i++) {
			if (data.get(i).id() == accessor.id()) {
				data.set(i, SynchedEntityData.DataValue.create(accessor, value));
				return;
			}
		}
		data.add(SynchedEntityData.DataValue.create(accessor, value));
	}
}
