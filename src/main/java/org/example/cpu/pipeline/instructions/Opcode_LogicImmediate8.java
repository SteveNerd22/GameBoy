package org.example.cpu.pipeline.instructions;

import org.example.cpu.SM83;

@CpuOpcode(value = {
        0xE6, // AND A, n
        0xEE, // XOR A, n
        0xF6  // OR A, n
})
public final class Opcode_LogicImmediate8 extends CpuInstruction {

    @Override
    protected boolean executeStep(int step, int opcode, SM83 cpu) {
        switch(step) {
            case 0 -> {
                cpu.PC.emit();
                cpu.Z.sampleSoCBus();
                advanceProgramCounter(cpu);
                return false;
            }
            case 1 -> {
                cpu.A.emitToAluBus1();
                cpu.Z.emitToAluBus2();
                int operation = (opcode >> 3) & 0x07;
                int aluFlags = switch (operation) {
                    case 4 -> cpu.alu.and(); // 1110 0110 -> AND
                    case 5 -> cpu.alu.xor(); // 1110 1110 -> XOR
                    case 6 -> cpu.alu.or();  // 1111 0110 -> OR
                    default ->
                            throw new IllegalArgumentException("Operazione logica immediata non valida: " + operation);
                };
                cpu.A.sampleSoCBus();
                cpu.F.set(aluFlags);
                cpu.PC.emit();
                return true;
            }
        }

        throw new IllegalStateException("Step non valido per Operatori Logici n: " + step);
    }
}