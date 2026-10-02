package dev.magicjinn.artifactspatch.poly;

import artifacts.entity.MimicEntity;
import eu.pb4.polymer.core.api.entity.PolymerEntity;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.world.entity.EntityType;

/**
 * Visual stand-in for {@link MimicEntity} on clients without Artifacts installed.
 */
public record MimicPolymerEntity(MimicEntity mimic) implements PolymerEntity {
	@Override
	public EntityType<?> getPolymerEntityType(PacketContext context) {
		// Small hostile mob placeholder; model comes from the Artifacts resource pack where supported.
		return EntityType.SILVERFISH;
	}
}
