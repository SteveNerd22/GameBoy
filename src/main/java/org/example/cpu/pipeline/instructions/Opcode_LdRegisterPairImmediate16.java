package org.example.cpu.pipeline.instructions;

import org.example.cpu.RegisterPair;
import org.example.cpu.SM83;

@CpuOpcode(value = {0x01, 0x11, 0x21, 0x31}) // 0x01: BC, 0x11: DE, 0x21: HL, 0x31: SP
public final class Opcode_LdRegisterPairImmediate16 extends CpuInstruction {

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
                RegisterPair targetPair = resolveTargetPair(opcode, cpu);
                targetPair.set(cpu.WZ.get());
                return true;
            }
            default -> throw new IllegalStateException("Step non valido per LD rr, nn: " + step);
        }
    }

    private RegisterPair resolveTargetPair(int opcode, SM83 cpu) {
        // Isoliamo i bit 4 e 5 dell'opcode: (opcode >> 4) & 0x03
        return switch ((opcode >> 4) & 0x03) {
            case 0 -> cpu.BC;
            case 1 -> cpu.DE;
            case 2 -> cpu.HL;
            case 3 -> cpu.SP; // Assumendo che SP estenda RegisterPair o ne simuli i pass-gate
            default -> throw new IllegalArgumentException("Opcode non valido per la selezione del registro: " + opcode);
        };
    }
}