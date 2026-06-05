package org.example.cpu.pipeline.instructions;

import org.example.cpu.SM83;

@CpuOpcode(value = {0xBE}) // CP (HL)
public final class Opcode_CompareIndirectHl extends CpuInstruction {

    @Override
    protected boolean executeStep(int step, int opcode, SM83 cpu) {
        switch (step) {
            case 0 -> {
                cpu.HL.emit();
                cpu.Z.sampleSoCBus();
                return false;
            }
            case 1 -> {
                cpu.A.emitToAluBus1();
                cpu.Z.emitToAluBus2();
                int aluFlags = cpu.alu.sub(false);
                cpu.F.set(aluFlags);
                cpu.PC.emit();
                return true;
            }
        }

        throw new IllegalStateException("Step non valido per CP (HL): " + step);
    }
}