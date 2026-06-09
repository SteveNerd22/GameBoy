package org.example.cpu.pipeline.instructions;

import org.example.cpu.SM83;

@CpuOpcode(value = 0x76) // HALT
public final class Opcode_Halt extends CpuInstruction {

    @Override
    protected boolean executeStep(int step, int opcode, SM83 cpu) {
        if (step == 0) {
            cpu.controlUnit.enterHaltMode();
            cpu.PC.emit();
            return true;
        }
        throw new IllegalStateException("Step non valido per HALT: " + step);
    }
}