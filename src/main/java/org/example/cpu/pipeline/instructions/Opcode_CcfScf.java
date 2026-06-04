package org.example.cpu.pipeline.instructions;

import org.example.cpu.SM83;

@CpuOpcode(value = {
        0x37, // SCF
        0x3F  // CCF
})
public final class Opcode_CcfScf extends CpuInstruction {

    @Override
    protected boolean executeStep(int step, int opcode, SM83 cpu) {
        if (step == 0) {
            int currentFlags = cpu.F.get();
            int aluFlags;

            boolean isCcf = (opcode & 0x08) != 0;

            if (isCcf) {
                aluFlags = cpu.alu.ccf(currentFlags);
            } else {
                aluFlags = cpu.alu.scf(currentFlags);
            }

            cpu.F.set(aluFlags);

            return true;
        }

        throw new IllegalStateException("Step non valido per SCF/CCF: " + step);
    }
}