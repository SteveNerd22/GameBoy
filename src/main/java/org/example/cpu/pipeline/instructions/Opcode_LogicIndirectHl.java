package org.example.cpu.pipeline.instructions;

import org.example.cpu.SM83;

@CpuOpcode(value = {
        0xA6, // AND A, (HL)
        0xAE, // XOR A, (HL)
        0xB6  // OR A, (HL)
})
public final class Opcode_LogicIndirectHl extends CpuInstruction {

    @Override
    protected boolean executeStep(int step, int opcode, SM83 cpu) {
        if (step == 0) {
            cpu.HL.emit();
            cpu.Z.sampleSoCBus();

            cpu.A.emitToAluBus1();
            cpu.Z.emitToAluBus2();

            int operation = (opcode >> 3) & 0x07;
            int aluFlags = switch (operation) {
                case 4 -> cpu.alu.and(); // 1010 0110 -> AND
                case 5 -> cpu.alu.xor(); // 1010 1110 -> XOR
                case 6 -> cpu.alu.or();  // 1011 0110 -> OR
                default -> throw new IllegalArgumentException("Operazione logica indiretta non valida: " + operation);
            };

            cpu.A.sampleSoCBus();
            cpu.F.set(aluFlags);

            return true;
        }

        throw new IllegalStateException("Step non valido per Operatori Logici (HL): " + step);
    }
}