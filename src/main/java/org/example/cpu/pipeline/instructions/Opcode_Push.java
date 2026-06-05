package org.example.cpu.pipeline.instructions;

import org.example.cpu.RegisterPair;
import org.example.cpu.SM83;

@CpuOpcode(value = {0xC5, 0xD5, 0xE5, 0xF5}) // PUSH BC, DE, HL, AF
public final class Opcode_Push extends CpuInstruction {

    @Override
    protected boolean executeStep(int step, int opcode, SM83 cpu) {
        switch (step) {
            case 0 -> {
                cpu.SP.emit();
                cpu.idu.decrementFormSoC();
                cpu.SP.sampleFromIduBus();
                return false;
            }
            case 1 -> {
                RegisterPair sourcePair = resolveSourcePair(opcode, cpu);
                sourcePair.getHigh().emit();
                cpu.controlUnit.sendWriteSignal();
                cpu.SP.emit();
                cpu.idu.decrementFormSoC();
                cpu.SP.sampleFromIduBus();
                return false;
            }
            case 2 -> {
                RegisterPair sourcePair = resolveSourcePair(opcode, cpu);
                sourcePair.getLow().emit();
                cpu.SP.emit();
                return false;
            }
            case 3 -> {
                cpu.controlUnit.sendReadSignal();
                cpu.PC.emit();
                return true;
            }
            default -> throw new IllegalStateException("Step non valido per PUSH rr: " + step);
        }
    }

    private RegisterPair resolveSourcePair(int opcode, SM83 cpu) {
        return switch ((opcode >> 4) & 0x03) {
            case 0 -> cpu.BC;
            case 1 -> cpu.DE;
            case 2 -> cpu.HL;
            case 3 -> cpu.AF;
            default -> throw new IllegalArgumentException("Opcode non valido per PUSH: " + opcode);
        };
    }
}