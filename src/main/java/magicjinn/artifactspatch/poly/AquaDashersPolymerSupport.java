package magicjinn.artifactspatch.poly;

import artifacts.component.SwimData;
import artifacts.platform.PlatformServices;
import artifacts.registry.ModDataComponents;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.ManualAttachment;
import eu.pb4.polymer.virtualentity.api.elements.InteractionElement;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// POLYMER WORKAROUND: Packet-only support deck for aqua-dashers water sprint (Polymer clients only). Real boats
// caused entity-data disconnects; this owner-only InteractionElement deck is synced via startWatching only.
public final class AquaDashersPolymerSupport {
	private static final double DECK_ABOVE_SURFACE = 1.0 / 16.0;
	private static final float DECK_WIDTH = 1.375F;
	private static final float DECK_HEIGHT = 0.5625F;
	private static final int DISCARD_AFTER_TICKS_WITHOUT_SPRINT = 15;

	private static final Map<UUID, SupportState> SUPPORT = new ConcurrentHashMap<>();
	private static final Map<UUID, Integer> TICKS_WITHOUT_SPRINT = new ConcurrentHashMap<>();

	private AquaDashersPolymerSupport() {
	}

	public static void tick(ServerPlayer player) {
		if (!PolymerClientChecks.lacksArtifactsClient(player)) {
			clearSupport(player.getUUID());
			return;
		}

		if (!isWaterSprinting(player)) {
			int idleTicks = TICKS_WITHOUT_SPRINT.merge(player.getUUID(), 1, Integer::sum);
			if (idleTicks >= DISCARD_AFTER_TICKS_WITHOUT_SPRINT) {
				clearSupport(player.getUUID());
			} else {
				syncSupport(player);
			}
			return;
		}

		TICKS_WITHOUT_SPRINT.put(player.getUUID(), 0);
		ensureSupport(player);
		syncSupport(player);
	}

	public static void onDisconnect(ServerPlayer player) {
		clearSupport(player.getUUID());
	}

	public static void clearOrphanedBoats(ServerLevel level) {
		Iterator<Map.Entry<UUID, SupportState>> iterator = SUPPORT.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, SupportState> entry = iterator.next();
			if (entry.getValue().isStale(level)) {
				entry.getValue().destroy();
				iterator.remove();
				TICKS_WITHOUT_SPRINT.remove(entry.getKey());
			}
		}
	}

	public static boolean isWaterSprinting(Player player) {
		if (!(player instanceof LivingEntity living)) {
			return false;
		}
		if (!player.isSprinting()) {
			return false;
		}
		SwimData swimData = PlatformServices.getPlatformHelper().getSwimData(living);
		if (swimData == null || swimData.shouldBreakSurfaceTension()) {
			return false;
		}
		if (!ModDataComponents.FLUID_COLLISION.on(living).findAny()) {
			return false;
		}
		FluidState fluid = fluidAtFeet(player);
		return fluid != null && fluid.is(FluidTags.WATER);
	}

	private static void ensureSupport(ServerPlayer player) {
		SUPPORT.computeIfAbsent(player.getUUID(), id -> new SupportState((ServerLevel) player.level(), player));
	}

	private static void syncSupport(ServerPlayer player) {
		SupportState state = SUPPORT.get(player.getUUID());
		if (state == null) {
			return;
		}
		state.sync(player);
	}

	private static void clearSupport(UUID playerId) {
		TICKS_WITHOUT_SPRINT.remove(playerId);
		SupportState state = SUPPORT.remove(playerId);
		if (state != null) {
			state.destroy();
		}
	}

	private static Vec3 deckPosition(ServerPlayer player) {
		WaterSurface surface = waterSurfaceAt(player);
		if (surface == null) {
			return player.position();
		}
		double y = surface.y + DECK_ABOVE_SURFACE - DECK_HEIGHT;
		return new Vec3(player.getX(), y, player.getZ());
	}

	private static WaterSurface waterSurfaceAt(Player player) {
		FluidState fluid = fluidAtFeet(player);
		if (fluid == null || fluid.isEmpty()) {
			return null;
		}
		BlockPos fluidPos = BlockPos.containing(player.getX(), player.getY() - 0.2, player.getZ());
		if (player.level().getFluidState(fluidPos).isEmpty()) {
			fluidPos = player.blockPosition();
			fluid = player.level().getFluidState(fluidPos);
			if (fluid.isEmpty()) {
				return null;
			}
		}
		double y = fluidPos.getY() + fluid.getHeight(player.level(), fluidPos);
		return new WaterSurface(y);
	}

	private static FluidState fluidAtFeet(Player player) {
		BlockPos pos = BlockPos.containing(player.getX(), player.getY() - 0.2, player.getZ());
		FluidState fluid = player.level().getFluidState(pos);
		if (!fluid.isEmpty()) {
			return fluid;
		}
		pos = player.blockPosition();
		return player.level().getFluidState(pos);
	}

	private static final class SupportState {
		private final ElementHolder holder;
		private final ManualAttachment attachment;
		private final ServerLevel level;
		private final ServerPlayer owner;
		private final UUID ownerId;
		private boolean watching;

		private SupportState(ServerLevel level, ServerPlayer owner) {
			this.level = level;
			this.owner = owner;
			this.ownerId = owner.getUUID();
			this.holder = new ElementHolder();
			var deck = new InteractionElement();
			deck.setSize(DECK_WIDTH, DECK_HEIGHT);
			deck.setResponse(true);
			deck.setOffset(new Vec3(0, DECK_HEIGHT * 0.5, 0));
			this.holder.addElement(deck);
			this.attachment = new ManualAttachment(this.holder, level, () -> deckPosition(this.owner));
		}

		private void sync(ServerPlayer player) {
			if (!player.getUUID().equals(ownerId)) {
				return;
			}
			if (!watching) {
				holder.startWatching(player);
				watching = true;
			}
			holder.tick();
		}

		private boolean isStale(ServerLevel currentLevel) {
			return currentLevel != level;
		}

		private void destroy() {
			if (watching && owner.isAlive()) {
				holder.stopWatching(owner);
				watching = false;
			}
			attachment.destroy();
			holder.destroy();
		}
	}

	private record WaterSurface(double y) {
	}
}
