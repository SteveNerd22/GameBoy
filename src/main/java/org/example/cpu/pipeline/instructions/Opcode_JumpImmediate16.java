package org.example.cpu.pipeline.instructions;

import org.example.cpu.SM83;

@CpuOpcode(value = 0xC3)
public final class Opcode_JumpImmediate16 extends CpuInstruction {
    @Override
    protected boolean executeStep(int step, int opcode, SM83 cpu) {
        switch (step) {
            case 0 -> {
                cpu.PC.emit();
                cpu.Z.sampleSoCBus();
                advanceProgramCounter(cpu);
                return false;
            }
            case 1 -> {
                cpu.PC.emit();
                cpu.W.sampleSoCBus();
                advanceProgramCounter(cpu);
                return false;
            }
            case 2 -> {
                cpu.PC.set(cpu.WZ.get());
                return false;
            }
            case 3 -> {
                cpu.PC.emit();
                return true;
            }
        }

        throw new IllegalStateException("Step non valido per JP nn: " + step);
    }
}
