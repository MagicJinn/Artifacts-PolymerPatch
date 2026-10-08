package magicjinn.artifactspolymer.polymer.mimic;

import java.util.Set;

import artifacts.entity.MimicEntity;
import eu.pb4.factorytools.api.virtualentity.emuvanilla.model.CubeDeformation;
import eu.pb4.factorytools.api.virtualentity.emuvanilla.model.CubeListBuilder;
import eu.pb4.factorytools.api.virtualentity.emuvanilla.model.EntityModel;
import eu.pb4.factorytools.api.virtualentity.emuvanilla.model.LayerDefinition;
import eu.pb4.factorytools.api.virtualentity.emuvanilla.model.MeshDefinition;
import eu.pb4.factorytools.api.virtualentity.emuvanilla.model.ModelPart;
import eu.pb4.factorytools.api.virtualentity.emuvanilla.model.PartPose;
import magicjinn.artifactspolymer.polymer.mimic.MimicModelBytecodeExtractor.ExtractedChestAnim;
import magicjinn.artifactspolymer.polymer.mimic.MimicModelBytecodeExtractor.ExtractedCube;
import magicjinn.artifactspolymer.polymer.mimic.MimicModelBytecodeExtractor.ExtractedLayer;
import magicjinn.artifactspolymer.polymer.mimic.MimicModelBytecodeExtractor.ExtractedPart;
import net.minecraft.core.Direction;

/**
 * FactoryTools {@link EntityModel} for Mimic.
 */
public final class MimicPolymerEntityModel extends EntityModel<MimicEntity> {

    private static final float CHEST_PIVOT_X = -8f;
    private static final float CHEST_PIVOT_Y = 15f;
    private static final float CHEST_PIVOT_Z = 7.9f;

    private static final float CHEST_ROT_PITCH_OFFSET = (float) Math.PI;

    private static final ExtractedChestAnim ANIM = MimicModelBytecodeExtractor.extractChestRotations();
    private static final int TICKS_IN_AIR_BIAS = MimicModelBytecodeExtractor.extractTicksInAirBias();

    private final ModelPart bottom;
    private final ModelPart lid;
    private final ModelPart chestBottom;
    private final ModelPart chestLid;

    public MimicPolymerEntityModel(ModelPart root) {
        super(root);
        this.bottom = root.getChild("bottom");
        this.lid = root.getChild("lid");
        this.chestBottom = root.getChild("chest_bottom");
        this.chestLid = root.getChild("chest_lid");
    }

    @Override
    public void setupAnim(MimicEntity entity) {
        super.setupAnim(entity);

        float spoofTicksInAir = Math.max(0, entity.ticksInAir - TICKS_IN_AIR_BIAS);
        float lidPitch = 0;
        float bottomPitch = 0;
        if (spoofTicksInAir > 0) {
            lidPitch = Math.max(ANIM.lidClamp(), spoofTicksInAir * ANIM.lidRate()) * ANIM.degToRad();
            bottomPitch = Math.min(ANIM.bottomClamp(), spoofTicksInAir * ANIM.bottomRate()) * ANIM.degToRad();
        }

        lid.xRot = lidPitch;
        bottom.xRot = bottomPitch;
        chestLid.xRot = lidPitch + CHEST_ROT_PITCH_OFFSET;
        chestBottom.xRot = bottomPitch + CHEST_ROT_PITCH_OFFSET;
    }

    public static LayerDefinition createLayerDefinition() {
        ExtractedLayer body = MimicModelBytecodeExtractor.extractCreateLayer();
        ExtractedLayer chest = MimicModelBytecodeExtractor.extractCreateChestLayer();
        int chestVOffset = body.texHeight();

        MeshDefinition data = new MeshDefinition();
        for (ExtractedPart part : body.parts()) {
            data.getRoot().addOrReplaceChild(
                    part.name(),
                    toCubeList(part, 0),
                    PartPose.offset(part.pivotX(), part.pivotY(), part.pivotZ()));
        }
        for (ExtractedPart part : chest.parts()) {
            data.getRoot().addOrReplaceChild(
                    switch (part.name()) { // Choose the correct chest part name
                        case "bottom" -> "chest_bottom";
                        case "lid" -> "chest_lid";
                        default -> "chest_" + part.name();
                    },
                    toCubeList(part, chestVOffset),
                    PartPose.offset(CHEST_PIVOT_X, CHEST_PIVOT_Y, CHEST_PIVOT_Z));
        }

        return LayerDefinition.create(data, body.texWidth(), body.texHeight() + chest.texHeight());
    }

    private static CubeListBuilder toCubeList(ExtractedPart part, int vOffset) {
        CubeListBuilder builder = CubeListBuilder.create();
        for (ExtractedCube cube : part.cubes()) {
            builder.texOffs(cube.u(), cube.v() + vOffset);
            if (cube.sizeY() == 0f) {
                // Zero-height mouth planes: single face keeps UVs inside the PolyModel baker.
                builder.addBox(
                        cube.x(), cube.y(), cube.z(),
                        cube.sizeX(), cube.sizeY(), cube.sizeZ(),
                        Set.of(Direction.DOWN));
            } else if (cube.deformation() != 0f) {
                builder.addBox(
                        cube.x(), cube.y(), cube.z(),
                        cube.sizeX(), cube.sizeY(), cube.sizeZ(),
                        new CubeDeformation(cube.deformation()));
            } else {
                builder.addBox(
                        cube.x(), cube.y(), cube.z(),
                        cube.sizeX(), cube.sizeY(), cube.sizeZ());
            }
        }
        return builder;
    }
}
