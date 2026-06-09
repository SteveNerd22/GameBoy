package org.example.cpu.pipeline.instructions;

import org.example.cpu.SM83;

@CpuOpcode(value = {
        0x17, // RLA
        0x1F  // RRA
})
public final class Opcode_RotateAccumulator extends CpuInstruction {

    @Override
    protected boolean executeStep(int step, int opcode, SM83 cpu) {
        if (step == 0) {
            int newFlags;
            cpu.A.emitToAluBus1();

            // Decodifica l'operazione in base al bit 3 dell'opcode
            // 0x17 -> 0001 0111 (Bit 3 è 0) -> RLA
            // 0x1F -> 0001 1111 (Bit 3 è 1) -> RRA
            boolean isRightRotate = (opcode & 0x08) != 0;

            if (isRightRotate) {
                newFlags = cpu.alu.rra(cpu.F.get());
            } else {
                newFlags = cpu.alu.rla(cpu.F.get());
            }

            // Campiona il risultato dall'ALU bus dentro l'accumulatore A
            cpu.A.sampleSoCBus();

            // Salva i nuovi flag calcolati
            cpu.F.set(newFlags);

            // Prepariamo il bus indirizzi per il fetch dell'istruzione successiva
            cpu.PC.emit();
            return true;
        }

        throw new IllegalStateException("Step non valido per Rotazioni Accumulatore: " + step);
    }
}