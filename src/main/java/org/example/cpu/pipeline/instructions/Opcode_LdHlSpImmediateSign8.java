package org.example.cpu.pipeline.instructions;

import org.example.cpu.SM83;

import static org.example.cpu.FlagsRegister.MASK_C;
import static org.example.cpu.FlagsRegister.MASK_H;

@CpuOpcode(value = {0xF8}) // LD HL, SP+e
public final class Opcode_LdHlSpImmediateSign8 extends CpuInstruction {

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
                cpu.SP.getLow().emitToAluBus1();
                cpu.Z.emitToAluBus2();
                int newFlags = cpu.alu.add();
                cpu.L.sampleSoCBus();
                int finalFlags = 0;
                if ((newFlags & MASK_H) != 0) finalFlags |= MASK_H;
                if ((newFlags & MASK_C) != 0) finalFlags |= MASK_C;
                cpu.F.set(finalFlags);
                return false;
            }
            case 2 -> {
                boolean carryFromStep1 = (cpu.F.get() & MASK_C) != 0;
                int adj = (cpu.Z.get() & 0x80) != 0 ? 0xFF : 0x00;
                cpu.SP.getHigh().emitToAluBus1();
                cpu.regToAlu2Emit(adj);
                cpu.alu.adc(carryFromStep1);
                cpu.H.sampleSoCBus();
                cpu.PC.emit();
                return true;
            }
            default -> throw new IllegalStateException("Step non valido per LD HL, SP+e: " + step);
        }
    }
}