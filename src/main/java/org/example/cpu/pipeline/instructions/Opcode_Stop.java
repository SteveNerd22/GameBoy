package org.example.cpu.pipeline.instructions;

import org.example.cpu.SM83;

@CpuOpcode(value = 0x10) // STOP
public final class Opcode_Stop extends CpuInstruction {

    @Override
    protected boolean executeStep(int step, int opcode, SM83 cpu) {
        switch (step) {
            case 0 -> {
                cpu.controlUnit.enterStopMode();
                advanceProgramCounter(cpu);
                return false;
            }
            case 1 -> {
                cpu.PC.emit();
                return true;
            }
            default -> throw new IllegalStateException("Step non valido per STOP: " + step);
        }
    }
}