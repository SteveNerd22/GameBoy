package org.example.cpu.pipeline.instructions;

import org.example.cpu.RegisterPair;
import org.example.cpu.SM83;

@CpuOpcode(value = {0x02, 0x12}) // 0x02: LD (BC), A | 0x12: LD (DE), A
public final class Opcode_LdIndirectBcDeA extends CpuInstruction {

    @Override
    protected boolean executeStep(int step, int opcode, SM83 cpu) {
        switch(step) {
            case 0 -> {
                cpu.A.emit();
                cpu.controlUnit.sendWriteSignal();
                RegisterPair destPair = resolveDestPair(opcode, cpu);
                destPair.emit();
                return false;
            }
            case 1 -> {
                cpu.controlUnit.sendReadSignal();
                cpu.PC.emit();
                return true;
            }
        }

        throw new IllegalStateException("Step non valido per LD (BC/DE), A: " + step);
    }

    private RegisterPair resolveDestPair(int opcode, SM83 cpu) {
        return ((opcode & 0x10) == 0) ? cpu.BC : cpu.DE;
    }
}