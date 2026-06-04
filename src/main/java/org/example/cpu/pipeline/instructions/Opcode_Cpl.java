package org.example.cpu.pipeline.instructions;

import org.example.cpu.SM83;

@CpuOpcode(value = {0x2F}) // CPL
public final class Opcode_Cpl extends CpuInstruction {

    @Override
    protected boolean executeStep(int step, int opcode, SM83 cpu) {
        if (step == 0) {
            cpu.A.emitToAluBus1();
            int currentFlags = cpu.F.get();
            int aluFlags = cpu.alu.cpl(currentFlags);
            cpu.A.sampleSoCBus();
            cpu.F.set(aluFlags);

            return true;
        }

        throw new IllegalStateException("Step non valido per CPL: " + step);
    }
}