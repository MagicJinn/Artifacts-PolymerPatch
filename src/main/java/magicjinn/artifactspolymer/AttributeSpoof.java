package magicjinn.artifactspolymer;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/**
 * Builds client-facing attribute snapshots that report a target total without
 * mutating the server instance.
 */
public final class AttributeSpoof {
    public static AttributeInstance copyWithTotal(AttributeInstance real, double targetTotal, Identifier deltaId) {
        AttributeInstance fake = new AttributeInstance(real.getAttribute(), _ -> {
        });
        fake.setBaseValue(real.getBaseValue());
        for (AttributeModifier modifier : real.getModifiers()) {
            fake.addTransientModifier(modifier);
        }
        double delta = targetTotal - fake.getValue();
        if (Math.abs(delta) > 1e-6)
            fake.addTransientModifier(new AttributeModifier(deltaId, delta, AttributeModifier.Operation.ADD_VALUE));

        return fake;
    }
}
