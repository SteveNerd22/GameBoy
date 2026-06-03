package org.example.cpu.pipeline.instructions;

import org.example.cpu.SM83;

@CpuOpcode(value = {0xFA}) // LD A, (nn)
public final class Opcode_LdAIndirectImmediate16 extends CpuInstruction {

    @Override
    protected boolean executeStep(int step, int opcode, SM83 cpu) {
        switch (step) {
            case 0 -> {
                cpu.PC.emit();
                cpu.Z.sampleSoCBus();

                advanceProgramCounter(cpu);

                return false;
            }
            case 1 -> {
                cpu.PC.emit();

                cpu.W.sampleSoCBus();
                advanceProgramCounter(cpu);

                cpu.WZ.emit();
                cpu.Z.sampleSoCBus();

                cpu.Z.emitToInternalData();
                cpu.A.sampleInternalData();

                return true;
            }
            default -> throw new IllegalStateException("Step non valido per LD A, (nn): " + step);
        }
    }
}