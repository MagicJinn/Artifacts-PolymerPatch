package magicjinn.artifactspolymer.polymer.mimic;

import artifacts.entity.MimicEntity;
import eu.pb4.factorytools.api.virtualentity.emuvanilla.PolyModelInstance;
import eu.pb4.factorytools.api.virtualentity.emuvanilla.model.EntityModel;
import magicjinn.artifactspolymer.ArtifactsPolymerPatch;
import net.minecraft.resources.Identifier;

/**
 * FactoryTools {@link PolyModelInstance} for Mimic.
 * {@link PolyModelInstance#generateAssets}
 * converts the layer into item-model JSON at Polymer pack build time.
 */
public final class MimicPolymerEntityModels {
	public static final Identifier ATTACHMENT_ID = ArtifactsPolymerPatch.id("mimic_polymer_model");

	public static final Identifier ATLAS_TEXTURE = ArtifactsPolymerPatch.id("entity/mimic_polymer_atlas");

	public static final PolyModelInstance<MimicPolymerEntityModel> MIMIC = PolyModelInstance.create(
			MimicPolymerEntityModel::new,
			MimicPolymerEntityModel.createLayerDefinition(),
			ATLAS_TEXTURE);

	@SuppressWarnings("unchecked")
	public static final PolyModelInstance<EntityModel<MimicEntity>> MIMIC_POLY_MODEL = (PolyModelInstance<EntityModel<MimicEntity>>) (Object) MIMIC;

	private MimicPolymerEntityModels() {
	}
}
