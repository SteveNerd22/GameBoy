package org.example.cpu.pipeline.instructions;

import org.example.cpu.SM83;

@CpuOpcode(value = {0x08}) // LD (nn), SP
public final class Opcode_LdIndirectImmediate16Sp extends CpuInstruction {

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
                cpu.controlUnit.sendWriteSignal();
                cpu.SP.getLow().emit();
                cpu.WZ.emit();
                cpu.idu.incrementFormSoC();
                cpu.WZ.sampleFromIduBus();
                return false;
            }
            case 3 -> {
                cpu.SP.getHigh().emit();
                cpu.WZ.emit();
                return false;
            }
            case 4 -> {
                cpu.controlUnit.sendReadSignal();
                cpu.PC.emit();
                return true;
            }
            default -> throw new IllegalStateException("Step non valido per LD (nn), SP: " + step);
        }
    }
}