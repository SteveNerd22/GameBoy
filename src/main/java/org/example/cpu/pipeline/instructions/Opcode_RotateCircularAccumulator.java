package org.example.cpu.pipeline.instructions;

import org.example.cpu.SM83;

@CpuOpcode(value = {
        0x07, // RLCA (Rotate Left Circular Accumulator)
        0x0F  // RRCA (Rotate Right Circular Accumulator)
})
public final class Opcode_RotateCircularAccumulator extends CpuInstruction {

    @Override
    protected boolean executeStep(int step, int opcode, SM83 cpu) {
        if (step == 0) {
            int newFlags;
            cpu.A.emitToAluBus1();
            boolean isRightRotate = (opcode & 0x08) != 0;

            if (isRightRotate) {
                newFlags = cpu.alu.rrca();
            } else {
                newFlags = cpu.alu.rlca();
            }

            cpu.A.sampleSoCBus();
            cpu.F.set(newFlags);
            cpu.PC.emit();
            return true;
        }

        throw new IllegalStateException("Step non valido per Rotazioni Circolari Accumulatore: " + step);
    }
}