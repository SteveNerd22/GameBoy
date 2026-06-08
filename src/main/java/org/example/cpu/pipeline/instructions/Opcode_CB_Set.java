package org.example.cpu.pipeline.instructions;

import org.example.cpu.SM83;

@CpuOpcode(
        value = {
                0xC0, 0xC1, 0xC2, 0xC3, 0xC4, 0xC5, 0xC7, // SET 0 (Registri)
                0xC8, 0xC9, 0xCA, 0xCB, 0xCC, 0xCD, 0xCF, // SET 1 (Registri)
                0xD0, 0xD1, 0xD2, 0xD3, 0xD4, 0xD5, 0xD7, // SET 2 (Registri)
                0xD8, 0xD9, 0xDA, 0xDB, 0xDC, 0xDD, 0xDF, // SET 3 (Registri)
                0xE0, 0xE1, 0xE2, 0xE3, 0xE4, 0xE5, 0xE7, // SET 4 (Registri)
                0xE8, 0xE9, 0xEA, 0xEB, 0xEC, 0xED, 0xEF, // SET 5 (Registri)
                0xF0, 0xF1, 0xF2, 0xF3, 0xF4, 0xF5, 0xF7, // SET 6 (Registri)
                0xF8, 0xF9, 0xFA, 0xFB, 0xFC, 0xFD, 0xFF  // SET 7 (Registri)
        },
        isCb = true
)
public final class Opcode_CB_Set extends CpuInstruction {

    @Override
    protected boolean executeStep(int step, int opcode, SM83 cpu) {
        switch (step) {
            case 0 -> {
                int bitIndex = (opcode >> 3) & 0x07;
                int regIndex = opcode & 0x07;
                int originalValue = getRegisterValue(regIndex, cpu);
                int newValue = originalValue | (1 << bitIndex);
                setRegisterValue(regIndex, newValue, cpu);
                cpu.PC.emit();
                return true;
            }
        }
        throw new IllegalStateException("Step non valido per SET: " + step);
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

    private void setRegisterValue(int regIndex, int value, SM83 cpu) {
        switch (regIndex) {
            case 0 -> cpu.B.set(value);
            case 1 -> cpu.C.set(value);
            case 2 -> cpu.D.set(value);
            case 3 -> cpu.E.set(value);
            case 4 -> cpu.H.set(value);
            case 5 -> cpu.L.set(value);
            case 7 -> cpu.A.set(value);
            default -> throw new IllegalArgumentException("Registro CB non supportato o invalido: " + regIndex);
        }
    }
}