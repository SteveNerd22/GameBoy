package org.example.cpu.pipeline.instructions;

import org.example.cpu.Register;
import org.example.cpu.SM83;

@CpuOpcode(value = {0x46, 0x4E, 0x56, 0x5E, 0x66, 0x6E, 0x7E}) // Escluso 0x76 che è l'istruzione speciale HALT
public final class Opcode_LdRegIndirectHl extends CpuInstruction {

    @Override
    protected boolean executeStep(int step, int opcode, SM83 cpu) {
        switch (step) {
            case 0:
                cpu.HL.emit();
                cpu.Z.sampleSoCBus();
                return false;

            case 1:
                Register destRegister = resolveDestRegister(opcode, cpu);
                cpu.Z.emitToInternalData();
                destRegister.sampleInternalData();
                cpu.PC.emit();
                return true;
        }

        throw new IllegalStateException("Step non valido per LD r, (HL): " + step);
    }
}