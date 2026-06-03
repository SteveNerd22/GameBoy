package org.example.cpu.pipeline.instructions;

import org.example.cpu.SM83;

@CpuOpcode(value = {0xF8}) // LD HL, SP+e
public final class Opcode_LdHlSpImmediateSign8 extends CpuInstruction {

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
                int lsbSp = cpu.SP.getLow().get() & 0xFF;
                int offset = cpu.Z.get() & 0xFF;

                boolean halfCarry = ((lsbSp & 0x0F) + (offset & 0x0F)) > 0x0F;
                boolean carry = (lsbSp + offset) > 0xFF;

                int lowResult = (lsbSp + offset) & 0xFF;
                cpu.L.set(lowResult);

                // Aggiornamento dei Flag nell'ALU:
                cpu.F.setZ(false); // Sempre 0
                cpu.F.setN(false); // Sempre 0
                cpu.F.setH(halfCarry);
                cpu.F.setC(carry);

                int msbSp = cpu.SP.getHigh().get() & 0xFF;

                int adj = ((offset & 0x80) != 0) ? 0xFF : 0x00;

                int highResult = (msbSp + adj + (carry ? 1 : 0)) & 0xFF;
                cpu.H.set(highResult);

                return true;
            }
            default -> throw new IllegalStateException("Step non valido per LD HL, SP+e: " + step);
        }
    }
}