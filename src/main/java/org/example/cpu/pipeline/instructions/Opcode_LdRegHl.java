package org.example.cpu.pipeline.instructions;

import org.example.bus.data.AddressData;
import org.example.cpu.SM83;
import org.example.cpu.Register;
@CpuOpcode(value = {0x46, 0x4E, 0x56, 0x5E, 0x66, 0x6E, 0x7E})
public final class Opcode_LdRegHl extends CpuInstruction {

    @Override
    public void executeCycle(SM83 cpu) {
        switch (this.currentStep) {
            case 0:
                int targetAddress = cpu.HL.get();
                cpu.SoCAddress.broadcast(cpu.H, new AddressData(targetAddress));

                this.currentStep = 1;
                break;

            case 1:
                int memoryValue = sampleDataBus(cpu);

                Register targetRegister = resolveTargetRegister(currentOpcode, cpu);
                targetRegister.setValue(memoryValue);

                terminate();
                break;
        }
    }

    /**
     * Mappatura interna basata sulla tabella hardware del Game Boy
     */
    private Register resolveTargetRegister(int opcode, SM83 cpu) {
        return switch (opcode) {
            case 0x46 -> cpu.B; // LD B, (HL)
            case 0x4E -> cpu.C; // LD C, (HL)
            case 0x56 -> cpu.D; // LD D, (HL)
            case 0x5E -> cpu.E; // LD E, (HL)
            case 0x66 -> cpu.H; // LD H, (HL)
            case 0x6E -> cpu.L; // LD L, (HL)
            case 0x7E -> cpu.A; // LD A, (HL) -> Il tuo test originale rientra qui!
            default -> throw new IllegalArgumentException("Opcode inatteso nella pipeline LdRegHl: " + opcode);
        };
    }
}