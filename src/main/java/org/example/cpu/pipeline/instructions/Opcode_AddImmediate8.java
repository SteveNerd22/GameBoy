package org.example.cpu.pipeline.instructions;

import org.example.cpu.SM83;

@CpuOpcode(value = {
        0xC6, // ADD A, n
        0xCE  // ADC A, n
})
public final class Opcode_AddImmediate8 extends CpuInstruction {

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
                cpu.A.emitToAluBus1();
                cpu.Z.emitToAluBus2();
                boolean isAdc = (opcode & 0x08) != 0;
                boolean carryIn = isAdc && cpu.F.isCarrySet();
                int aluFlags = cpu.alu.adc(carryIn);
                cpu.A.sampleSoCBus();
                cpu.F.set(aluFlags);
                cpu.PC.emit();
                return true;
            }
        }

        throw new IllegalStateException("Step non valido per ADD/ADC A, n: " + step);
    }
}