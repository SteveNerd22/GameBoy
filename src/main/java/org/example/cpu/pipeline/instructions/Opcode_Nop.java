package org.example.cpu.pipeline.instructions;

import org.example.cpu.SM83;

@CpuOpcode(value = 0x00)
public final class Opcode_Nop extends CpuInstruction {
    @Override
    public void executeCycle(SM83 cpu) {
        terminate();
    }
}
