package org.example.cpu.pipeline.instructions;

import org.example.cpu.Register;
import org.example.cpu.SM83;

@CpuOpcode(value = {
        0x80, 0x81, 0x82, 0x83, 0x84, 0x85, 0x87, // ADD A, r
        0x88, 0x89, 0x8A, 0x8B, 0x8C, 0x8D, 0x8F  // ADC A, r
})
public final class Opcode_AddRegister8 extends CpuInstruction {

    @Override
    protected boolean executeStep(int step, int opcode, SM83 cpu) {
        if (step == 0) {
            cpu.A.emitToAluBus1();
            Register sourceReg = resolveSourceRegister(opcode, cpu);
            sourceReg.emitToAluBus2();
            boolean isAdc = (opcode & 0x08) != 0;
            boolean carryIn = isAdc && cpu.F.isCarrySet();
            int aluFlags = cpu.alu.adc(carryIn);
            cpu.A.sampleSoCBus();
            cpu.F.set(aluFlags);
            cpu.PC.emit();
            return true;
        }

        throw new IllegalStateException("Step non valido per ADD/ADC A, r: " + step);
    }
}