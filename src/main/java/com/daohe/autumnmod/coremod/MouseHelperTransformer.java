package com.daohe.autumnmod.coremod;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import static org.objectweb.asm.Opcodes.*;

import java.util.List;

public class MouseHelperTransformer implements IClassTransformer {

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if ("net.minecraft.util.MouseHelper".equals(transformedName)) {
            System.out.println("[UhcCraftHelper] Transforming MouseHelper class");
            return transformMouseHelper(basicClass);
        }
        return basicClass;
    }

    private byte[] transformMouseHelper(byte[] basicClass) {
        try {
            ClassNode classNode = new ClassNode();
            ClassReader classReader = new ClassReader(basicClass);
            classReader.accept(classNode, 0);

            @SuppressWarnings("unchecked")
            List<MethodNode> methods = (List<MethodNode>) classNode.methods;

            for (MethodNode method : methods) {
                if ("ungrabMouseCursor".equals(method.name) || "func_74373_b".equals(method.name)) {
                    System.out.println("[UhcCraftHelper] Found ungrabMouseCursor method: " + method.name);
                    transformUngrabMethod(method);
                    break;
                }
            }

            ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            classNode.accept(classWriter);
            return classWriter.toByteArray();

        } catch (Exception e) {
            System.err.println("[UhcCraftHelper] Error transforming MouseHelper: " + e.getMessage());
            e.printStackTrace();
            return basicClass;
        }
    }

    private void transformUngrabMethod(MethodNode method) {
        InsnList instructions = new InsnList();

        instructions.add(new MethodInsnNode(INVOKESTATIC,
                "com/daohe/autumnmod/UhcCraftHelper",
                "shouldPreventMouseReset",
                "()Z",
                false));

        LabelNode continueLabel = new LabelNode();
        instructions.add(new JumpInsnNode(IFEQ, continueLabel));
        instructions.add(new InsnNode(RETURN));
        instructions.add(continueLabel);

        method.instructions.insert(instructions);

        System.out.println("[UhcCraftHelper] Successfully transformed ungrabMouseCursor method");
    }
}