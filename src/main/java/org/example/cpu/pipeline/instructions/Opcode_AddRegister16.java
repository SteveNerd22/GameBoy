package org.example.cpu.pipeline.instructions;

import org.example.cpu.Register;
import org.example.cpu.RegisterPair;
import org.example.cpu.SM83;

@CpuOpcode(value = {
        0x09, // ADD HL, BC
        0x19, // ADD HL, DE
        0x29, // ADD HL, HL
        0x39  // ADD HL, SP
})
public final class Opcode_AddRegister16 extends CpuInstruction {

    @Override
    protected boolean executeStep(int step, int opcode, SM83 cpu) {
        RegisterPair pair = resolveRegisterPair(opcode, cpu);
        Register regHigh = pair.getHigh();
        Register regLow = pair.getLow();

        switch(step) {
            case 0 -> {
                cpu.L.emitToAluBus1();
                regLow.emitToAluBus2();
                int aluFlags = cpu.alu.add();
                cpu.L.sampleSoCBus();
                int oldZ = cpu.F.get() & 0x80;
                cpu.F.set(oldZ | (aluFlags & 0x70));
                return false;
            }
            case 1 -> {
                cpu.H.emitToAluBus1();
                regHigh.emitToAluBus2();
                boolean carryIn = cpu.F.isCarrySet();
                int aluFlags = cpu.alu.adc(carryIn);
                cpu.H.sampleSoCBus();
                int oldZ = cpu.F.get() & 0x80;
                int finalFlags = oldZ | (aluFlags & 0x30);
                cpu.F.set(finalFlags);
                cpu.PC.emit();
                return true;
            }
        }

        throw new IllegalStateException("Step non valido per ADD HL, rr: " + step);
    }
}