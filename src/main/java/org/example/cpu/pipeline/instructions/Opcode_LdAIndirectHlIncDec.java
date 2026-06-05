package org.example.cpu.pipeline.instructions;

import org.example.cpu.SM83;

@CpuOpcode(value = {0x2A, 0x3A}) // 0x2A: LD A, (HL+) | 0x3A: LD A, (HL-)
public final class Opcode_LdAIndirectHlIncDec extends CpuInstruction {

    @Override
    protected boolean executeStep(int step, int opcode, SM83 cpu) {
        switch (step) {
            case 0-> {
                cpu.HL.emit();
                cpu.Z.sampleSoCBus();
                if ((opcode & 0x10) == 0) {
                    cpu.idu.incrementFormSoC(); // HL+
                } else {
                    cpu.idu.decrementFormSoC(); // HL-
                }
                cpu.HL.sampleFromIduBus();
                return false;
            }
            case 1-> {
                cpu.PC.emit();
                cpu.Z.emitToInternalData();
                cpu.A.sampleInternalData();
                return true;
            }

        }


        throw new IllegalStateException("Step non valido per LD A, (HL+/-): " + step);
    }
}