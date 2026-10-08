package magicjinn.artifactspolymer.polymer.mimic;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

/**
 * Reads Artifacts' client Mimic class bytes and extracts mesh / anim / texture
 * metadata without loading client classes. Dedicated servers never load
 * {@code artifacts.client.*}. The .class files still ship in the Artifacts jar.
 * 
 * Let's just pretend I know what I'm doing.
 */
public final class MimicModelBytecodeExtractor {
    private static final String MIMIC_MODEL = "artifacts/client/mimic/MimicModel.class";
    private static final String MIMIC_RENDERER = "artifacts/client/mimic/MimicRenderer.class";
    private static final String CUBE_LIST = "net/minecraft/client/model/geom/builders/CubeListBuilder";
    private static final String PART_POSE = "net/minecraft/client/model/geom/PartPose";
    private static final String PART_DEF = "net/minecraft/client/model/geom/builders/PartDefinition";
    private static final String LAYER_DEF = "net/minecraft/client/model/geom/builders/LayerDefinition";
    private static final String CUBE_DEFORM = "net/minecraft/client/model/geom/builders/CubeDeformation";
    private static final String MESH_DEF = "net/minecraft/client/model/geom/builders/MeshDefinition";

    private MimicModelBytecodeExtractor() {
    }

    private static final Object LOCK = new Object();
    private static ClassNode cachedModelClass;
    private static ClassNode cachedRendererClass;
    private static ExtractedLayer cachedBody;
    private static ExtractedLayer cachedChest;
    private static ExtractedChestAnim cachedAnim;
    private static String cachedTextureAssetPath;

    private static final String CREATE_LAYER_DESCRIPTOR = "()Lnet/minecraft/client/model/geom/builders/LayerDefinition;";

    public static ExtractedLayer extractCreateLayer() {
        synchronized (LOCK) {
            if (cachedBody == null)
                cachedBody = interpretLayer("createLayer",
                        CREATE_LAYER_DESCRIPTOR);

            return cachedBody;
        }
    }

    public static ExtractedLayer extractCreateChestLayer() {
        synchronized (LOCK) {
            if (cachedChest == null)
                cachedChest = interpretLayer(
                        "createChestLayer",
                        CREATE_LAYER_DESCRIPTOR);

            return cachedChest;
        }
    }

    /**
     * Constants from {@code MimicModel#setChestRotations}:
     * {@code lid = max(lidClamp, ticks * lidRate) * degToRad},
     * {@code bottom = min(bottomClamp, ticks * bottomRate) * degToRad}.
     */
    public static ExtractedChestAnim extractChestRotations() {
        synchronized (LOCK) {
            if (cachedAnim == null)
                cachedAnim = interpretChestRotations();

            return cachedAnim;
        }
    }

    /**
     * Pack path under {@code assets/artifacts/}, from {@code MimicRenderer}'s
     * TEXTURE id (e.g. {@code assets/artifacts/textures/entity/mimic.png}).
     */
    public static String extractMimicTextureAssetPath() {
        synchronized (LOCK) {
            if (cachedTextureAssetPath == null)
                cachedTextureAssetPath = interpretMimicTextureAssetPath();

            return cachedTextureAssetPath;
        }
    }

