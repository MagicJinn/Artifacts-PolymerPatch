package magicjinn.artifactspolymer.resourcepack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.imageio.ImageIO;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.polymer.resourcepack.api.ResourcePackBuilder;

import magicjinn.artifactspolymer.ArtifactsPolymerPatch;
import magicjinn.artifactspolymer.polymer.mimic.MimicPolymerModelInstances;
import magicjinn.artifactspolymer.polymer.mimic.MimicModelBytecodeExtractor;
import magicjinn.artifactspolymer.polymer.mimic.MimicModelBytecodeExtractor.ExtractedLayer;
import eu.pb4.polymer.resourcepack.extras.api.format.atlas.AtlasAsset;
import eu.pb4.polymer.resourcepack.extras.api.format.atlas.AtlasAsset.Builder;

/**
 * MimicPolymerResourcePack
 */
public final class MimicPolymerResourcePack {
    private static final String ITEMS_ATLAS = "assets/minecraft/atlases/items.json";

    // Combined texture for for the mimic polymer model
    private static final String ATLAS_OUT = "assets/artifacts-polymer-patch/textures/entity/mimic_polymer_atlas.png";
    // Minecrafts chest texture to use
    private static final String CHEST_MINECRAFT = "assets/minecraft/textures/entity/chest/normal.png";
    // private static final String CHEST_FALLBACK =
    // "assets/artifacts-polymer-patch/textures/entity/mimic_polymer_chest_normal.png";

    public static void register() {
        PolymerResourcePackUtils.RESOURCE_PACK_AFTER_INITIAL_CREATION_EVENT.register(MimicPolymerResourcePack::build);
    }

    private static void build(ResourcePackBuilder builder) {
        // Produce or ensure the combined texture is created
        ensureMimicPolymerAtlas(builder);

        // Rebuild items.json
        Builder atlas = AtlasAsset.builder();
        mergeExistingItemsAtlas(builder, atlas);
        MimicPolymerModelInstances.MIMIC.generateAssets(builder::addData, atlas);
        builder.addData(ITEMS_ATLAS, atlas.build());
    }

    private static void ensureMimicPolymerAtlas(ResourcePackBuilder builder) {
        if (builder.getDataOrSource(ATLAS_OUT) != null)
            return;

        ExtractedLayer body = MimicModelBytecodeExtractor.extractCreateLayer();
        ExtractedLayer chest = MimicModelBytecodeExtractor.extractCreateChestLayer();

        int mimicW = body.texWidth();
        int mimicH = body.texHeight();
        int chestW = chest.texWidth();
        int chestH = chest.texHeight();
        int atlasH = mimicH + chestH;

        // Get the mimic texture path
        String mimicPath = MimicModelBytecodeExtractor.extractMimicTextureAssetPath();

        // Prefer assets already in the Polymer builder, fall back to classpath jars.
        byte[] mimicBytes = firstNonNull(
                builder.getDataOrSource(mimicPath),
                readClasspath(mimicPath));
        byte[] chestBytes = firstNonNull(
                builder.getDataOrSource(CHEST_MINECRAFT),
                readClasspath(CHEST_MINECRAFT));

        if (mimicBytes == null) {
            ArtifactsPolymerPatch.LOGGER.warn(
                    "{} not in pack/classpath. Skipping mimic_polymer_atlas", mimicPath);
            return;
        }
        if (chestBytes == null) {
            ArtifactsPolymerPatch.LOGGER.warn(
                    "chest texture not in pack. Skipping mimic_polymer_atlas (add {} for wood)", CHEST_MINECRAFT);
            return;
        }

        try {
            // Read the mimic and chest textures from the byte arrays.
            BufferedImage mimicImg = ImageIO.read(new ByteArrayInputStream(mimicBytes));
            BufferedImage chestImg = ImageIO.read(new ByteArrayInputStream(chestBytes));
            if (mimicImg == null || chestImg == null) {
                ArtifactsPolymerPatch.LOGGER.warn("Failed to decode mimic/chest PNG. Skipping mimic_polymer_atlas");
                return;
            }

            // Normalize to extracted tex sizes (nearest-neighbor because pixels).
            mimicImg = scaleToNN(mimicImg, mimicW, mimicH);
            chestImg = scaleToNN(chestImg, chestW, chestH);

            BufferedImage combined = new BufferedImage(mimicW, atlasH, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphic = combined.createGraphics();
            graphic.drawImage(mimicImg, 0, 0, null);
            graphic.drawImage(chestImg, 0, mimicH, null);
            graphic.dispose();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(combined, "png", out);
            builder.addData(ATLAS_OUT, out.toByteArray());
        } catch (IOException e) {
            ArtifactsPolymerPatch.LOGGER.error("Failed to write mimic_polymer_atlas", e);
            return;
        }
    }

    @SafeVarargs
    private static <T> T firstNonNull(T... values) {
        for (T v : values) {
            if (v != null)
                return v;
        }
        return null;
    }

    /**
     * Read a resource path from any jar on the classpath (Artifacts / Minecraft /
     * our mod).
     */
    private static byte[] readClasspath(String path) {
        try (InputStream in = MimicPolymerResourcePack.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                return null;
            }
            return in.readAllBytes();
        } catch (IOException e) {
            return null;
        }
    }

    /** Scale with nearest-neighbor. No-op if already the right size. */
    private static BufferedImage scaleToNN(BufferedImage src, int w, int h) {
        if (src.getWidth() == w && src.getHeight() == h) {
            return src;
        }
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return out;
    }

    /**
     * Copy sources from an existing items.json already in the builder so we don't
     * wipe other mods' / Polymer's atlas entries when we rewrite the file.
     * Not sure if this is necessary.
     */
    private static void mergeExistingItemsAtlas(ResourcePackBuilder builder, AtlasAsset.Builder atlas) {
        byte[] data = builder.getDataOrSource(ITEMS_ATLAS);
        if (data == null)
            return;

        try {
            AtlasAsset existing = AtlasAsset.fromJson(new String(data, StandardCharsets.UTF_8));
            for (var source : existing.sources()) {
                atlas.add(source);
            }
        } catch (Throwable ignored) {
            // Corrupt / unexpected atlas JSON. Start fresh with only our sources.
        }
    }
}
