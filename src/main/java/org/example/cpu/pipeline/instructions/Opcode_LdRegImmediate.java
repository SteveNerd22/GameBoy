package org.example.cpu.pipeline.instructions;

import org.example.cpu.Register;
import org.example.cpu.SM83;

@CpuOpcode(value = {0x06, 0x0E, 0x16, 0x1E, 0x26, 0x2E, 0x3E})
public final class Opcode_LdRegImmediate extends CpuInstruction {

    @Override
    protected boolean executeStep(int step, int opcode, SM83 cpu) {
        switch (step) {
            case 0 -> {
                cpu.PC.emit();

                Register targetRegister = resolveDestRegister(opcode, cpu);
                targetRegister.sampleSoCBus();
                advanceProgramCounter(cpu);

                return true;
            }
            default -> throw new IllegalStateException("Step non valido per LD r, n: " + step);
        }
    }
}