package org.example.cpu.pipeline.instructions;

import org.example.cpu.SM83;

@CpuOpcode(value = {0x22, 0x32}) // 0x22: LD (HL+), A | 0x32: LD (HL-), A
public final class Opcode_LdIndirectHlIncDecA extends CpuInstruction {

    @Override
    protected boolean executeStep(int step, int opcode, SM83 cpu) {
        switch (step) {
            case 0 -> {
                cpu.A.emit();
                cpu.controlUnit.sendWriteSignal();
                cpu.HL.emit();
                if ((opcode & 0x10) == 0) {
                    cpu.idu.incrementFormSoC(); // HL+
                } else {
                    cpu.idu.decrementFormSoC(); // HL-
                }
                cpu.HL.sampleFromIduBus();
                return false;
            }
            case 1 -> {
                cpu.controlUnit.sendReadSignal();
                cpu.PC.emit();
                return true;
            }
        }

        throw new IllegalStateException("Step non valido per LD (HL+/-), A: " + step);
    }
}