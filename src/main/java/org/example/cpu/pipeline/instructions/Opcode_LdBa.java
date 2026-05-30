package org.example.cpu.pipeline.instructions;

import org.example.cpu.SM83;
import org.example.cpu.pipeline.instructions.CpuInstruction;

@CpuOpcode(value = 0x47)
public final class Opcode_LdBa extends CpuInstruction {

    @Override
    public void executeCycle(SM83 cpu) {
        int valueOfA = cpu.A.get();
        cpu.B.setValue(valueOfA);

        terminate();
    }
}