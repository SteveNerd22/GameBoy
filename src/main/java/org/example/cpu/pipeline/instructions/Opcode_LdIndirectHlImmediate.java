package org.example.cpu.pipeline.instructions;

import org.example.cpu.SM83;

@CpuOpcode(value = {0x36}) // LD (HL), n
public final class Opcode_LdIndirectHlImmediate extends CpuInstruction {

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
                cpu.Z.emit();
                cpu.controlUnit.sendWriteSignal();
                cpu.HL.emit();
                return false;
            }
            case 2 -> {
                cpu.controlUnit.sendReadSignal();
                cpu.PC.emit();
                return true;
            }
            default -> throw new IllegalStateException("Step non valido per LD (HL), n: " + step);
        }
    }
}