package org.example.cpu.pipeline.instructions;

import org.example.cpu.SM83;

@CpuOpcode(value = 0xC9)
public final class Opcode_Return extends CpuInstruction{

    @Override
    protected boolean executeStep(int step, int opcode, SM83 cpu) {
        switch (step) {
            case 0 -> {
                cpu.SP.emit();
                cpu.Z.sampleSoCBus();
                cpu.idu.incrementFormSoC();
                cpu.SP.sampleFromIduBus();
                return false;
            }
            case 1 -> {
                cpu.SP.emit();
                cpu.W.sampleSoCBus();
                cpu.idu.incrementFormSoC();
                cpu.SP.sampleFromIduBus();
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
        throw new IllegalStateException("Step non valido per RET: " + step);
    }
}
