package org.example.cpu.pipeline.instructions;

import java.util.List;

public class InstructionRegistry {

    private static final CpuInstruction[] baseInstructions = new CpuInstruction[256];
    private static final CpuInstruction[] cbInstructions = new CpuInstruction[256];

    static {
        initializeRegistry();
    }

    private static void initializeRegistry() {
        for (int i = 0; i < 256; i++) {
            baseInstructions[i] = new UnimplementedInstruction(i, false);
            cbInstructions[i] = new UnimplementedInstruction(i, true);
        }

        Class<?>[] permittedSubclasses = CpuInstruction.class.getPermittedSubclasses();

        if (permittedSubclasses != null) {
            for (Class<?> clazz : permittedSubclasses) {
                try {
                    if (CpuInstruction.class.isAssignableFrom(clazz) && clazz.isAnnotationPresent(CpuOpcode.class)) {

                        CpuOpcode annotation = clazz.getAnnotation(CpuOpcode.class);
                        int opcodeValue = annotation.value() & 0xFF;
                        boolean isCb = annotation.isCb();

                        CpuInstruction instance = (CpuInstruction) clazz.getDeclaredConstructor().newInstance();

                        if (isCb) {
                            cbInstructions[opcodeValue] = instance;
                        } else {
                            baseInstructions[opcodeValue] = instance;
                        }
                    }
                } catch (Exception e) {
                    System.err.println("FATAL: Failed to auto-register sealed instruction: " + clazz.getSimpleName());
                    e.printStackTrace();
                    System.exit(1);
                }
            }
        }
    }

    public static CpuInstruction get(int opcode) {
        return baseInstructions[opcode & 0xFF];
    }

    public static CpuInstruction getCB(int opcode) {
        return cbInstructions[opcode & 0xFF];
    }
}