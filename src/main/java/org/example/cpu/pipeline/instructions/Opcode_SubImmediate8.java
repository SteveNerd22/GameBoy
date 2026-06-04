package org.example.cpu.pipeline.instructions;

import org.example.cpu.SM83;

@CpuOpcode(value = {
        0xD6, // SUB A, n
        0xDE  // SBC A, n
})
public final class Opcode_SubImmediate8 extends CpuInstruction {

    @Override
    protected boolean executeStep(int step, int opcode, SM83 cpu) {
        if (step == 0) {
            cpu.PC.emit();
            cpu.Z.sampleSoCBus();
            advanceProgramCounter(cpu);

            cpu.A.emitToAluBus1();
            cpu.Z.emitToAluBus2();

            boolean isSbc = (opcode & 0x08) != 0;
            boolean carryIn = isSbc && cpu.F.isCarrySet();

            int aluFlags = cpu.alu.sbc(carryIn);
            cpu.A.sampleSoCBus();
            cpu.F.set(aluFlags);

            return true;
        }

        throw new IllegalStateException("Step non valido per SUB/SBC A, n: " + step);
    }
}