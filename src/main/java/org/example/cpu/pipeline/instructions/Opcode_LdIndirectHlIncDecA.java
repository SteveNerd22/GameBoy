package org.example.cpu.pipeline.instructions;

import org.example.cpu.SM83;

@CpuOpcode(value = {0x22, 0x32}) // 0x22: LD (HL+), A | 0x32: LD (HL-), A
public final class Opcode_LdIndirectHlIncDecA extends CpuInstruction {

    @Override
    protected boolean executeStep(int step, int opcode, SM83 cpu) {
        if (step == 0) {
            cpu.HL.emit();

            cpu.A.emit();

            if ((opcode & 0x10) == 0) {
                cpu.idu.incrementFormSoC(); // HL+
            } else {
                cpu.idu.decrementFormSoC(); // HL-
            }

            cpu.HL.sampleFromIduBus();

            return true;
        }

        throw new IllegalStateException("Step non valido per LD (HL+/-), A: " + step);
    }
}