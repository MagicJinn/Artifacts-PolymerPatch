package magicjinn.artifactspatch.poly;

import artifacts.Artifacts;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Vanilla clients gate mining on local {@link Attributes#BLOCK_BREAK_SPEED}; Artifacts applies bonuses server-side only.
 * Scale server destroy progress so break time matches the server's attribute value.
 */
public final class DiggingClawsPolymerSupport {
	private DiggingClawsPolymerSupport() {
	}

	public static float destroyProgressMultiplier(ServerPlayer player, BlockState state) {
		if (!PolymerClientChecks.lacksArtifactsClient(player)) {
			return 1.0F;
		}

		float serverSpeed = player.getDestroySpeed(state);
		float clientSpeed = estimateClientDestroySpeed(player, state);
		if (clientSpeed <= 1.0E-4F || serverSpeed <= clientSpeed) {
			return 1.0F;
		}
		return Mth.clamp(serverSpeed / clientSpeed, 1.0F, 64.0F);
	}

	private static float estimateClientDestroySpeed(ServerPlayer player, BlockState state) {
		float serverSpeed = player.getDestroySpeed(state);
		double serverAttr = player.getAttributeValue(Attributes.BLOCK_BREAK_SPEED);
		double clientAttr = attributeValueExcludingNamespace(player, Attributes.BLOCK_BREAK_SPEED, Artifacts.MOD_ID);
		if (serverAttr <= 1.0E-6 || Math.abs(serverAttr - clientAttr) < 1.0E-6) {
			return serverSpeed;
		}
		return serverSpeed * (float) (clientAttr / serverAttr);
	}

	private static double attributeValueExcludingNamespace(
			ServerPlayer player,
			Holder<Attribute> attribute,
			String excludedNamespace
	) {
		AttributeInstance instance = player.getAttribute(attribute);
		if (instance == null) {
			return player.getAttributeBaseValue(attribute);
		}

		double value = instance.getBaseValue();

		for (AttributeModifier modifier : instance.getModifiers()) {
			if (excludedNamespace.equals(modifier.id().getNamespace())) {
				continue;
			}
			if (modifier.operation() == AttributeModifier.Operation.ADD_VALUE) {
				value += modifier.amount();
			}
		}

		double multipliedBase = value;
		for (AttributeModifier modifier : instance.getModifiers()) {
			if (excludedNamespace.equals(modifier.id().getNamespace())) {
				continue;
			}
			if (modifier.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_BASE) {
				multipliedBase += value * modifier.amount();
			}
		}
		value = multipliedBase;

		for (AttributeModifier modifier : instance.getModifiers()) {
			if (excludedNamespace.equals(modifier.id().getNamespace())) {
				continue;
			}
			if (modifier.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) {
				value *= 1.0 + modifier.amount();
			}
		}
		return value;
	}
}
