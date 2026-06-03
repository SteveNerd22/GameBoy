package org.example.cpu.pipeline.instructions;

import org.example.cpu.SM83;

@CpuOpcode(value = {0x86}) // ADD A, (HL)
public final class Opcode_AddIndirectHl extends CpuInstruction {

    @Override
    protected boolean executeStep(int step, int opcode, SM83 cpu) {
        if (step == 0) {
            cpu.HL.emit();
            cpu.Z.sampleSoCBus();

            cpu.A.emitToAluBus1();
            cpu.Z.emitToAluBus2();

            int aluFlags = cpu.alu.add();

            cpu.A.sampleSoCBus();
            cpu.F.set(aluFlags);

            return true;
        }

        throw new IllegalStateException("Step non valido per ADD A, (HL): " + step);
    }
}