package org.example.cpu.pipeline.instructions;

import org.example.cpu.Register;
import org.example.cpu.SM83;

@CpuOpcode(value = {0x80, 0x81, 0x82, 0x83, 0x84, 0x85, 0x87}) // Escluso 0x86 (HL) che richiede un ciclo RAM in più
public final class Opcode_AddRegister8 extends CpuInstruction {

    @Override
    protected boolean executeStep(int step, int opcode, SM83 cpu) {
        if (step == 0) {
            cpu.A.emitToAluBus1();

            Register sourceReg = resolveSourceRegister(opcode, cpu);
            sourceReg.emitToAluBus2();

            int aluFlags = cpu.alu.add();

            cpu.A.sampleSoCBus();

            cpu.F.set(aluFlags);

            return true;
        }

        throw new IllegalStateException("Step non valido per ADD A, r: " + step);
    }
}