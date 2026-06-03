package org.example.cpu.pipeline.instructions;

import org.example.cpu.RegisterPair;
import org.example.cpu.SM83;

@CpuOpcode(value = {0x0A, 0x1A}) // 0x0A: LD A, (BC) | 0x1A: LD A, (DE)
public final class Opcode_LdAIndirectBcDe extends CpuInstruction {

    @Override
    protected boolean executeStep(int step, int opcode, SM83 cpu) {
        if (step == 0) {
            RegisterPair sourcePair = resolveSourcePair(opcode, cpu);
            sourcePair.emit();
            cpu.Z.sampleSoCBus();

            cpu.Z.emitToInternalData();
            cpu.A.sampleInternalData();

            return true;
        }

        throw new IllegalStateException("Step non valido per LD A, (BC/DE): " + step);
    }

    private RegisterPair resolveSourcePair(int opcode, SM83 cpu) {
        return ((opcode & 0x10) == 0) ? cpu.BC : cpu.DE;
    }
}