package org.example.cpu.pipeline.instructions;

import org.example.cpu.SM83;

@CpuOpcode(value = {0xF9}) // LD SP, HL
public final class Opcode_LdSpHl extends CpuInstruction {

    @Override
    protected boolean executeStep(int step, int opcode, SM83 cpu) {
        if (step == 0) {
            cpu.SP.set(cpu.HL.get());

            return true;
        }

        throw new IllegalStateException("Step non valido per LD SP, HL: " + step);
    }
}