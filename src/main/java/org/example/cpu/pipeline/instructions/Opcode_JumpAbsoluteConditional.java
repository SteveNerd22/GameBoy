package org.example.cpu.pipeline.instructions;

import org.example.cpu.SM83;
import org.example.cpu.FlagsRegister;

@CpuOpcode(value = {
        0xC2, // JP NZ, nn
        0xCA, // JP Z, nn
        0xD2, // JP NC, nn
        0xDA  // JP C, nn
})
public final class Opcode_JumpAbsoluteConditional extends CpuInstruction {

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
                cpu.WZ.emit();
                cpu.idu.copyFormSoC();
                cpu.PC.sampleFromIduBus();
                return false;
            }
            case 3 -> {
                cpu.PC.emit();
                return true;
            }
        }

        throw new IllegalStateException("Step non valido per JP condizionale: " + step);
    }

    /**
     * Controlla lo stato dei flag in base all'opcode JP corrente
     */
    private boolean checkCondition(int opcode, SM83 cpu) {
        int flags = cpu.F.get();
        boolean isZeroSet = (flags & FlagsRegister.MASK_Z) != 0;
        boolean isCarrySet = (flags & FlagsRegister.MASK_C) != 0;

        return switch (opcode) {
            case 0xC2 -> !isZeroSet;  // NZ
            case 0xCA -> isZeroSet;   // Z
            case 0xD2 -> !isCarrySet; // NC
            case 0xDA -> isCarrySet;  // C
            default -> throw new IllegalArgumentException("Opcode sconosciuto per JP: " + String.format("0x%02X", opcode));
        };
    }
}