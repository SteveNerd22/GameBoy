package org.example.cpu.pipeline.instructions;

import org.example.cpu.SM83;

@CpuOpcode(value = {0xE8}) // ADD SP, e
public final class Opcode_AddSpImmediate8 extends CpuInstruction {
    private boolean isNegative;

    @Override
    protected boolean executeStep(int step, int opcode, SM83 cpu) {
        switch (step) {
            case 0 -> {
                cpu.PC.emit();
                cpu.Z.sampleSoCBus();
                advanceProgramCounter(cpu);
                isNegative = (cpu.Z.get() & 0x80) != 0;
                return false;
            }
            case 1 -> {
                cpu.SP.getLow().emitToAluBus1();
                cpu.Z.emitToAluBus2();
                int aluFlags = cpu.alu.add();
                cpu.Z.sampleSoCBus();
                int finalFlags = aluFlags & 0x30;
                cpu.F.set(finalFlags);
                return false;
            }

            case 2 -> {
                int adj = isNegative ? 0xFF : 0x00;
                cpu.SP.getHigh().emitToAluBus1();
                cpu.W.set(adj);
                cpu.W.emitToAluBus2();
                boolean carryIn = cpu.F.isCarrySet();
                cpu.alu.adc(carryIn);
                cpu.W.sampleSoCBus();
                return false;
            }
            case 3 -> {
                cpu.PC.emit();
                cpu.SP.set(cpu.WZ.get());
                return true;
            }
        }

        throw new IllegalStateException("Step non valido per ADD SP, e: " + step);
    }
}