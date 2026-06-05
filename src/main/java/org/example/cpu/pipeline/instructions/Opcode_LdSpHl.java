package org.example.cpu.pipeline.instructions;

import org.example.cpu.SM83;

@CpuOpcode(value = {0xF9}) // LD SP, HL
public final class Opcode_LdSpHl extends CpuInstruction {

    @Override
    protected boolean executeStep(int step, int opcode, SM83 cpu) {
        switch (step) {
            case 0-> {
                cpu.HL.emit();
                cpu.idu.copyFormSoC();
                cpu.SP.sampleFromIduBus();
                return false;
            }
            case 1-> {
                cpu.PC.emit();
                return true;
            }
        }

        throw new IllegalStateException("Step non valido per LD SP, HL: " + step);
    }
}