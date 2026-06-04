package org.example.cpu.pipeline.instructions;

import org.example.cpu.Register;
import org.example.cpu.SM83;

@CpuOpcode(value = {
        0xA0, 0xA1, 0xA2, 0xA3, 0xA4, 0xA5, 0xA7, // AND r
        0xA8, 0xA9, 0xAA, 0xAB, 0xAC, 0xAD, 0xAF, // XOR r
        0xB0, 0xB1, 0xB2, 0xB3, 0xB4, 0xB5, 0xB7  // OR r
})
public final class Opcode_LogicRegister8 extends CpuInstruction {

    @Override
    protected boolean executeStep(int step, int opcode, SM83 cpu) {
        if (step == 0) {
            cpu.A.emitToAluBus1();
            Register sourceReg = resolveSourceRegister(opcode, cpu);
            sourceReg.emitToAluBus2();

            int operation = (opcode >> 3) & 0x07;
            int aluFlags = switch (operation) {
                case 4 -> cpu.alu.and(); // 1010 0xxx -> AND
                case 5 -> cpu.alu.xor(); // 1010 1xxx -> XOR
                case 6 -> cpu.alu.or();  // 1011 0xxx -> OR
                default -> throw new IllegalArgumentException("Operazione logica non valida: " + operation);
            };

            cpu.A.sampleSoCBus();
            cpu.F.set(aluFlags);

            return true;
        }

        throw new IllegalStateException("Step non valido per Operatori Logici r: " + step);
    }
}