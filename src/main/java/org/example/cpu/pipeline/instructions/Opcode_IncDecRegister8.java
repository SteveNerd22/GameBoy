package org.example.cpu.pipeline.instructions;

import org.example.cpu.Register;
import org.example.cpu.SM83;

@CpuOpcode(value = {
        0x04, 0x05, // INC B, DEC B
        0x0C, 0x0D, // INC C, DEC C
        0x14, 0x15, // INC D, DEC D
        0x1C, 0x1D, // INC E, DEC E
        0x24, 0x25, // INC H, DEC H
        0x2C, 0x2D, // INC L, DEC L
        0x3C, 0x3D  // INC A, DEC A
})
public final class Opcode_IncDecRegister8 extends CpuInstruction {

    @Override
    protected boolean executeStep(int step, int opcode, SM83 cpu) {
        if (step == 0) {
            Register targetReg = resolveDestRegister(opcode, cpu);

            targetReg.emitToAluBus1();
            int currentFlags = cpu.F.get();
            int aluFlags;
            boolean isDec = (opcode & 0x01) != 0;

            if (isDec) {
                aluFlags = cpu.alu.dec(currentFlags);
            } else {
                aluFlags = cpu.alu.inc(currentFlags);
            }

            targetReg.sampleSoCBus();
            cpu.F.set(aluFlags);

            return true;
        }

        throw new IllegalStateException("Step non valido per INC/DEC r: " + step);
    }
}