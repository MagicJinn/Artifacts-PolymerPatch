package magicjinn.artifactspatch.poly;

import artifacts.Artifacts;
import artifacts.effect.MagnetismMobEffect;
import artifacts.registry.ModItems;
import artifacts.registry.ModMobEffects;
import artifacts.util.ItemDamageUtil;
import eu.pb4.trinkets.api.TrinketsApi;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

// POLYMER WORKAROUND: When magnetism is missing or toggle-gated, still run Artifacts pull + pickup on the server each tick,
// then push entity and inventory packets to the viewer (vanilla clients otherwise keep ghost ground items until relog).
public final class UniversalAttractorPolymerSupport {
	private static final double PICKUP_REACH = 1.35;

	private UniversalAttractorPolymerSupport() {
	}

	public static void tick(ServerPlayer player) {
		if (!PolymerClientChecks.lacksArtifactsClient(player)) {
			return;
		}

		int amplifier = magnetAmplifierIfEquipped(player);
		if (amplifier < 0) {
			return;
		}

		ServerLevel level = (ServerLevel) player.level();
		MagnetismMobEffect magnetism = (MagnetismMobEffect) ModMobEffects.MAGNETISM.value();
		magnetism.applyEffectTick(level, player, amplifier);
		MagnetismPolymerSupport.syncPulledItems(level, player, amplifier);
		forcePickupNearby(level, player, amplifier);
	}

	private static int magnetAmplifierIfEquipped(ServerPlayer player) {
		int[] amplifier = {-1};
		if (FabricLoader.getInstance().isModLoaded("trinkets_updated")) {
			TrinketsApi.getAttachment(player).forEach((slot, stack) -> {
				amplifier[0] = Math.max(amplifier[0], amplifierFromStack(stack));
			});
		}
		return amplifier[0];
	}

	private static int amplifierFromStack(ItemStack stack) {
		if (stack.isEmpty() || !stack.is(ModItems.UNIVERSAL_ATTRACTOR) || ItemDamageUtil.needsRepair(stack)) {
			return -1;
		}
		int level = Artifacts.CONFIG.items.universalAttractor.magnetismLevel.get();
		return Math.max(0, level - 1);
	}

	private static void forcePickupNearby(ServerLevel level, ServerPlayer player, int amplifier) {
		Vec3 center = player.position().add(0.0, 0.75, 0.0);
		int radius = Math.min(amplifier + 1, 10);
		AABB box = new AABB(
				center.x - radius,
				center.y - radius,
				center.z - radius,
				center.x + radius,
				center.y + radius,
				center.z + radius
		);
		double reachSq = PICKUP_REACH * PICKUP_REACH;
		for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, box)) {
			if (!item.isAlive() || item.hasPickUpDelay()) {
				continue;
			}
			if (item.distanceToSqr(player) <= reachSq) {
				item.playerTouch(player);
			}
		}
	}
}
