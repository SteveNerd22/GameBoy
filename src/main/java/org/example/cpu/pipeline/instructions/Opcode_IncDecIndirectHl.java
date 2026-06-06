package org.example.cpu.pipeline.instructions;

import org.example.cpu.SM83;

@CpuOpcode(value = {
        0x34, // INC (HL)
        0x35  // DEC (HL)
})
public final class Opcode_IncDecIndirectHl extends CpuInstruction {

    @Override
    protected boolean executeStep(int step, int opcode, SM83 cpu) {
        switch(step) {
            case 0 -> {
                cpu.HL.emit();
                cpu.Z.sampleSoCBus();
                return false;
            }
            case 1 -> {
                cpu.Z.emitToAluBus1();
                int currentFlags = cpu.F.get();
                int aluFlags;
                boolean isDec = (opcode & 0x01) != 0;
                if (isDec) {
                    aluFlags = cpu.alu.dec(currentFlags);
                } else {
                    aluFlags = cpu.alu.inc(currentFlags);
                }
                cpu.controlUnit.sendWriteSignal();
                cpu.HL.emit();
                cpu.F.set(aluFlags);
                return false;
            }
            case 2 -> {
                cpu.controlUnit.sendReadSignal();
                cpu.PC.emit();
                return true;
            }
        }

        throw new IllegalStateException("Step non valido per INC/DEC (HL): " + step);
    }
}