    /**
     * {@code MimicRenderer#extractRenderState} feeds the model
     * {@code (ticksInAir - bias) + partialTick}
     * when airborne. Polymer has no partial tick here, callers should still
     * subtract {@code bias}.
     */
    public static int extractTicksInAirBias() {
        MethodNode method = findMethod(
                mimicRendererClass(),
                "extractRenderState",
                "(Lartifacts/entity/MimicEntity;Lartifacts/client/mimic/MimicRenderState;F)V");
        boolean sawTicksField = false;
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            int op = insn.getOpcode();
            if (op == Opcodes.GETFIELD && insn instanceof FieldInsnNode field
                    && "ticksInAir".equals(field.name))
                sawTicksField = true;

            if (sawTicksField && op == Opcodes.ICONST_1) {
                AbstractInsnNode next = insn.getNext();
                while (next != null && next.getOpcode() < 0) {
                    next = next.getNext();
                }
                if (next != null && next.getOpcode() == Opcodes.ISUB) {
                    return 1;
                }
            }
        }
        return 0;
    }

    private static ExtractedLayer interpretLayer(String methodName, String descriptor) {
        return runLayer(findMethod(mimicModelClass(), methodName, descriptor));
    }

    private static ExtractedChestAnim interpretChestRotations() {
        MethodNode method = findMethod(
                mimicModelClass(),
                "setChestRotations",
                "(Lartifacts/client/mimic/MimicRenderState;)V");
        List<Float> floats = new ArrayList<>();
        boolean sawMax = false;
        boolean sawMin = false;
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn.getOpcode() == Opcodes.LDC && ((LdcInsnNode) insn).cst instanceof Float f)
                floats.add(f);

            if (insn instanceof MethodInsnNode call
                    && "java/lang/Math".equals(call.owner)
                    && "(FF)F".equals(call.desc)) {
                if ("max".equals(call.name)) {
                    sawMax = true;
                } else if ("min".equals(call.name)) {
                    sawMin = true;
                }
            }
        }
        // Expected LDC order: lidClamp, lidRate, deg, bottomClamp, bottomRate, deg
        if (floats.size() < 5 || !sawMax || !sawMin)
            throw new IllegalStateException(
                    "Unexpected MimicModel.setChestRotations constants: floats=" + floats
                            + " max=" + sawMax + " min=" + sawMin);

        return new ExtractedChestAnim(
                floats.get(0),
                floats.get(1),
                floats.get(3),
                floats.get(4),
                floats.get(2));
    }

    private static String interpretMimicTextureAssetPath() {
        MethodNode clinit = findMethod(mimicRendererClass(), "<clinit>", "()V");
        for (AbstractInsnNode insn = clinit.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn.getOpcode() == Opcodes.LDC && ((LdcInsnNode) insn).cst instanceof String path
                    && path.contains("mimic") && path.endsWith(".png"))
                return "assets/artifacts/" + path;
        }
        throw new IllegalStateException("MimicRenderer <clinit> has no mimic.png texture path");
    }

    private static MethodNode findMethod(ClassNode node, String methodName, String descriptor) {
        for (MethodNode m : node.methods) {
            if (methodName.equals(m.name) && descriptor.equals(m.desc))
                return m;
        }
        throw new IllegalStateException("Missing " + node.name + "." + methodName + descriptor);
    }

    private static ClassNode mimicModelClass() {
        synchronized (LOCK) {
            if (cachedModelClass == null)
                cachedModelClass = readClass(MIMIC_MODEL);

            return cachedModelClass;
        }
    }

    private static ClassNode mimicRendererClass() {
        synchronized (LOCK) {
            if (cachedRendererClass == null)
                cachedRendererClass = readClass(MIMIC_RENDERER);

            return cachedRendererClass;
        }
    }

    private static ClassNode readClass(String resourcePath) {
        ClassNode node = new ClassNode();
        new ClassReader(readClasspathBytes(resourcePath)).accept(node,
                ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
        return node;
    }

    private static byte[] readClasspathBytes(String resourcePath) {
        try (InputStream in = MimicModelBytecodeExtractor.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null)
                throw new IllegalStateException(
                        "Cannot find " + resourcePath + " on classpath (Artifacts Fabric jar required)");

            return in.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read " + resourcePath, e);
        }
    }

    private static ExtractedLayer runLayer(MethodNode method) {
        Deque<Object> stack = new ArrayDeque<>();
        Object[] locals = new Object[Math.max(method.maxLocals, 4)];
        List<ExtractedPart> parts = new ArrayList<>();
        int texWidth = -1;
        int texHeight = -1;

        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            int op = insn.getOpcode();
            if (op < 0)
                continue;

            switch (op) {
                case Opcodes.ICONST_M1 -> stack.push(-1);
                case Opcodes.ICONST_0 -> stack.push(0);
                case Opcodes.ICONST_1 -> stack.push(1);
                case Opcodes.ICONST_2 -> stack.push(2);
                case Opcodes.ICONST_3 -> stack.push(3);
                case Opcodes.ICONST_4 -> stack.push(4);
                case Opcodes.ICONST_5 -> stack.push(5);
                case Opcodes.FCONST_0 -> stack.push(0f);
                case Opcodes.FCONST_1 -> stack.push(1f);
                case Opcodes.FCONST_2 -> stack.push(2f);
                case Opcodes.BIPUSH, Opcodes.SIPUSH -> stack.push(((IntInsnNode) insn).operand);
                case Opcodes.LDC -> stack.push(((LdcInsnNode) insn).cst);
                case Opcodes.DUP -> stack.push(stack.peek());
                case Opcodes.POP -> stack.pop();
                case Opcodes.ALOAD -> stack.push(locals[((VarInsnNode) insn).var]);
                case Opcodes.ASTORE -> locals[((VarInsnNode) insn).var] = stack.pop();
                case Opcodes.NEW -> stack.push(new Uninitialized(((TypeInsnNode) insn).desc));
                case Opcodes.INVOKESTATIC, Opcodes.INVOKEVIRTUAL, Opcodes.INVOKESPECIAL -> {
                    LayerResult created = invoke((MethodInsnNode) insn, stack, parts);
                    if (created != null) {
                        texWidth = created.texWidth();
                        texHeight = created.texHeight();
                    }
                }
                case Opcodes.ARETURN, Opcodes.RETURN -> {
                }
                default -> throw new IllegalStateException(
                        "Unsupported opcode " + op + " in MimicModel layer method");
            }
        }

        if (texWidth < 0 || texHeight < 0)
            throw new IllegalStateException("LayerDefinition.create not found while extracting MimicModel layer");

        if (parts.isEmpty())
            throw new IllegalStateException("No parts extracted from MimicModel layer method");

        return new ExtractedLayer(List.copyOf(parts), texWidth, texHeight);
    }

    private static LayerResult invoke(MethodInsnNode call, Deque<Object> stack, List<ExtractedPart> parts) {
        String owner = call.owner;
        String name = call.name;
        String desc = call.desc;
        Type[] argTypes = Type.getArgumentTypes(desc);
        Object[] args = new Object[argTypes.length];
        for (int i = argTypes.length - 1; i >= 0; i--) {
            args[i] = stack.pop();
        }

        if (call.getOpcode() == Opcodes.INVOKESPECIAL && "<init>".equals(name)) {
            stack.pop(); // uninitialized 'this' consumed by <init>
            // NEW/DUP/<init> leaves the DUP'd slot. Replace that Uninitialized with the
            // instance.
            if (stack.peek() instanceof Uninitialized)
                stack.pop();

            if (CUBE_DEFORM.equals(owner) && "(F)V".equals(desc)) {
                stack.push(new Deformation(asFloat(args[0])));
                return null;
            }
            if (MESH_DEF.equals(owner) && "()V".equals(desc)) {
                stack.push(new MeshMark());
                return null;
            }
            throw new IllegalStateException("Unsupported <init> " + owner + desc);
        }

        Object receiver = call.getOpcode() == Opcodes.INVOKESTATIC ? null : stack.pop();

        if (CUBE_LIST.equals(owner) && "create".equals(name)) {
            stack.push(new CubeBuilder());
            return null;
        }
        if (CUBE_LIST.equals(owner) && receiver instanceof CubeBuilder builder) {
            if ("texOffs".equals(name)) {
                builder.u = asInt(args[0]);
                builder.v = asInt(args[1]);
                stack.push(builder);
                return null;
            }
            if ("addBox".equals(name) && desc.startsWith("(FFFFFF")) {
                float deform = 0f;
                if (desc.contains("CubeDeformation"))
                    deform = args[6] instanceof Deformation d ? d.grow() : asFloat(args[6]);

                builder.cubes.add(new ExtractedCube(
                        builder.u, builder.v,
                        asFloat(args[0]), asFloat(args[1]), asFloat(args[2]),
                        asFloat(args[3]), asFloat(args[4]), asFloat(args[5]),
                        deform));
                stack.push(builder);
                return null;
            }
        }

        if (MESH_DEF.equals(owner) && "getRoot".equals(name)) {
            stack.push(new RootMark());
            return null;
        }

        if (PART_POSE.equals(owner) && "offset".equals(name)) {
            stack.push(new Pose(asFloat(args[0]), asFloat(args[1]), asFloat(args[2])));
            return null;
        }

        if (PART_DEF.equals(owner) && "addOrReplaceChild".equals(name)) {
            String partName = Objects.toString(args[0]);
            CubeBuilder cubes = (CubeBuilder) args[1];
            Pose pose = (Pose) args[2];
            parts.add(new ExtractedPart(partName, List.copyOf(cubes.cubes), pose.x(), pose.y(), pose.z()));
            stack.push(new RootMark());
            return null;
        }

        if (LAYER_DEF.equals(owner) && "create".equals(name)) {
            LayerResult result = new LayerResult(asInt(args[1]), asInt(args[2]));
            stack.push(result);
            return result;
        }

        throw new IllegalStateException("Unsupported invoke " + owner + "." + name + desc
                + " receiver=" + receiver);
    }

    private static int asInt(Object v) {
        if (v instanceof Integer i)
            return i;

        if (v instanceof Number n)
            return n.intValue();

        throw new IllegalStateException("Expected int, got " + v);
    }

    private static float asFloat(Object v) {
        if (v instanceof Float f)
            return f;

        if (v instanceof Integer i)
            return i.floatValue();

        if (v instanceof Number n)
            return n.floatValue();

        throw new IllegalStateException("Expected float, got " + v);
    }

    public record ExtractedLayer(List<ExtractedPart> parts, int texWidth, int texHeight) {
    }

    public record ExtractedChestAnim(
            float lidClamp,
            float lidRate,
            float bottomClamp,
            float bottomRate,
            float degToRad) {
    }

    public record ExtractedPart(String name, List<ExtractedCube> cubes, float pivotX, float pivotY, float pivotZ) {
    }

    public record ExtractedCube(
            int u, int v,
            float x, float y, float z,
            float sizeX, float sizeY, float sizeZ,
            float deformation) {
    }

    private static final class CubeBuilder {
        int u;
        int v;
        final List<ExtractedCube> cubes = new ArrayList<>();
    }

    private record Pose(float x, float y, float z) {
    }

    private record Deformation(float grow) {
    }

    private record Uninitialized(String internalName) {
    }

    private static final class MeshMark {
    }

    private static final class RootMark {
    }

    private record LayerResult(int texWidth, int texHeight) {
    }
}
