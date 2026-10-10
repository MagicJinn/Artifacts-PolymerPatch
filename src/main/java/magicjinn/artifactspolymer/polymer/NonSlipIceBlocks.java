package magicjinn.artifactspolymer.polymer;

import eu.pb4.polymer.blocks.api.BlockModelType;
import eu.pb4.polymer.blocks.api.PolymerBlockModel;
import eu.pb4.polymer.blocks.api.PolymerBlockResourceUtils;
import eu.pb4.polymer.blocks.api.PolymerTexturedBlock;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import magicjinn.artifactspolymer.ArtifactsPolymerPatch;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * POLYMER WORKAROUND: Ice-lookalike blocks with default (non-ice) friction for
 * Steadfast Spikes on vanilla clients.
 */
public final class NonSlipIceBlocks {
    private static final BlockState FALLBACK = Blocks.LIGHT_BLUE_WOOL.defaultBlockState();

    // POLYMER WORKAROUND: Regular ice is translucent. FULL_BLOCK (note block)
    // stand-ins are opaque, so neighbors like grass cull faces against them and
    // become see-through. LEAVES stand-ins are non-opaque so adjacent faces draw.
    public static final Block ICE = register("non_slip_ice", "block/ice", BlockModelType.LEAVES);
    // Packed / blue ice are opaque in vanilla. Note-block stand-ins are fine.
    public static final Block PACKED_ICE = register("non_slip_packed_ice", "block/packed_ice",
            BlockModelType.FULL_BLOCK);
    public static final Block BLUE_ICE = register("non_slip_blue_ice", "block/blue_ice",
            BlockModelType.FULL_BLOCK);

    private NonSlipIceBlocks() {
    }

    public static void register() {
        // Touch static fields so Blocks.register runs during mod init.
    }

    public static BlockState clientFootingFor(ServerPlayer player, BlockState real) {
        // POLYMER WORKAROUND: Without the main pack, textured stand-ins look like
        // note blocks. Fall back to non-slippery wool.
        if (!PolymerResourcePackUtils.hasMainPack(player))
            return FALLBACK;

        if (real.is(Blocks.BLUE_ICE))
            return BLUE_ICE.defaultBlockState();
        if (real.is(Blocks.PACKED_ICE))
            return PACKED_ICE.defaultBlockState();
        return ICE.defaultBlockState();
    }

    private static Block register(String path, String vanillaModelPath, BlockModelType modelType) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, ArtifactsPolymerPatch.id(path));
        Identifier model = Identifier.withDefaultNamespace(vanillaModelPath);
        return Blocks.register(key, props -> new NonSlipIceBlock(props, model, modelType),
                BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)
                        .friction(0.6f)
                        .sound(SoundType.GLASS)
                        .strength(0.5f));
    }

    private static final class NonSlipIceBlock extends Block implements PolymerTexturedBlock {
        private final BlockState polymerState;

        private NonSlipIceBlock(Properties properties, Identifier model, BlockModelType modelType) {
            super(properties);
            BlockState requested = PolymerBlockResourceUtils.requestBlock(
                    modelType,
                    PolymerBlockModel.of(model));
            this.polymerState = requested != null ? requested : FALLBACK;
        }

        @Override
        public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
            return polymerState;
        }
    }
}
