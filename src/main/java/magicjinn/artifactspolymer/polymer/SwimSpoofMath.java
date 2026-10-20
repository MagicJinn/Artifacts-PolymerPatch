package magicjinn.artifactspolymer.polymer;

/**
 * Shared travelInWater constants for Charm of Sinking / Flippers spoofs.
 * Keep formulas in the call sites. Only the duplicated numbers live here.
 */
final class SwimSpoofMath {
    /** Vanilla moveRelative swim acceleration before WME scaling. */
    static final double BASE_SWIM_ACCEL = 0.02;
    /** Vanilla travelInWater lerps water drag toward this as WME rises. */
    static final double WME_DRAG_TARGET = 0.54600006;

    /** Vanilla water horizontal drag before WME lerp (sprint vs walk). */
    static double baseWaterDrag(boolean sprinting) {
        return sprinting ? 0.9 : 0.8;
    }

    /** {@code d0 + (WME_DRAG_TARGET - d0) * effectiveWme}. */
    static double waterDrag(double d0, double effectiveWme) {
        return d0 + (WME_DRAG_TARGET - d0) * effectiveWme;
    }
}
