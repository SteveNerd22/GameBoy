package org.example.cpu.pipeline.instructions;

import org.example.cpu.SM83;

@CpuOpcode(value = {0xF3, 0xFB}) // DI, EI
public final class Opcode_DIEI extends CpuInstruction{
    @Override
    protected boolean executeStep(int step, int opcode, SM83 cpu) {
        if(step == 0) {
            if(opcode == 0xF3)
                cpu.controlUnit.disableInterrupt();
            else
                cpu.controlUnit.enableInterrupt();
            return true;
        }
        throw new IllegalStateException("Step non valido per DI: " + step);
    }
}
