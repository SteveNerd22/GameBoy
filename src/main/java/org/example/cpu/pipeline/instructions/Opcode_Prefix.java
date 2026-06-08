package org.example.cpu.pipeline.instructions;

import org.example.cpu.SM83;

@CpuOpcode(value = 0xCB)
public final class Opcode_Prefix extends CpuInstruction{
    @Override
    protected boolean executeStep(int step, int opcode, SM83 cpu) {
        if(step == 0) {
            cpu.controlUnit.setCBPrefix();
            cpu.PC.emit();
            return true;
        }
        throw new IllegalStateException("Step non valido per Prefix: " + step);
    }
}
