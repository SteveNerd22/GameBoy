package org.example.cpu.pipeline.instructions;

import org.example.cpu.SM83;
import org.example.cpu.FlagsRegister;

@CpuOpcode(
        value = {
                0x40, 0x41, 0x42, 0x43, 0x44, 0x45, 0x47, // BIT 0
                0x48, 0x49, 0x4A, 0x4B, 0x4C, 0x4D, 0x4F, // BIT 1
                0xD0, 0x51, 0x52, 0x53, 0x54, 0x55, 0x57, // BIT 2
                0x58, 0x59, 0x5A, 0x5B, 0x5C, 0x5D, 0x5F, // BIT 3
                0x60, 0x61, 0x62, 0x63, 0x64, 0x65, 0x67, // BIT 4
                0x68, 0x69, 0x6A, 0x6B, 0x6C, 0x6D, 0x6F, // BIT 5
                0x70, 0x71, 0x72, 0x73, 0x74, 0x75, 0x77, // BIT 6
                0x78, 0x79, 0x7A, 0x7B, 0x7C, 0x7D, 0xFF  // BIT 7
        },
        isCb = true
)
public final class Opcode_CB_Bit extends CpuInstruction {

    @Override
    protected boolean executeStep(int step, int opcode, SM83 cpu) {
        switch (step) {
            case 0 -> {
                int bitIndex = (opcode >> 3) & 0x07;
                int regIndex = opcode & 0x07;
                int regValue = getRegisterValue(regIndex, cpu);
                boolean isBitSet = (regValue & (1 << bitIndex)) != 0;
                int currentFlags = cpu.F.get();
                if (!isBitSet) {
                    currentFlags |= FlagsRegister.MASK_Z;
                } else {
                    currentFlags &= ~FlagsRegister.MASK_Z;
                }
                currentFlags &= ~FlagsRegister.MASK_N;
                currentFlags |= FlagsRegister.MASK_H;
                cpu.F.set(currentFlags);
                cpu.PC.emit();
                return true;
            }
        }
        throw new IllegalStateException("Step non valido per BIT: " + step);
    }

    private int getRegisterValue(int regIndex, SM83 cpu) {
        return switch (regIndex) {
            case 0 -> cpu.B.get();
            case 1 -> cpu.C.get();
            case 2 -> cpu.D.get();
            case 3 -> cpu.E.get();
            case 4 -> cpu.H.get();
            case 5 -> cpu.L.get();
            case 7 -> cpu.A.get();
            default -> throw new IllegalArgumentException("Registro CB non supportato o invalido: " + regIndex);
        };
    }
}