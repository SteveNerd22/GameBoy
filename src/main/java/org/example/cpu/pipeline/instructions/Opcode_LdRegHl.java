package org.example.cpu.pipeline.instructions;

import org.example.bus.data.AddressData;
import org.example.cpu.Register;
import org.example.cpu.SM83;

@CpuOpcode(value = {0x46, 0x4E, 0x56, 0x5E, 0x66, 0x6E, 0x7E})
public final class Opcode_LdRegHl extends CpuInstruction {

    @Override
    public void executeCycle(SM83 cpu) {
        switch (this.currentStep) {
            case 0:
                cpu.HL.emit();

                this.currentStep = 1;
                break;

            case 1:
                int memoryValue = sampleDataBus(cpu);

                Register targetRegister = resolveDestRegister(this.currentOpcode, cpu);
                targetRegister.setValue(memoryValue);

                terminate();
                break;
        }
    }
}