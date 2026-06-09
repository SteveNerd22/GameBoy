package org.example.cpu.pipeline.instructions;

import org.example.cpu.SM83;
import org.example.cpu.FlagsRegister;

@CpuOpcode(value = {
        0xC0, // RET NZ (Return if Not Zero)
        0xC8, // RET Z  (Return if Zero)
        0xD0, // RET NC (Return if Not Carry)
        0xD8  // RET C  (Return if Carry)
})
public final class Opcode_ReturnConditional extends CpuInstruction {

    @Override
    protected boolean executeStep(int step, int opcode, SM83 cpu) {
        switch (step) {
            case 0 -> {
                return false;
            }
            case 1 -> {
                if (!checkCondition(opcode, cpu)) {
                    cpu.PC.emit();
                    return true;
                }

                cpu.SP.emit();
                cpu.Z.sampleSoCBus();
                cpu.idu.incrementFormSoC();
                cpu.SP.sampleFromIduBus();
                return false;
            }
            case 2 -> {
                cpu.SP.emit();
                cpu.W.sampleSoCBus();
                cpu.idu.incrementFormSoC();
                cpu.SP.sampleFromIduBus();
                return false;
            }
            case 3 -> {
                cpu.PC.set(cpu.WZ.get());
                return false;
            }
            case 4 -> {
                cpu.PC.emit();
                return true;
            }
        }
        throw new IllegalStateException("Step non valido per RET condizionale: " + step);
    }

    /**
     * Controlla lo stato dei flag nel registro F in base all'opcode corrente
     */
    private boolean checkCondition(int opcode, SM83 cpu) {
        int flags = cpu.F.get();
        boolean isZeroSet = (flags & FlagsRegister.MASK_Z) != 0;
        boolean isCarrySet = (flags & FlagsRegister.MASK_C) != 0;

        return switch (opcode) {
            case 0xC0 -> !isZeroSet;  // NZ
            case 0xC8 -> isZeroSet;   // Z
            case 0xD0 -> !isCarrySet; // NC
            case 0xD8 -> isCarrySet;  // C
            default -> throw new IllegalArgumentException("Opcode sconosciuto per RET condizionale: " + String.format("0x%02X", opcode));
        };
    }
}