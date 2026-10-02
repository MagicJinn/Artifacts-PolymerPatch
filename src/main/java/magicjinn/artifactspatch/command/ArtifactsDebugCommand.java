package magicjinn.artifactspatch.command;

import artifacts.Artifacts;
import com.mojang.brigadier.CommandDispatcher;
import magicjinn.artifactspatch.ArtifactsPolymerPatch;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ArtifactsDebugCommand {
	private ArtifactsDebugCommand() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register(ArtifactsDebugCommand::registerCommands);
	}

	private static void registerCommands(
			CommandDispatcher<CommandSourceStack> dispatcher,
			net.minecraft.commands.CommandBuildContext registryAccess,
			Commands.CommandSelection environment
	) {
		var root = Commands.literal("artifactspatch")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.literal("all").executes(ctx -> placeAllArtifactsChest(ctx.getSource())));

		var alias = Commands.literal("artifacts-polymer")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.literal("all").executes(ctx -> placeAllArtifactsChest(ctx.getSource())));

		dispatcher.register(root);
		dispatcher.register(alias);
	}

	private static int placeAllArtifactsChest(CommandSourceStack source) {
		if (!(source.getEntity() instanceof ServerPlayer player)) {
			source.sendFailure(Component.literal("This command must be run by a player."));
			return 0;
		}

		ServerLevel level = (ServerLevel) player.level();
		List<ItemStack> stacks = collectArtifactsItems();
		if (stacks.isEmpty()) {
			source.sendFailure(Component.literal("No items found in the artifacts registry."));
			return 0;
		}

		Direction facing = player.getDirection().getOpposite();
		BlockPos origin = BlockPos.containing(player.getX(), player.getY(), player.getZ());
		BlockPos primary = findDoubleChestOrigin(level, origin, facing);
		if (primary == null) {
			source.sendFailure(Component.literal("No space for a double chest near you."));
			return 0;
		}

		BlockPos secondary = primary.relative(facing.getClockWise());
		placeDoubleChest(level, primary, secondary, facing);

		BlockState state = level.getBlockState(primary);
		Container container = ChestBlock.getContainer((ChestBlock) Blocks.CHEST, state, level, primary, true);
		if (container == null) {
			source.sendFailure(Component.literal("Failed to open chest inventory after placement."));
			return 0;
		}

		final int itemCount = Math.min(stacks.size(), container.getContainerSize());
		for (int slot = 0; slot < itemCount; slot++) {
			container.setItem(slot, stacks.get(slot));
		}

		int overflow = stacks.size() - itemCount;
		if (overflow > 0) {
			source.sendFailure(Component.literal(
					"Double chest holds " + itemCount + " items; " + overflow + " artifacts items did not fit."
			));
		}

		source.sendSuccess(
				() -> Component.literal("Placed a double chest at "
						+ primary.toShortString()
						+ " with "
						+ itemCount
						+ " artifacts item(s)."),
				true
		);
		ArtifactsPolymerPatch.LOGGER.info(
				"{} placed artifacts debug chest at {} ({} items, {} overflow)",
				player.getName().getString(),
				primary,
				itemCount,
				overflow
		);
		return itemCount > 0 ? 1 : 0;
	}

	private static List<ItemStack> collectArtifactsItems() {
		List<ItemStack> stacks = new ArrayList<>();
		for (Item item : BuiltInRegistries.ITEM) {
			var key = BuiltInRegistries.ITEM.getKey(item);
			if (key == null || !Artifacts.MOD_ID.equals(key.getNamespace())) {
				continue;
			}
			stacks.add(new ItemStack(item));
		}
		stacks.sort(Comparator.comparing(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()));
		return stacks;
	}

	private static BlockPos findDoubleChestOrigin(ServerLevel level, BlockPos origin, Direction facing) {
		BlockPos[] candidates = {
				origin,
				origin.below(),
				origin.north(),
				origin.south(),
				origin.east(),
				origin.west(),
				origin.above()
		};

		for (BlockPos candidate : candidates) {
			BlockPos secondary = candidate.relative(facing.getClockWise());
			if (canPlaceChestPart(level, candidate) && canPlaceChestPart(level, secondary)) {
				return candidate;
			}
		}
		return null;
	}

	private static boolean canPlaceChestPart(ServerLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (!state.canBeReplaced()) {
			return false;
		}
		return level.getBlockState(pos.below()).isSolidRender();
	}

	private static void placeDoubleChest(ServerLevel level, BlockPos primary, BlockPos secondary, Direction facing) {
		BlockState left = Blocks.CHEST.defaultBlockState()
				.setValue(ChestBlock.FACING, facing)
				.setValue(ChestBlock.TYPE, ChestType.LEFT);
		BlockState right = Blocks.CHEST.defaultBlockState()
				.setValue(ChestBlock.FACING, facing)
				.setValue(ChestBlock.TYPE, ChestType.RIGHT);

		level.setBlock(primary, left, Block.UPDATE_ALL);
		level.setBlock(secondary, right, Block.UPDATE_ALL);
	}

}
