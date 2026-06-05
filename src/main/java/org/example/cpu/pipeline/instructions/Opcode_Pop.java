package org.example.cpu.pipeline.instructions;

import org.example.cpu.RegisterPair;
import org.example.cpu.SM83;

@CpuOpcode(value = {0xC1, 0xD1, 0xE1, 0xF1}) // POP BC, DE, HL, AF
public final class Opcode_Pop extends CpuInstruction {

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
                RegisterPair targetPair = resolveTargetPair(opcode, cpu);
                targetPair.set(cpu.WZ.get());
                cpu.PC.emit();
                return true;
            }
            default -> throw new IllegalStateException("Step non valido per POP rr: " + step);
        }
    }

    private RegisterPair resolveTargetPair(int opcode, SM83 cpu) {
        return switch ((opcode >> 4) & 0x03) {
            case 0 -> cpu.BC;
            case 1 -> cpu.DE;
            case 2 -> cpu.HL;
            case 3 -> cpu.AF;
            default -> throw new IllegalArgumentException("Opcode non valido per POP: " + opcode);
        };
    }
}