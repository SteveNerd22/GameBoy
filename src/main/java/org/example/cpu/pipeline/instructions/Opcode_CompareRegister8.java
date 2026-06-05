package org.example.cpu.pipeline.instructions;

import org.example.cpu.Register;
import org.example.cpu.SM83;

@CpuOpcode(value = {0xB8, 0xB9, 0xBA, 0xBB, 0xBC, 0xBD, 0xBF}) // Escluso 0xBE (HL)
public final class Opcode_CompareRegister8 extends CpuInstruction {

    @Override
    protected boolean executeStep(int step, int opcode, SM83 cpu) {
        if (step == 0) {
            cpu.A.emitToAluBus1();
            Register sourceReg = resolveSourceRegister(opcode, cpu);
            sourceReg.emitToAluBus2();
            int aluFlags = cpu.alu.sub(false);
            cpu.F.set(aluFlags);
            cpu.PC.emit();
            return true;
        }

        throw new IllegalStateException("Step non valido per CP r: " + step);
    }
}