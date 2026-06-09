package org.example.cpu.pipeline.instructions;

import org.example.cpu.SM83;
import org.example.cpu.FlagsRegister;

@CpuOpcode(value = {
        0xC4, // CALL NZ, nn
        0xCC, // CALL Z, nn
        0xD4, // CALL NC, nn
        0xDC  // CALL C, nn
})
public final class Opcode_CallImmediate16Conditional extends CpuInstruction {

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
                if (!checkCondition(opcode, cpu)) {
                    cpu.PC.emit();
                    return true;
                }
                cpu.SP.emit();
                cpu.idu.decrementFormSoC();
                cpu.SP.sampleFromIduBus();
                return false;
            }
            case 3 -> {
                cpu.controlUnit.sendWriteSignal();
                cpu.PC.getHigh().emit();
                cpu.SP.emit();
                cpu.idu.decrementFormSoC();
                cpu.SP.sampleFromIduBus();
                return false;
            }
            case 4 -> {
                cpu.PC.getLow().emit();
                cpu.SP.emit();
                cpu.idu.copyFormSoC();
                cpu.SP.sampleFromIduBus();
                cpu.PC.set(cpu.WZ.get());
                return false;
            }
            case 5 -> {
                cpu.controlUnit.sendReadSignal();
                cpu.PC.emit();
                return true;
            }
        }
        throw new IllegalStateException("Step non valido per CALL condizionale: " + step);
    }

    /**
     * Controlla lo stato dei flag in base all'opcode corrente
     */
    private boolean checkCondition(int opcode, SM83 cpu) {
        int flags = cpu.F.get();
        boolean isZeroSet = (flags & FlagsRegister.MASK_Z) != 0;
        boolean isCarrySet = (flags & FlagsRegister.MASK_C) != 0;

        return switch (opcode) {
            case 0xC4 -> !isZeroSet;  // NZ
            case 0xCC -> isZeroSet;   // Z
            case 0xD4 -> !isCarrySet; // NC
            case 0xDC -> isCarrySet;  // C
            default -> throw new IllegalArgumentException("Opcode sconosciuto per CALL condizionale: " + String.format("0x%02X", opcode));
        };
    }
}