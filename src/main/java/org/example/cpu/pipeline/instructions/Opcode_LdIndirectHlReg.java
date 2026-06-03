package org.example.cpu.pipeline.instructions;

import org.example.cpu.Register;
import org.example.cpu.SM83;

@CpuOpcode(value = {0x70, 0x71, 0x72, 0x73, 0x74, 0x75, 0x77})
public final class Opcode_LdIndirectHlReg extends CpuInstruction {

    @Override
    protected boolean executeStep(int step, int opcode, SM83 cpu) {
        switch (step) {
            case 0 -> {
                cpu.HL.emit();
                Register sourceRegister = resolveSourceRegister(opcode, cpu);

                sourceRegister.emit();
                return true;
            }
            default -> throw new IllegalStateException("Step non valido per LD (HL), r: " + step);
        }
    }
}