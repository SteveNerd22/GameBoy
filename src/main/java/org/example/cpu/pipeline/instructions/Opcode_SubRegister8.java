package org.example.cpu.pipeline.instructions;

import org.example.cpu.Register;
import org.example.cpu.SM83;

@CpuOpcode(value = {
        0x90, 0x91, 0x92, 0x93, 0x94, 0x95, 0x97, // SUB r
        0x98, 0x99, 0x9A, 0x9B, 0x9C, 0x9D, 0x9F  // SBC A, r
})
public final class Opcode_SubRegister8 extends CpuInstruction {

    @Override
    protected boolean executeStep(int step, int opcode, SM83 cpu) {
        if (step == 0) {
            cpu.A.emitToAluBus1();
            Register sourceReg = resolveSourceRegister(opcode, cpu);
            sourceReg.emitToAluBus2();
            boolean isSbc = (opcode & 0x08) != 0;
            boolean carryIn = isSbc && cpu.F.isCarrySet();
            int aluFlags = cpu.alu.sbc(carryIn);
            cpu.A.sampleSoCBus();
            cpu.PC.emit();
            cpu.F.set(aluFlags);
            return true;
        }

        throw new IllegalStateException("Step non valido per SUB/SBC A, r: " + step);
    }
}