package org.example.cpu.pipeline.instructions;

import org.example.Main;

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
                        boolean isCb = annotation.isCb();

                        // Creiamo UNA SOLA istanza della classe condivisa (es. Opcode_LdRegHl)
                        CpuInstruction instance = (CpuInstruction) clazz.getDeclaredConstructor().newInstance();

                        // Iteriamo su tutti gli opcode dichiarati nell'array dell'annotazione
                        for (int rawOpcode : annotation.value()) {
                            int opcodeValue = rawOpcode & 0xFF; // Sicurezza a 8-bit

                            if (isCb) {
                                cbInstructions[opcodeValue] = instance;
                            } else {
                                baseInstructions[opcodeValue] = instance;
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("FATAL: Failed to auto-register sealed instruction: " + clazz.getSimpleName());
                    e.printStackTrace();
                    System.exit(1);
                }
            }
        }
        if (Main.DEBUG) {
            System.out.println(registeredOpcodes());
        }
    }

    private static String registeredOpcodes() {
        int baseRegistered = 0;
        int cbRegistered = 0;

        // 1. Contiamo quanti opcode effettivi abbiamo registrato
        for (int i = 0; i < 256; i++) {
            if (!(baseInstructions[i] instanceof UnimplementedInstruction)) baseRegistered++;
            if (!(cbInstructions[i] instanceof UnimplementedInstruction)) cbRegistered++;
        }

        StringBuilder report = new StringBuilder();
        report.append("\n==================================================\n");
        report.append("          SM83 INSTRUCTION REGISTRY REPORT        \n");
        report.append("==================================================\n");
        report.append(String.format("BASE OPCODES : %3d / 256 COPERTI (Mancano: %3d)\n", baseRegistered, (256 - baseRegistered)));
        report.append(String.format("CB OPCODES   : %3d / 256 COPERTI (Mancano: %3d)\n", cbRegistered, (256 - cbRegistered)));
        report.append("==================================================\n");

        // 2. Generiamo la griglia visiva d'impatto per le istruzioni BASE
        report.append("MAPPA COPERTURA INSTRUCTION SET (BASE):\n");
        report.append("   0  1  2  3  4  5  6  7  8  9  A  B  C  D  E  F\n"); // Intestazione colonne

        for (int row = 0; row < 16; row++) {
            report.append(String.format("%X ", row)); // Intestazione riga (0x0_, 0x1_, ecc.)
            for (int col = 0; col < 16; col++) {
                int index = (row << 4) | col;

                if (baseInstructions[index] instanceof UnimplementedInstruction) {
                    report.append(" . "); // Un punto per gli opcode di default (ancora vuoti)
                } else {
                    report.append(" # "); // Un cancelletto (o "X") per quelli registrati con successo
                }
            }
            report.append("\n");
        }
        report.append("==================================================");

        return report.toString();
    }

    public static CpuInstruction get(int opcode, boolean isCBSet) {
        if(isCBSet) return cbInstructions[opcode &  0xFF];
        return baseInstructions[opcode & 0xFF];
    }

    public static CpuInstruction getCB(int opcode) {
        return cbInstructions[opcode & 0xFF];
    }
}