package org.example.cpu.pipeline.instructions;

import org.example.cpu.SM83;

@CpuOpcode(value = {0x08}) // LD (nn), SP
public final class Opcode_LdIndirectImmediate16Sp extends CpuInstruction {

    @Override
    protected boolean executeStep(int step, int opcode, SM83 cpu) {
        switch (step) {
            case 0 -> {
                // === M-CYCLE 2: Lettura byte basso dell'indirizzo ===
                cpu.PC.emit();
                cpu.Z.sampleSoCBus();
                advanceProgramCounter(cpu);
                return false;
            }
            case 1 -> {
                // === M-CYCLE 3: Lettura byte alto dell'indirizzo ===
                cpu.PC.emit();
                cpu.W.sampleSoCBus();
                advanceProgramCounter(cpu);
                return false;
            }
            case 2 -> {
                cpu.WZ.emit();
                cpu.SP.getLow().emit();

                cpu.idu.incrementFormSoC();
                cpu.WZ.sampleFromIduBus();

                return false;
            }
            case 3 -> {
                cpu.WZ.emit();

                cpu.SP.getHigh().emit();

                return true;
            }
            default -> throw new IllegalStateException("Step non valido per LD (nn), SP: " + step);
        }
    }
}