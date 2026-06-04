package org.example.cpu.pipeline.instructions;

import org.example.cpu.RegisterPair;
import org.example.cpu.SM83;

@CpuOpcode(value = {
        0x03, 0x0B, // INC BC, DEC BC
        0x13, 0x1B, // INC DE, DEC DE
        0x23, 0x2B, // INC HL, DEC HL
        0x33, 0x3B  // INC SP, DEC SP
})
public final class Opcode_IncDecRegister16 extends CpuInstruction {

    @Override
    protected boolean executeStep(int step, int opcode, SM83 cpu) {
        if (step == 0) {
            RegisterPair targetReg = resolveRegisterPair(opcode, cpu);
            targetReg.emit();

            boolean isDec = (opcode & 0x08) != 0;
            if (isDec) {
                cpu.idu.decrementFormSoC();
            } else {
                cpu.idu.incrementFormSoC();
            }

            targetReg.sampleFromIduBus();

            return true;
        }

        throw new IllegalStateException("Step non valido per INC/DEC rr: " + step);
    }
}