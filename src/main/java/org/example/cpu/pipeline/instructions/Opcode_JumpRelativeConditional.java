package org.example.cpu.pipeline.instructions;

import org.example.cpu.SM83;
import org.example.cpu.FlagsRegister;

@CpuOpcode(value = {
        0x20, // JR NZ, e
        0x28, // JR Z, e
        0x30, // JR NC, e
        0x38  // JR C, e
})
public final class Opcode_JumpRelativeConditional extends CpuInstruction {

    @Override
    protected boolean executeStep(int step, int opcode, SM83 cpu) {
        switch (step) {
            case 0 -> {
                cpu.PC.emit();
                cpu.Z.sampleSoCBus();
                advanceProgramCounter(cpu);
                return false;
            }
            case 1 -> {
                if(!checkCondition(opcode, cpu)) {
                    cpu.PC.emit();
                    return true;
                }
                boolean z_sign = (cpu.Z.get() & 0x80) == 0;
                cpu.Z.emitToAluBus1();
                cpu.PC.getLow().emitToAluBus2();
                int flags = cpu.alu.cpb();
                cpu.Z.sampleSoCBus();
                cpu.emitPCHOnAddressBus();
                boolean cpb_sign = (flags & 0x80) != 0;

                if(cpb_sign && !z_sign)
                    cpu.idu.incrementFormSoC();
                else if(!cpb_sign && z_sign)
                    cpu.idu.decrementFormSoC();
                else
                    cpu.idu.copyFormSoC();

                int Z = cpu.Z.get();
                cpu.WZ.sampleFromIduBus();
                cpu.Z.set(Z);
                return false;
            }
            case 2 -> {
                cpu.WZ.emit();
                cpu.idu.incrementFormSoC();
                cpu.PC.sampleFromIduBus();
                return true;
            }
        }

        throw new IllegalStateException("Step non valido per JR condizionale: " + step);
    }

    /**
     * Controlla lo stato dei flag in base all'opcode corrente
     */
    private boolean checkCondition(int opcode, SM83 cpu) {
        int flags = cpu.F.get();
        boolean isZeroSet = (flags & FlagsRegister.MASK_Z) != 0;
        boolean isCarrySet = (flags & FlagsRegister.MASK_C) != 0;

        return switch (opcode) {
            case 0x20 -> !isZeroSet;  // NZ
            case 0x28 -> isZeroSet;   // Z
            case 0x30 -> !isCarrySet; // NC
            case 0x38 -> isCarrySet;  // C
            default -> throw new IllegalArgumentException("Opcode sconosciuto per JR: " + String.format("0x%02X", opcode));
        };
    }
}